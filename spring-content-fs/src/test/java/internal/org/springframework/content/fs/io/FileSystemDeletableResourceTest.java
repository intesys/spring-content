package internal.org.springframework.content.fs.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.content.commons.utils.FileService;
import org.springframework.core.io.FileSystemResource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileSystemDeletableResourceTest {

	private FileSystemDeletableResource resource;

	private FileSystemResource delegate;
	private FileService fileService;

	@Nested
	@DisplayName("FileSystemDeletableResource")
	class FileSystemDeletableResourceTests {
		@BeforeEach
		void setUp() throws Exception {
			delegate = mock(FileSystemResource.class);
			fileService = mock(FileService.class);
			resource = new FileSystemDeletableResource(delegate, fileService);
		}
		@Test
		@DisplayName("should delegate isOpen")
		void shouldDelegateIsopen() throws Exception {
			resource.isOpen();
			verify(delegate).isOpen();
		}
		@Test
		@DisplayName("should delegate exists")
		void shouldDelegateExists() throws Exception {
			resource.exists();
			verify(delegate).exists();
		}
		@Test
		@DisplayName("should delegate isReadable")
		void shouldDelegateIsreadable() throws Exception {
			resource.isReadable();
			verify(delegate).isReadable();
		}
		@Test
		@DisplayName("should delegate getInputStream")
		void shouldDelegateGetinputstream() throws Exception {
			resource.getInputStream();
			verify(delegate).getInputStream();
		}
		@Test
		@DisplayName("should delegate isWritable")
		void shouldDelegateIswritable() throws Exception {
			resource.isWritable();
			verify(delegate).isWritable();
		}
		@Test
		@DisplayName("should delegate getOutputStream")
		void shouldDelegateGetoutputstream() throws Exception {
			when(delegate.exists()).thenReturn(true);
			resource.getOutputStream();
			verify(delegate).getOutputStream();
		}
		@Test
		@DisplayName("should delegate getURL")
		void shouldDelegateGeturl() throws Exception {
			resource.getURL();
			verify(delegate).getURL();
		}
		@Test
		@DisplayName("should delegate getURI")
		void shouldDelegateGeturi() throws Exception {
			resource.getURI();
			verify(delegate).getURI();
		}
		@Test
		@DisplayName("should delegate getFile")
		void shouldDelegateGetfile() throws Exception {
			resource.getFile();
			verify(delegate).getFile();
		}
		@Test
		@DisplayName("should delegate contentLength")
		void shouldDelegateContentlength() throws Exception {
			resource.contentLength();
			verify(delegate).contentLength();
		}
		@Test
		@DisplayName("should delegate createRelative")
		void shouldDelegateCreaterelative() throws Exception {
			resource.createRelative("some-path");
			verify(delegate).createRelative("some-path");
		}
		@Test
		@DisplayName("should delegate getFilename")
		void shouldDelegateGetfilename() throws Exception {
			resource.getFilename();
			verify(delegate).getFilename();
		}
		@Test
		@DisplayName("should delegate getDescription")
		void shouldDelegateGetdescription() throws Exception {
			resource.getDescription();
			verify(delegate).getDescription();
		}
	}
}
