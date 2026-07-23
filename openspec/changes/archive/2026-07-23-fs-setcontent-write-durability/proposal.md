## Why

`DefaultFilesystemStoreImpl.setContent(...)` delegates write durability to the sole `close()` of the `OutputStream` and then discards that `close()`'s `IOException` via `IOUtils.closeQuietly(os)`, with no `fsync`. On filesystems whose write errors are deferred to flush-on-close (NFS), this produces an apparent success with a missing or partial blob — no exception, no log — causing silent data loss downstream (a node referencing a non-existent blob).

The defect is latent while the backend is local disk or S3 (write = atomic ACKed `PUT`), but it is exposed when storage moves from `S3ContentStore` to `FilesystemContentStore` on an NFS share. The root-cause fix belongs in this library.

## What Changes

- Replace `IOUtils.closeQuietly(os)` with a `close()` whose `IOException` **propagates** as `StoreAccessException` — no flush/close error is silently discarded.
- Add an explicit `fsync` (`FileChannel.force(true)` / `FileDescriptor.sync()`) on the blob file **before** close, so a successful `setContent` means the bytes reached stable storage. Best-effort `fsync` of the containing directory to make the new entry durable, where the filesystem supports it.
- Ensure every flush / `fsync` / `close` error surfaces as an exception out of `setContent`, independent of the NFS mount mode (`soft`/`hard`).
- Apply the fix to **both** durability-bearing code paths in the class: `setContent(S, InputStream)` (line ~172) **and** the PropertyPath-based `setContent(S, PropertyPath, InputStream, SetContentParams)` (line ~271), which share the identical `closeQuietly` / no-`fsync` defect.
- No change to public store API signatures beyond the durability / error-propagation semantics. Nominal path behaviour is unchanged (content written and readable, `ContentLength` correct).

## Capabilities

### New Capabilities
- `filesystem-content-write-durability`: Durability and error-propagation guarantees for `FilesystemContentStore` writes — a successful `setContent` implies the content is `fsync`'d to stable storage, and any flush/fsync/close failure surfaces as an exception rather than a silent success.

### Modified Capabilities
<!-- None: no existing spec covers filesystem write durability. -->

## Impact

- **Code**: `spring-content-fs/src/main/java/internal/org/springframework/content/fs/store/DefaultFilesystemStoreImpl.java` — both `setContent` write paths.
- **Behaviour**: `setContent` now performs `fsync` before returning and propagates flush/close/fsync errors; callers that previously received a silent success on a failed NFS flush will now receive a `StoreAccessException`.
- **Performance**: `force(true)` adds a durability cost per write (acceptable — it is the price of durability); note for high-volume write workloads.
- **Release lines**: fix must ship on **both** `main` (4.x) and `support/spring-boot-3` (3.x) with distinct artifact versions.
- **Tests**: new regression test simulating a flush/close failure (fault-injected `OutputStream` / non-writable target) asserting `setContent` propagates instead of returning success.
