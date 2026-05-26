package org.springframework.content.fs.io;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.core.io.Resource;

public class FileSystemResourceLoaderTest {

	private FileSystemResourceLoader loader = null;

	private String path;

	private String location;

	private File parent;
	private File file;

	private Exception ex;

	@Nested
	@DisplayName("FileSystemResourceLoader")
	class Filesystemresourceloader {

		@Nested
		@DisplayName("#FileSystemResourceLoader")
		class FilesystemresourceloaderConstructor {

			private void createLoader() {
				try {
					loader = new FileSystemResourceLoader(path);
				}
				catch (Exception e) {
					ex = e;
				}
			}

			@Nested
			@DisplayName("given well formed path (has a trailing slash)")
			class GivenWellFormedPathHasATrailingSlash {

				@BeforeEach
				void setUp() throws Exception {
					path = Paths.get("some", "well-formed", "path") + File.separator;
				}

				@Test
				@DisplayName("succeeds")
				void succeeds() throws Exception {
					createLoader();
					assertThat(ex, is(nullValue()));

					File expected = Paths.get(path, "something").toFile();
					File actual = loader.getResource("/something").getFile();

					assertThat(actual.getAbsolutePath(), is(expected.getAbsolutePath()));
					assertThat(loader.getResource("/something"),
							   instanceOf(DeletableResource.class));
				}
			}

			@Nested
			@DisplayName("given malformed path without a trailing slash)")
			class GivenMalformedPathWithoutATrailingSlash {

				@BeforeEach
				void setUp() throws Exception {
					path = Paths.get("some", "malformed", "path").toString();
				}

				@Test
				@DisplayName("succeeds")
				void succeeds() throws Exception {
					createLoader();
					assertThat(ex, is(nullValue()));

					File expected = Paths.get(path, "something").toFile();
					File actual = loader.getResource("/something").getFile();

					assertThat(actual.getAbsolutePath(), is(expected.getAbsolutePath()));
					assertThat(loader.getResource("/something"),
							   instanceOf(DeletableResource.class));
				}
			}
		}

		@Nested
		@DisplayName("DeletableResource")
		class Deletableresource {

			@Nested
			@DisplayName("#delete")
			class Delete {

				@BeforeEach
				void setUp() throws Exception {
					parent = new File(Paths
							.get(System.getProperty("java.io.tmpdir"),
									UUID.randomUUID().toString())
							.toAbsolutePath().toString());
				}

				private void executeDelete() throws Exception {
					loader = new FileSystemResourceLoader(parent.getPath() + File.separator);
					Resource resource = loader.getResource(location);
					assertThat(resource, instanceOf(DeletableResource.class));
					((DeletableResource) resource).delete();
				}

				@Nested
				@DisplayName("given a file resource that exists")
				class GivenAFileResourceThatExists {

					@BeforeEach
					void setUp() throws Exception {
						location = "FileSystemResourceLoaderTest.tmp";
						file = new File(parent, location);
						FileUtils.touch(file);
						assertThat(file.exists(), is(true));
					}

					@Test
					@DisplayName("should delete the underlying file")
					void shouldDeleteTheUnderlyingFile() throws Exception {
						executeDelete();
						assertThat(file.exists(), is(false));
					}
				}
			}
		}
	}
}
