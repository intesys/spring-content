package org.springframework.content.fs.boot.autoconfigure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;

@DisplayName("FilesystemProperties")
public class FilesystemPropertiesTest {

	private FilesystemContentAutoConfiguration.FilesystemProperties props;

	@Nested
	@DisplayName("given a filesystem properties with no root set")
	class NoRootSet {
		@BeforeEach
		void setUp() {
			props = new FilesystemContentAutoConfiguration.FilesystemProperties();
		}
		@Test
		@DisplayName("should return a JAVA.IO.TMPDIR based default")
		void shouldReturnDefault() {
			assertThat(props.getFilesystemRoot(),
					startsWith(System.getProperty("java.io.tmpdir")));
		}
	}

	@Nested
	@DisplayName("given a filesystem properties with root set")
	class RootSet {
		@BeforeEach
		void setUp() {
			props = new FilesystemContentAutoConfiguration.FilesystemProperties();
			props.setFilesystemRoot("/some/random/path");
		}
		@Test
		@DisplayName("should return a JAVA.IO.TMPDIR based default")
		void shouldReturnSetRoot() {
			assertThat(props.getFilesystemRoot(), is("/some/random/path"));
		}
	}
}
