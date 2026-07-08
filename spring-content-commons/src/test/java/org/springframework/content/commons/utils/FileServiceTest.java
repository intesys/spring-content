package org.springframework.content.commons.utils;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class FileServiceTest {

	private FileService fileService;
	private File file;
	private File parent;
	private Exception ex;

	@Nested
	@DisplayName("mkdirs")
	class Mkdirs {

		@BeforeEach
		void setUp() {
			parent = new File(Paths
					.get(System.getProperty("java.io.tmpdir"),
							UUID.randomUUID().toString())
					.toAbsolutePath().toString());
			fileService = new FileServiceImpl();
			ex = null;
		}

		void executeMkdirs() {
			try {
				fileService.mkdirs(file);
			}
			catch (Exception e) {
				ex = e;
			}
		}

		@Nested
		@DisplayName("when passed in a file that exists")
		class WhenFileExists {
			@BeforeEach
			void setUp() throws IOException {
				file = new File(parent, "something.txt");
				FileUtils.touch(file);
				assertThat(file.exists(), is(true));
				executeMkdirs();
			}

			@AfterEach
			void tearDown() {
				file.delete();
			}

			@Test
			@DisplayName("should throw an IOException")
			void shouldThrowIOException() {
				assertThat(ex, is(not(nullValue())));
				assertThat(ex, instanceOf(IOException.class));
			}
		}

		@Nested
		@DisplayName("when passed in a file that does not exist")
		class WhenFileDoesNotExist {
			@BeforeEach
			void setUp() {
				file = new File(parent, "something.txt");
				assertThat(file.exists(), is(false));
				executeMkdirs();
			}

			@AfterEach
			void tearDown() {
				file.delete();
			}

			@Test
			@DisplayName("should not throw an exception")
			void shouldNotThrowException() {
				assertThat(ex, is(nullValue()));
			}

			@Test
			@DisplayName("should create the directory")
			void shouldCreateDirectory() {
				assertThat(file.isDirectory(), is(true));
				assertThat(file.exists(), is(true));
			}
		}

		@Nested
		@DisplayName("when passed in a directory that exists")
		class WhenDirectoryExists {
			@BeforeEach
			void setUp() {
				file = new File(parent, "something");
				file.mkdirs();
				assertThat(file.exists(), is(true));
				executeMkdirs();
			}

			@AfterEach
			void tearDown() {
				file.delete();
			}

			@Test
			@DisplayName("should succeed")
			void shouldSucceed() {
				assertThat(ex, is(nullValue()));
				assertThat(file.exists(), is(true));
				assertThat(file.isDirectory(), is(true));
			}
		}

		@Nested
		@DisplayName("when passed in a directory that does not exist")
		class WhenDirectoryDoesNotExist {
			@BeforeEach
			void setUp() {
				file = new File(parent, "something");
				assertThat(file.exists(), is(false));
				executeMkdirs();
			}

			@AfterEach
			void tearDown() {
				file.delete();
			}

			@Test
			@DisplayName("should succeed")
			void shouldSucceed() {
				assertThat(ex, is(nullValue()));
				assertThat(file.exists(), is(true));
				assertThat(file.isDirectory(), is(true));
			}
		}

		@Nested
		@DisplayName("when passed null")
		class WhenNull {
			@BeforeEach
			void setUp() {
				file = null;
				executeMkdirs();
			}

			@Test
			@DisplayName("should throw an IllegalArgumentException")
			void shouldThrowIllegalArgumentException() {
				assertThat(ex, is(not(nullValue())));
				assertThat(ex, instanceOf(IllegalArgumentException.class));
			}
		}
	}

	@Nested
	@DisplayName("rmdirs")
	class Rmdirs {

		@BeforeEach
		void setUp() {
			fileService = new FileServiceImpl();
		}

		@Test
		@DisplayName("should delete empty directories but stop at 'to'")
		void shouldDeleteEmptyDirectories() throws IOException {
			Path p0 = Files.createTempDirectory(null);
			Path p1 = Files.createTempDirectory(p0, null);
			Path p2 = Files.createTempDirectory(p1, null);

			fileService.rmdirs(p2.toFile(), p0.toFile());

			assertThat(p2.toFile().exists(), is(false));
			assertThat(p1.toFile().exists(), is(false));
			assertThat(p0.toFile().exists(), is(true));
		}

		@Test
		@DisplayName("should reject files")
		void shouldRejectFiles() throws IOException {
			Path tempFile = Files.createTempFile(null, null);

			try {
				fileService.rmdirs(tempFile.toFile(), null);
				fail("unexpected");
			} catch (IOException e) {
				assertThat(e, is(not(nullValue())));
			}
		}

		@Test
		@DisplayName("should leave directories that are not empty")
		void shouldLeaveNonEmptyDirectories() throws IOException {
			Path p0 = Files.createTempDirectory(null);
			Path p1 = Files.createTempDirectory(p0, null);
			Path f1 = Files.createTempFile(p1, null, null);
			Path p2 = Files.createTempDirectory(p1, null);

			fileService.rmdirs(p2.toFile(), p0.toFile());

			assertThat(p2.toFile().exists(), is(false));
			assertThat(p1.toFile().exists(), is(true));
			assertThat(f1.toFile().exists(), is(true));
			assertThat(p0.toFile().exists(), is(true));
		}

		@Test
		@DisplayName("should do nothing when 'from' and 'to' are the same")
		void shouldDoNothingWhenSame() throws IOException {
			Path p0 = Files.createTempDirectory(null);

			fileService.rmdirs(p0.toFile(), p0.toFile());

			assertThat(p0.toFile().exists(), is(true));
		}
	}
}
