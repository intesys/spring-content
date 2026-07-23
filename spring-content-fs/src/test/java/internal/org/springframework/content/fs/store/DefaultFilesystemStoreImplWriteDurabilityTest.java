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

import com.github.paulcwarren.ginkgo4j.Ginkgo4jRunner;
import org.junit.runner.RunWith;
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
import java.util.stream.Stream;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.AfterEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.BeforeEach;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Context;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.Describe;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.It;
import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.JustBeforeEach;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
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
@RunWith(Ginkgo4jRunner.class)
public class DefaultFilesystemStoreImplWriteDurabilityTest {

	private static final byte[] CONTENT = "hello durable world".getBytes(StandardCharsets.UTF_8);
	private static final String PROPERTY_NAME = "content";

	private FileSystemResourceLoader loader;
	private PlacementService placer;
	private FileService fileService;
	private MappingContext mappingContext;
	private DefaultFilesystemStoreImpl<TestEntity, String> store;

	private Path tempDir;
	private Path blobPath;
	private FileSystemResource resource;

	private TestEntity entity;
	private Exception e;

	{
		Describe("DefaultFilesystemStoreImpl write durability", () -> {

			BeforeEach(() -> {
				loader = mock(FileSystemResourceLoader.class);
				placer = mock(PlacementService.class);
				fileService = mock(FileService.class);
				mappingContext = mock(MappingContext.class);

				store = spy(new DefaultFilesystemStoreImpl<>(loader, mappingContext, placer, fileService));

				tempDir = Files.createTempDirectory("fs-durability");
				blobPath = tempDir.resolve("blob.bin");
				resource = new FileSystemResource(blobPath.toFile());

				entity = new TestEntity();
				this.e = null;
			});

			AfterEach(() -> {
				deleteRecursively(tempDir);
			});

			Context("nominal path", () -> {

				Context("on the entity path", () -> {
					BeforeEach(() -> {
						doReturn(resource).when(store).getResource(entity);
					});

					It("writes content, fsyncs, and sets the correct @ContentLength", () -> {
						store.setContent(entity, new ByteArrayInputStream(CONTENT));

						assertThat(new String(Files.readAllBytes(blobPath), StandardCharsets.UTF_8),
								is(new String(CONTENT, StandardCharsets.UTF_8)));
						assertThat(entity.getContentLength(), is((long) CONTENT.length));

						// readable via getContent
						try (InputStream in = store.getContent(entity)) {
							assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8),
									is(new String(CONTENT, StandardCharsets.UTF_8)));
						}
					});

					It("invokes force(true) before the channel is closed", () -> {
						List<String> events = new ArrayList<>();
						FileChannel real = FileChannel.open(blobPath,
								java.nio.file.StandardOpenOption.CREATE,
								java.nio.file.StandardOpenOption.WRITE,
								java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
						RecordingFileChannel recording = new RecordingFileChannel(real, events, false, false);
						doReturn(recording).when(store).openChannel(any());

						store.setContent(entity, new ByteArrayInputStream(CONTENT));

						assertThat("force(true) must be called before the channel is closed",
								events, is(List.of("force", "close")));
					});
				});
			});

			Context("failure propagation", () -> {

				Context("on the entity path", () -> {
					BeforeEach(() -> {
						doReturn(resource).when(store).getResource(entity);
					});
					JustBeforeEach(() -> {
						try {
							store.setContent(entity, new ByteArrayInputStream(CONTENT));
						} catch (Exception ex) {
							this.e = ex;
						}
					});

					Context("when fsync (force) fails", () -> {
						BeforeEach(() -> {
							doReturn(faultChannel(true, false)).when(store).openChannel(any());
						});
						It("surfaces a StoreAccessException", () -> {
							assertThat(e, is(instanceOf(StoreAccessException.class)));
						});
					});

					Context("when close fails", () -> {
						BeforeEach(() -> {
							doReturn(faultChannel(false, true)).when(store).openChannel(any());
						});
						It("surfaces a StoreAccessException", () -> {
							assertThat(e, is(instanceOf(StoreAccessException.class)));
						});
					});
				});

				Context("on the PropertyPath path", () -> {
					PropertyPath propertyPath = PropertyPath.from(PROPERTY_NAME);
					SetContentParams params = SetContentParams.builder().contentLength((long) CONTENT.length).build();

					BeforeEach(() -> {
						stubContentProperty();
						doReturn(resource).when(store).getResource(eq(entity), eq(propertyPath));
					});
					JustBeforeEach(() -> {
						try {
							store.setContent(entity, propertyPath, new ByteArrayInputStream(CONTENT), params);
						} catch (Exception ex) {
							this.e = ex;
						}
					});

					Context("when fsync (force) fails", () -> {
						BeforeEach(() -> {
							doReturn(faultChannel(true, false)).when(store).openChannel(any());
						});
						It("surfaces a StoreAccessException", () -> {
							assertThat(e, is(instanceOf(StoreAccessException.class)));
						});
					});

					Context("when close fails", () -> {
						BeforeEach(() -> {
							doReturn(faultChannel(false, true)).when(store).openChannel(any());
						});
						It("surfaces a StoreAccessException", () -> {
							assertThat(e, is(instanceOf(StoreAccessException.class)));
						});
					});
				});
			});
		});
	}

	private ContentProperty stubContentProperty() {
		ContentProperty contentProperty = mock(ContentProperty.class);
		when(mappingContext.getContentProperty(TestEntity.class, PROPERTY_NAME)).thenReturn(contentProperty);
		// Non-null id + default (non-CreateNew) disposition => skip id creation, so no placer.convert stubbing needed.
		when(contentProperty.getContentId(any())).thenReturn("existing-id");
		return contentProperty;
	}

	private RecordingFileChannel faultChannel(boolean failForce, boolean failClose) throws IOException {
		FileChannel real = FileChannel.open(blobPath,
				java.nio.file.StandardOpenOption.CREATE,
				java.nio.file.StandardOpenOption.WRITE,
				java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
		return new RecordingFileChannel(real, new ArrayList<>(), failForce, failClose);
	}

	private static void deleteRecursively(Path root) throws IOException {
		if (root == null || !Files.exists(root)) {
			return;
		}
		try (Stream<Path> paths = Files.walk(root)) {
			paths.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.deleteIfExists(p);
				} catch (IOException ignored) {
				}
			});
		}
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
