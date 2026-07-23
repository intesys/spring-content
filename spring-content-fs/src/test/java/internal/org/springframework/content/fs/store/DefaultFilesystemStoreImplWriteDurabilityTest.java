/*
 * Copyright (c) ${year} Intesys S.r.l. and the Spring Content contributors
 *
 * This file is part of Spring Content.
 *
 * Spring Content is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Spring Content is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Spring Content.  If not, see <https://www.gnu.org/licenses/>.
 */
package internal.org.springframework.content.fs.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.mappingcontext.ContentProperty;
import org.springframework.content.commons.mappingcontext.MappingContext;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.utils.FileService;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.core.io.FileSystemResource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the write-durability guarantees of {@link DefaultFilesystemStoreImpl}: a
 * successful {@code setContent} implies the blob has been {@code fsync}'d, and any
 * flush/{@code fsync}/close failure surfaces as a {@link StoreAccessException} rather than a
 * silent success. Covers both write paths (entity-based and PropertyPath-based).
 *
 * @author RuudGianesini
 */
@DisplayName("DefaultFilesystemStoreImpl write durability")
class DefaultFilesystemStoreImplWriteDurabilityTest {

    private static final byte[] CONTENT = "hello durable world".getBytes(StandardCharsets.UTF_8);
    private static final String PROPERTY_NAME = "content";

    @TempDir
    Path tempDir;

    private FileSystemResourceLoader loader;
    private PlacementService placer;
    private FileService fileService;
    private MappingContext mappingContext;
    private DefaultFilesystemStoreImpl<TestEntity, String> store;

    private Path blobPath;
    private FileSystemResource resource;

    @BeforeEach
    void setUp() {
        loader = mock(FileSystemResourceLoader.class);
        placer = mock(PlacementService.class);
        fileService = mock(FileService.class);
        mappingContext = mock(MappingContext.class);

        store = spy(new DefaultFilesystemStoreImpl<>(loader, mappingContext, placer, fileService));

        blobPath = tempDir.resolve("blob.bin");
        resource = new FileSystemResource(blobPath.toFile());
    }

    private ContentProperty stubContentProperty() {
        ContentProperty contentProperty = mock(ContentProperty.class);
        when(mappingContext.getContentProperty(TestEntity.class, PROPERTY_NAME)).thenReturn(contentProperty);
        // Non-null id + default (non-CreateNew) disposition => skip id creation, so no placer.convert stubbing needed.
        when(contentProperty.getContentId(any())).thenReturn("existing-id");
        return contentProperty;
    }

    @Nested
    @DisplayName("nominal path")
    class NominalPath {

