package internal.org.springframework.content.jpa.store;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.UUID;

import jakarta.persistence.Id;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.jpa.io.BlobResource;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import internal.org.springframework.content.jpa.io.GenericBlobResource;

@DisplayName("DefaultJpaStoreImpl")
public class DefaultJpaStoreImplTest {

	private DefaultJpaStoreImpl<Object, String> store;

	private BlobResourceLoader blobResourceLoader;

	private TestEntity entity;
	private JakartaTestEntity jakartaAnnotatedEntity;
	private InputStream stream;
	private InputStream inputStream;
	private Resource inputResource;
	private OutputStream outputStream;
	private Resource resource;
	private BlobResource blobResource;
	private String id;
	private Exception e;

	@Nested
	@DisplayName("Store")
	class Store {

		@BeforeEach
		void setUp() throws Exception {
			blobResourceLoader = mock(BlobResourceLoader.class);
			store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
		}

		@Nested
		@DisplayName("#getResource")
		class GetResource {

			@Nested
			@DisplayName("given an id")
			class GivenAnId {

				@BeforeEach
				void setUp() throws Exception {
					id = "1";
				}

				@Test
				@DisplayName("should use the blob resource loader to load a blob resource")
				void shouldUseBlobResourceLoader() throws Exception {
					resource = store.getResource(id);
					verify(blobResourceLoader).getResource(id.toString());
				}
			}
		}
	}

	@Nested
	@DisplayName("AssociativeStore")
	class AssociativeStore {

		@BeforeEach
		void setUp() throws Exception {
			blobResourceLoader = mock(BlobResourceLoader.class);
			store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
		}

		@Nested
		@DisplayName("#getResource")
		class GetResource {

			@Nested
			@DisplayName("when the entity is not associated with a resource")
			class WhenTheEntityIsNotAssociatedWithAResource {

				@BeforeEach
				void setUp() throws Exception {
					entity = new TestEntity();
				}

				@Test
				@DisplayName("should return null")
				void shouldReturnNull() throws Exception {
					resource = store.getResource(entity);
					verify(blobResourceLoader, never()).getResource(anyString());
					assertThat(resource, is(nullValue()));
				}
			}

			@Nested
			@DisplayName("when the entity is associated with a resource")
			class WhenTheEntityIsAssociatedWithAResource {

				@BeforeEach
				void setUp() throws Exception {
					entity = new TestEntity();
					entity.setContentId("12345");
				}

				@Test
				@DisplayName("should load a new resource")
				void shouldLoadNewResource() throws Exception {
					resource = store.getResource(entity);
					verify(blobResourceLoader).getResource(eq("12345"));
				}
			}
		}

		@Nested
		@DisplayName("#associate")
		class Associate {

			@BeforeEach
			void setUp() throws Exception {
				id = "12345";
				entity = new TestEntity();
				resource = mock(BlobResource.class);
				when(blobResourceLoader.getResource(eq("12345")))
						.thenReturn(resource);
				when(resource.contentLength()).thenReturn(20L);
			}

			@Test
			@DisplayName("should set the entity's content ID attribute")
			void shouldSetContentId() throws Exception {
				store.associate(entity, id);
				assertThat(entity.getContentId(), CoreMatchers.is("12345"));
			}
		}

		@Nested
		@DisplayName("#unassociate")
		class Unassociate {

			@BeforeEach
			void setUp() throws Exception {
				id = "12345";
				entity = new TestEntity();
				entity.setContentId(id);
				entity.setContentLen(20L);
			}

			@Test
			@DisplayName("should reset the @ContentId")
			void shouldResetContentId() throws Exception {
				store.unassociate(entity);
				assertThat(entity.getContentId(), is(nullValue()));
			}
		}
	}

	@Nested
	@DisplayName("ContentStore")
	class ContentStore {

		@Nested
		@DisplayName("#getContent")
		class GetContent {

			@BeforeEach
			void setUp() throws Exception {
				blobResourceLoader = mock(BlobResourceLoader.class);
				resource = mock(GenericBlobResource.class);
				entity = new TestEntity("12345");
				when(blobResourceLoader.getResource(entity.getContentId().toString()))
						.thenReturn(resource);
			}

			@Nested
			@DisplayName("given content")
			class GivenContent {

				@BeforeEach
				void setUp() throws Exception {
					stream = new ByteArrayInputStream("hello content world!".getBytes());
					when(resource.getInputStream()).thenReturn(stream);
				}

