# filesystem-content-write-durability Specification

## Purpose
TBD - created by archiving change fs-setcontent-write-durability. Update Purpose after archive.
## Requirements
### Requirement: Durable write on successful setContent

`FilesystemContentStore.setContent` SHALL guarantee that when it returns successfully, the written content has been flushed and `fsync`'d to stable storage (`FileChannel.force(true)` / `FileDescriptor.sync()` on the blob file). A successful return SHALL NOT be possible while the blob is missing or partially written. This guarantee SHALL hold regardless of the underlying mount mode (`soft` or `hard` for NFS) — correctness SHALL NOT depend on mount configuration.

This requirement applies to every durability-bearing write path of the store, specifically both `setContent(S, InputStream)` and the PropertyPath-based `setContent(S, PropertyPath, InputStream, SetContentParams)`.

#### Scenario: Content is fsync'd before setContent returns

- **WHEN** a caller invokes `setContent` with a readable input stream against a writable filesystem resource
- **THEN** the store copies the bytes, calls `fsync` (`FileChannel.force(true)`) on the blob file, and only then closes the stream and returns
- **AND** after the call returns, the full content is present on disk and readable via `getContent`

#### Scenario: Nominal path behaviour is unchanged

- **WHEN** `setContent` completes successfully on a healthy filesystem
- **THEN** the content is written and readable and the entity's `@ContentLength` reflects the correct byte count of the written blob

### Requirement: Flush, fsync, and close failures propagate

`FilesystemContentStore.setContent` SHALL propagate any `IOException` arising from flush, `fsync`, or `close` of the write stream as a `StoreAccessException`. It SHALL NOT swallow such errors (no `IOUtils.closeQuietly` on the write stream). A deferred write error that only surfaces at flush-on-close SHALL cause `setContent` to fail rather than return success.

#### Scenario: close/flush failure surfaces as an exception

- **WHEN** the underlying `OutputStream`'s flush or `close` fails with an `IOException` (e.g. deferred NFS write error, or a fault-injected stream)
- **THEN** `setContent` throws `StoreAccessException` wrapping the `IOException`
- **AND** the caller does not observe a silent success

#### Scenario: fsync failure surfaces as an exception

- **WHEN** the `fsync` (`FileChannel.force(true)`) of the blob file fails with an `IOException`
- **THEN** `setContent` throws `StoreAccessException` and does not return success

#### Scenario: mid-copy IO error still propagates

- **WHEN** an `IOException` occurs while copying bytes from the input stream to the output stream
- **THEN** `setContent` throws `StoreAccessException` (unchanged from current behaviour)

