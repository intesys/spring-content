## 1. Durable write helper

- [x] 1.1 Add a private helper in `DefaultFilesystemStoreImpl` that writes an `InputStream` to the resolved blob `File`/`Path` via a `FileChannel` (`CREATE, WRITE, TRUNCATE_EXISTING`), calls `channel.force(true)` after the full copy, and closes via `try`-with-resources so flush/fsync/close errors propagate.
- [x] 1.2 Add best-effort directory `fsync`: open the parent directory as a channel and `force(true)`, logging-and-continuing on `IOException` from platforms/filesystems that reject directory sync.
- [x] 1.3 Map any `IOException` from the helper to `StoreAccessException` (reuse existing log+throw pattern).

## 2. Wire the helper into both setContent paths

- [x] 2.1 Replace the `getOutputStream()` + `IOUtils.copy` + `finally { IOUtils.closeQuietly(os); }` block in `setContent(S, InputStream)` (line ~188-206) with the durable helper; remove `closeQuietly` from this path.
- [x] 2.2 Replace the identical block in `setContent(S, PropertyPath, InputStream, SetContentParams)` (line ~271-288) with the durable helper; remove `closeQuietly` from this path.
- [x] 2.3 Confirm `@ContentLength` handling is unchanged and now reads a fully-flushed blob (entity path via `resource.contentLength()`, PropertyPath path via `params.getContentLength()` fallback).
- [x] 2.4 Remove the now-unused `IOUtils` import if no other usage remains.

## 3. Tests

- [x] 3.1 Add a regression test (JUnit 5 / Mockito) that fault-injects a flush/`force`/`close` `IOException` and asserts `setContent` throws `StoreAccessException` instead of returning success — for both write paths.
- [x] 3.2 Add/confirm a nominal-path test: content written, readable via `getContent`, `@ContentLength` correct.
- [x] 3.3 Add a test asserting `force(true)` is invoked before the stream is closed (e.g. via a spy/decorated channel ordering assertion).

## 4. Build & release (both lines)

- [x] 4.1 `AWS_REGION=us-west-1 ./mvnw -pl spring-content-fs -P tests test` passes on `main` (4.x).

## 5. Documentation

- [x] 5.1 Document the durability guarantee and best-effort directory-fsync behaviour (Javadoc on `setContent` / helper) and add the `@author` tag per repo convention.