				@Test
				@DisplayName("should use the blob resource factory to create a new blob resource")
				void shouldUseBlobResourceFactory() throws Exception {
					store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
					inputStream = store.getContent(entity);
					verify(blobResourceLoader).getResource(entity.getContentId().toString());
				}

				@Test
				@DisplayName("should return an inputstream")
				void shouldReturnInputStream() throws Exception {
					store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
					inputStream = store.getContent(entity);
					assertThat(inputStream, is(not(nullValue())));
				}
			}

			@Nested
			@DisplayName("given fetching the input stream fails")
			class GivenFetchingInputStreamFails {

				@BeforeEach
				void setUp() throws Exception {
					when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
				}

				@Test
				@DisplayName("should return null and throw a StoreAccessException")
				void shouldReturnNullAndThrow() throws Exception {
					store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
					try {
						inputStream = store.getContent(entity);
					} catch (Exception ex) {
						e = ex;
					}
					assertThat(inputStream, is(nullValue()));
					assertThat(e, is(instanceOf(StoreAccessException.class)));
					assertThat(e.getCause().getMessage(), is("get-ioexception"));
				}
			}
		}

		@Nested
		@DisplayName("#setContent")
		class SetContent {

			@BeforeEach
			void setUp() throws Exception {
				blobResourceLoader = mock(BlobResourceLoader.class);
				entity = new TestEntity();
				byte[] content = new byte[5000];
				new Random().nextBytes(content);
				inputStream = new ByteArrayInputStream(content);
				resource = mock(BlobResource.class);
				when(blobResourceLoader.getResource(matches(
						"[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")))
						.thenReturn(resource);
				outputStream = mock(OutputStream.class);
				when(((BlobResource) resource).getOutputStream())
						.thenReturn(outputStream);
				when(((BlobResource) resource).getId()).thenReturn(12345);
			}

			@Test
			@DisplayName("should write the contents of the inputstream to the resource's outputstream")
			void shouldWriteToOutputStream() throws Exception {
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
				try {
					store.setContent(entity, inputStream);
				} catch (Exception ex) {
					e = ex;
				}
				verify(outputStream, atLeastOnce()).write(any(byte[].class), anyInt(), anyInt());
			}

			@Test
			@DisplayName("should update the @ContentId field")
			void shouldUpdateContentId() throws Exception {
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
				try {
					store.setContent(entity, inputStream);
				} catch (Exception ex) {
					e = ex;
				}
				assertThat(entity.getContentId(), is("12345"));
			}

			@Test
			@DisplayName("should update the @ContentLength field")
			void shouldUpdateContentLength() throws Exception {
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
				try {
					store.setContent(entity, inputStream);
				} catch (Exception ex) {
					e = ex;
				}
				assertThat(entity.getContentLen(), is(5000L));
			}

			@Nested
			@DisplayName("when the resource output stream throws an IOException")
			class WhenResourceOutputStreamThrows {

				@BeforeEach
				void setUp() throws Exception {
					when(((BlobResource) resource).getOutputStream()).thenThrow(new IOException("set-ioexception"));
				}

				@Test
				@DisplayName("should throw a StoreAccessException")
				void shouldThrowStoreAccessException() throws Exception {
					store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
					try {
						store.setContent(entity, inputStream);
					} catch (Exception ex) {
						e = ex;
					}
					assertThat(e, is(instanceOf(StoreAccessException.class)));
					assertThat(e.getCause().getMessage(), is("set-ioexception"));
				}
			}
		}

		@Nested
		@DisplayName("#setContent from Resource")
		class SetContentFromResource {

			@BeforeEach
			void setUp() throws Exception {
				entity = new TestEntity();
				stream = new ByteArrayInputStream("Hello content world!".getBytes());
				inputResource = new InputStreamResource(stream);
			}

			@Test
			@DisplayName("should delegate")
			void shouldDelegate() throws Exception {
				blobResourceLoader = mock(BlobResourceLoader.class);
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
				try {
					store.setContent(entity, inputResource);
				} catch (Exception ex) {
					e = ex;
				}
				verify(store).setContent(eq(entity), eq(stream));
			}

			@Nested
			@DisplayName("when the resource throws an IOException")
			class WhenResourceThrowsIOException {

				@BeforeEach
				void setUp() throws Exception {
					inputResource = mock(Resource.class);
					when(inputResource.getInputStream()).thenThrow(new IOException("setContent badness"));
				}

