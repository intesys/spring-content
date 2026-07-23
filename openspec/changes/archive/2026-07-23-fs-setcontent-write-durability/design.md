## Context

`DefaultFilesystemStoreImpl` writes blobs by obtaining `((WritableResource) resource).getOutputStream()`, running `IOUtils.copy(content, os)`, and closing the stream in a `finally` via `IOUtils.closeQuietly(os)`. There is no `fsync` anywhere in the class, and `closeQuietly` swallows the `IOException` that flush-on-close raises. On NFS (and any FS that defers write errors to close), a line drop during `close()` yields a `setContent` that returns success with a missing/partial blob — silent data loss (verified against sources `4.0.0`; both the entity-based path at line ~172 and the PropertyPath-based path at line ~271 share the defect).

Constraint: the store's write stream comes from a Spring `WritableResource`. Depending on the Spring/JDK version, `FileSystemResource.getOutputStream()` may return a `FileOutputStream` (exposes `FileDescriptor` / channel) **or** an NIO channel-backed stream (`Files.newOutputStream`), so we cannot assume a `FileDescriptor` is reachable from the returned `OutputStream`. The design must obtain a durable-sync handle deterministically.

Two release lines are in scope: `main` (4.x) and `support/spring-boot-3` (3.x). The fix must land on both with distinct artifact versions.

## Goals / Non-Goals

**Goals:**
- A successful `setContent` implies the blob is `fsync`'d to stable storage, or the call fails with `StoreAccessException`.
- Flush/`fsync`/`close` `IOException`s propagate; no `closeQuietly` on the write stream.
- Correctness independent of NFS mount mode (`soft`/`hard`).
- Fix applied to both write paths; nominal behaviour and `@ContentLength` unchanged.

**Non-Goals:**
- No dependency on NFS mount configuration for correctness.
- No public store API signature changes beyond durability/error-propagation semantics.
- No change to `getContent`, `unsetContent`, `associate`, or S3/other backends.
- Guaranteeing directory-entry durability on every filesystem (best-effort only).

## Decisions

### Decision 1: Write through a `FileChannel` we own, then `force(true)`
Resolve the resource to its `File`/`Path` (`resource.getFile()`, already used in the `mkdirs` branch) and write via a `FileChannel` opened with `CREATE, WRITE, TRUNCATE_EXISTING`. Copy bytes, call `channel.force(true)`, then close inside a `try`-with-resources so close errors propagate.

- **Why over "cast the resource OutputStream to FileOutputStream and call `getFD().sync()`":** the returned stream type is not guaranteed across Spring/JDK versions; casting is fragile and would silently skip `fsync` when the stream is channel-backed. Owning the channel makes `force(true)` deterministic.
- **Alternative considered — reflectively extract the `FileDescriptor` from the resource's stream:** rejected as brittle and version-dependent.

### Decision 2: Replace `IOUtils.closeQuietly(os)` with `try`-with-resources / explicit `close()` in the guarded `try`
Move the write+`force`+close into the `try` block that already maps `IOException` → `StoreAccessException`, so flush/fsync/close failures are wrapped and propagated. Remove `IOUtils.closeQuietly` from the write path entirely.

### Decision 3: Best-effort directory `fsync`
After the blob `force(true)`, best-effort `fsync` the containing directory (open the parent dir as a channel and `force(true)`) so a newly-created entry is durable. Wrap in a narrow try that tolerates platforms/filesystems that reject directory sync (e.g. some Windows/FS combinations) — a failed directory sync SHALL be logged but SHALL NOT by itself fail an otherwise-durable blob write. Document this behaviour.

### Decision 4: Preserve `ContentLength` semantics
Keep setting `@ContentLength` from `resource.contentLength()` (entity path) / `params.getContentLength()` fallback (PropertyPath path). Because the blob is now `fsync`'d before this read, the stat cannot observe a stale/partial size on the nominal path.

### Decision 5: Ship on both release lines
Apply the identical fix on `main` (4.x) and cherry-pick/port to `support/spring-boot-3` (3.x), each with its own artifact version bump. Regression test duplicated on both lines.

## Risks / Trade-offs

- **[Performance] `force(true)` per write adds latency, especially high-volume writes** → Accepted: it is the cost of durability; call it once per `setContent` after the full copy, not per buffer. Note in release notes for high-throughput callers.
- **[Compatibility] `resource.getFile()` may throw for non-file-backed `WritableResource`s** → The store is filesystem-backed; `getFile()` is already used in the `mkdirs` branch, so this path is already file-based. Guard remains inside the `IOException`-mapping `try`.
- **[Directory fsync portability] directory `force(true)` unsupported on some FS/OS** → Best-effort with log-and-continue (Decision 3); blob durability is unaffected.
- **[Behaviour change for callers] callers previously getting silent success on NFS flush failure now get an exception** → Intended and the whole point; called out in the proposal Impact so downstream consumers expect it.

## Migration Plan

1. Implement the fix in `DefaultFilesystemStoreImpl` on `main`, add regression test with fault-injected write failure.
2. `AWS_REGION=us-west-1 ./mvnw -pl spring-content-fs -P tests test` to validate nominal + failure paths.
3. Release 4.x artifact; port to `support/spring-boot-3`, release 3.x artifact (distinct version).

Rollback: revert the commit on the affected line and re-release the prior artifact; no data migration involved.

## Open Questions

- Should directory `fsync` be opt-out via a store property for FS/OS combinations that reject it, or is best-effort log-and-continue sufficient? (Leaning: best-effort is sufficient; revisit only if a target FS logs noisily.)
- Preferred fault-injection mechanism for the regression test: a decorating `OutputStream`/`FileChannel` that throws on `force`/`close`, vs. a read-only target directory. (Leaning: decorating stream for deterministic close/force failure.)