        @Test
        @DisplayName("writes content, fsyncs, and sets the correct @ContentLength (entity path)")
        void entityPathWritesReadableContentWithCorrectLength() throws Exception {
            TestEntity entity = new TestEntity();
            doReturn(resource).when(store).getResource(entity);

            store.setContent(entity, new ByteArrayInputStream(CONTENT));

            assertEquals(new String(CONTENT, StandardCharsets.UTF_8),
                    new String(Files.readAllBytes(blobPath), StandardCharsets.UTF_8));
            assertEquals((long) CONTENT.length, entity.getContentLength());

            // readable via getContent
            try (InputStream in = store.getContent(entity)) {
                assertEquals(new String(CONTENT, StandardCharsets.UTF_8),
                        new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
        }

        @Test
        @DisplayName("force(true) is invoked before the channel is closed")
        void forceHappensBeforeClose() throws Exception {
            TestEntity entity = new TestEntity();
            doReturn(resource).when(store).getResource(entity);

            List<String> events = new ArrayList<>();
            FileChannel real = FileChannel.open(blobPath,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE,
                    java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
            RecordingFileChannel recording = new RecordingFileChannel(real, events, false, false);
            doReturn(recording).when(store).openChannel(any());

            store.setContent(entity, new ByteArrayInputStream(CONTENT));

            assertEquals(List.of("force", "close"), events,
                    "force(true) must be called before the channel is closed");
        }
    }

    @Nested
    @DisplayName("failure propagation")
    class FailurePropagation {

        @Test
        @DisplayName("fsync (force) failure surfaces as StoreAccessException on the entity path")
        void forceFailureEntityPath() throws Exception {
            TestEntity entity = new TestEntity();
            doReturn(resource).when(store).getResource(entity);
            doReturn(faultChannel(true, false)).when(store).openChannel(any());

            assertThrows(StoreAccessException.class,
                    () -> store.setContent(entity, new ByteArrayInputStream(CONTENT)));
        }

        @Test
        @DisplayName("close failure surfaces as StoreAccessException on the entity path")
        void closeFailureEntityPath() throws Exception {
            TestEntity entity = new TestEntity();
            doReturn(resource).when(store).getResource(entity);
            doReturn(faultChannel(false, true)).when(store).openChannel(any());

            assertThrows(StoreAccessException.class,
                    () -> store.setContent(entity, new ByteArrayInputStream(CONTENT)));
        }

        @Test
        @DisplayName("fsync (force) failure surfaces as StoreAccessException on the PropertyPath path")
        void forceFailurePropertyPath() throws Exception {
            TestEntity entity = new TestEntity();
            PropertyPath propertyPath = PropertyPath.from(PROPERTY_NAME);
            stubContentProperty();
            doReturn(resource).when(store).getResource(eq(entity), eq(propertyPath));
            doReturn(faultChannel(true, false)).when(store).openChannel(any());

            SetContentParams params = SetContentParams.builder().contentLength((long) CONTENT.length).build();

            assertThrows(StoreAccessException.class,
                    () -> store.setContent(entity, propertyPath, new ByteArrayInputStream(CONTENT), params));
        }

        @Test
        @DisplayName("close failure surfaces as StoreAccessException on the PropertyPath path")
        void closeFailurePropertyPath() throws Exception {
            TestEntity entity = new TestEntity();
            PropertyPath propertyPath = PropertyPath.from(PROPERTY_NAME);
            stubContentProperty();
            doReturn(resource).when(store).getResource(eq(entity), eq(propertyPath));
            doReturn(faultChannel(false, true)).when(store).openChannel(any());

            SetContentParams params = SetContentParams.builder().contentLength((long) CONTENT.length).build();

            assertThrows(StoreAccessException.class,
                    () -> store.setContent(entity, propertyPath, new ByteArrayInputStream(CONTENT), params));
        }
    }

    private RecordingFileChannel faultChannel(boolean failForce, boolean failClose) throws IOException {
        FileChannel real = FileChannel.open(blobPath,
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.WRITE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
        return new RecordingFileChannel(real, new ArrayList<>(), failForce, failClose);
    }

    /**
     * A {@link FileChannel} decorator that delegates to a real channel, records the ordering of
     * {@code force}/{@code close}, and can inject an {@link IOException} on either — used to
     * simulate a deferred NFS flush-on-close/fsync failure deterministically.
     */
    static class RecordingFileChannel extends FileChannel {

        private final FileChannel delegate;
        private final List<String> events;
        private final boolean failForce;
        private final boolean failClose;

        RecordingFileChannel(FileChannel delegate, List<String> events, boolean failForce, boolean failClose) {
            this.delegate = delegate;
            this.events = events;
            this.failForce = failForce;
            this.failClose = failClose;
        }

        @Override
        public void force(boolean metaData) throws IOException {
            events.add("force");
            if (failForce) {
                throw new IOException("injected fsync failure");
            }
            delegate.force(metaData);
        }

        @Override
        protected void implCloseChannel() throws IOException {
            events.add("close");
            delegate.close();
            if (failClose) {
                throw new IOException("injected close failure");
            }
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            return delegate.read(dst);
        }

        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) throws IOException {
            return delegate.read(dsts, offset, length);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            return delegate.write(src);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            return delegate.write(srcs, offset, length);
        }

        @Override
        public long position() throws IOException {
            return delegate.position();
        }

        @Override
        public FileChannel position(long newPosition) throws IOException {
            delegate.position(newPosition);
            return this;
        }

        @Override
        public long size() throws IOException {
            return delegate.size();
        }

        @Override
        public FileChannel truncate(long size) throws IOException {
            delegate.truncate(size);
            return this;
        }

        @Override
        public long transferTo(long position, long count, WritableByteChannel target) throws IOException {
            return delegate.transferTo(position, count, target);
        }

        @Override
        public long transferFrom(ReadableByteChannel src, long position, long count) throws IOException {
            return delegate.transferFrom(src, position, count);
        }

        @Override
        public int read(ByteBuffer dst, long position) throws IOException {
            return delegate.read(dst, position);
        }

        @Override
        public int write(ByteBuffer src, long position) throws IOException {
            return delegate.write(src, position);
        }

        @Override
        public MappedByteBuffer map(MapMode mode, long position, long size) throws IOException {
            return delegate.map(mode, position, size);
        }

        @Override
        public FileLock lock(long position, long size, boolean shared) throws IOException {
            return delegate.lock(position, size, shared);
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            return delegate.tryLock(position, size, shared);
        }
    }
}

class TestEntity {

    @ContentId
    private String contentId;

    @ContentLength
    private long contentLength;

    public String getContentId() {
        return contentId;
    }

    public void setContentId(String contentId) {
        this.contentId = contentId;
    }

    public long getContentLength() {
        return contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }
}