				@Test
				@DisplayName("should throw a StoreAccessException")
				void shouldThrowStoreAccessException() throws Exception {
					blobResourceLoader = mock(BlobResourceLoader.class);
					store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
					try {
						store.setContent(entity, inputResource);
					} catch (Exception ex) {
						e = ex;
					}
					assertThat(e, CoreMatchers.is(instanceOf(StoreAccessException.class)));
					assertThat(e.getCause().getMessage(), containsString("setContent badness"));
				}
			}
		}

		@Nested
		@DisplayName("#unsetContent")
		class UnsetContent {

			@BeforeEach
			void setUp() throws Exception {
				blobResourceLoader = mock(BlobResourceLoader.class);
				blobResource = mock(GenericBlobResource.class);
				entity = new TestEntity("12345");
				when(blobResourceLoader.getResource(entity.getContentId().toString()))
						.thenReturn(blobResource);
			}

			@Test
			@DisplayName("should delete the content")
			void shouldDeleteContent() throws Exception {
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
				try {
					store.unsetContent(entity);
				} catch (Exception ex) {
					e = ex;
				}
				verify(blobResource).delete();
			}

			@Nested
			@DisplayName("resource delete throws an Exception")
			class ResourceDeleteThrows {

				@BeforeEach
				void setUp() throws Exception {
					doThrow(new IOException("unset-ioexception")).when(blobResource).delete();
				}

				@Test
				@DisplayName("should throw a StoreAccessException")
				void shouldThrowStoreAccessException() throws Exception {
					store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
					try {
						store.unsetContent(entity);
					} catch (Exception ex) {
						e = ex;
					}
					assertThat(e, is(instanceOf(StoreAccessException.class)));
					assertThat(e.getCause().getMessage(), is("unset-ioexception"));
				}
			}
		}
	}

	@Nested
	@DisplayName("DefaultJpaStoreImpl jakartaAnnotatedEntity")
	class JakartaAnnotatedEntity {

		@Nested
		@DisplayName("Store")
		class Store {

			@BeforeEach
			void setUp() throws Exception {
				blobResourceLoader = mock(BlobResourceLoader.class);
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
			}

			@Nested
			@DisplayName("#getResource")
			class GetResource {

				@Nested
				@DisplayName("given an id")
				class GivenAnId {

					@BeforeEach
					void setUp() throws Exception {
						id = "1";
					}

					@Test
					@DisplayName("should use the blob resource loader to load a blob resource")
					void shouldUseBlobResourceLoader() throws Exception {
						resource = store.getResource(id);
						verify(blobResourceLoader).getResource(id.toString());
					}
				}
			}
		}

		@Nested
		@DisplayName("AssociativeStore")
		class AssociativeStore {

			@BeforeEach
			void setUp() throws Exception {
				blobResourceLoader = mock(BlobResourceLoader.class);
				store = spy(new DefaultJpaStoreImpl(blobResourceLoader, null, 8096));
			}

			@Nested
			@DisplayName("#unassociate jakarta annotated entity")
			class UnassociateJakartaAnnotatedEntity {

				@BeforeEach
				void setUp() throws Exception {
					id = "12345";
					jakartaAnnotatedEntity = new JakartaTestEntity();
					jakartaAnnotatedEntity.setContentId(id);
					jakartaAnnotatedEntity.setContentLen(20L);
				}

				@Test
				@DisplayName("should NOT reset the @ContentId")
				void shouldNotResetContentId() throws Exception {
					store.unassociate(jakartaAnnotatedEntity);
					assertThat(jakartaAnnotatedEntity.getContentId(), CoreMatchers.is(id));
				}
			}
		}
	}

	public static class TestEntity {
		@ContentId
		private String contentId;
		@ContentLength
		private long contentLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = contentId;
		}

		public String getContentId() {
			return this.contentId;
		}

		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		public long getContentLen() {
			return contentLen;
		}

		public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}

	}

	public static class JakartaTestEntity {
		@Id
		@ContentId
		private String contentId;
		@ContentLength
		private long contentLen;

		public JakartaTestEntity() {
			this.contentId = null;
		}

		public JakartaTestEntity(String contentId) {
			this.contentId = contentId;
		}

		public String getContentId() {
			return this.contentId;
		}

		public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		public long getContentLen() {
			return contentLen;
		}

		public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}
}
