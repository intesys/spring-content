package internal.org.springframework.content.rest.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import internal.org.springframework.content.commons.storeservice.StoreInfoImpl;
import internal.org.springframework.content.rest.annotations.ContentStoreRestResource;
import internal.org.springframework.content.rest.support.TestEntity;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.rest.StoreRestResource;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
public class StoreUtilsTest {

	private StoreInfo info;
	private String storePath;

	@Nested
	@DisplayName("#storePath")
	class Storepath {

		@Nested
		@DisplayName("given a content store with no annotation")
		class GivenAContentStoreWithNoAnnotation {
			@BeforeEach
			void setUp() throws Exception {
				ContentStore storeImpl = mock(TestContentStore.class);
				info = new StoreInfoImpl(TestContentStore.class, TestEntity.class, storeImpl);
			}

			@Test
			@DisplayName("should return return 'testEntities'")
			void shouldReturnReturnTestentities() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("testEntities"));
			}
		}

		@Nested
		@DisplayName("given a content store with a deprecated ContentStoreRestResource annotation")
		class GivenAContentStoreWithADeprecatedContentstorerestresourceAnnotation {
			@BeforeEach
			void setUp() throws Exception {
				ContentStore storeImpl = mock(ContentStoreWithDeprecatedAnnotation.class);
				info = new StoreInfoImpl(
						ContentStoreWithDeprecatedAnnotation.class,
						TestEntity.class, storeImpl);
			}

			@Test
			@DisplayName("should return return the specified path")
			void shouldReturnReturnTheSpecifiedPath() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("testEntities"));
			}
		}

		@Nested
		@DisplayName("given a content store with a deprecated ContentStoreRestResource annotation that specifies a path")
		class GivenAContentStoreWithADeprecatedContentstorerestresourceAnnotationThatSpecifiesAPath {
			@BeforeEach
			void setUp() throws Exception {
				ContentStore storeImpl = mock(ContentStoreWithPath.class);
				info = new StoreInfoImpl(ContentStoreWithPath.class,
						TestEntity.class, storeImpl);
			}

			@Test
			@DisplayName("should return return the specified path")
			void shouldReturnReturnTheSpecifiedPath() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("some-path"));
			}
		}

		@Nested
		@DisplayName("given a content store with a StoreRestResource annotation")
		class GivenAContentStoreWithAStorerestresourceAnnotation {
			@BeforeEach
			void setUp() throws Exception {
				ContentStore storeImpl = mock(ContentStoreWithAnnotation.class);
				info = new StoreInfoImpl(ContentStoreWithAnnotation.class,
						TestEntity.class, storeImpl);
			}

			@Test
			@DisplayName("should return return the specified path")
			void shouldReturnReturnTheSpecifiedPath() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("testEntities"));
			}
		}

		@Nested
		@DisplayName("given a content store with a StoreRestResource annotation that specifies a path")
		class GivenAContentStoreWithAStorerestresourceAnnotationThatSpecifiesAPath {
			@BeforeEach
			void setUp() throws Exception {
				ContentStore storeImpl = mock(ContentStoreWithAnotherPath.class);
				info = new StoreInfoImpl(ContentStoreWithAnotherPath.class, TestEntity.class, storeImpl);
			}

			@Test
			@DisplayName("should return return the specified path")
			void shouldReturnReturnTheSpecifiedPath() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("some-other-path"));
			}
		}

		@Nested
		@DisplayName("given a Store with no annotations")
		class GivenAStoreWithNoAnnotations {
			@BeforeEach
			void setUp() throws Exception {
				Store storeImpl = mock(TestStore.class);
				info = new StoreInfoImpl(TestStore.class, null, storeImpl);
			}

			@Test
			@DisplayName("should return 'tests'")
			void shouldReturnTests() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("tests"));
			}
		}

		@Nested
		@DisplayName("given a Store with a StoreRestResource annotation")
		class GivenAStoreWithAStorerestresourceAnnotation {
			@BeforeEach
			void setUp() throws Exception {
				Store storeImpl = mock(TestStoreWithAnnotation.class);
				info = new StoreInfoImpl(TestStoreWithAnnotation.class, null, storeImpl);
			}

			@Test
			@DisplayName("should return 'tests'")
			void shouldReturnTests() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("testWithAnnotations"));
			}
		}

		@Nested
		@DisplayName("given a Store with a StoreRestResource annotation with a path of 'foo'")
		class GivenAStoreWithAStorerestresourceAnnotationWithAPathOfFoo {
			@BeforeEach
			void setUp() throws Exception {
				Store storeImpl = mock(TestStoreWithPath.class);
				info = new StoreInfoImpl(TestStoreWithPath.class, null, storeImpl);
			}

			@Test
			@DisplayName("should return 'tests'")
			void shouldReturnTests() throws Exception {
				storePath = StoreUtils.storePath(info);
				assertThat(storePath, is("foo"));
			}
		}
	}

	public interface TestStore extends Store<String> {}

	@StoreRestResource
	public interface TestStoreWithAnnotation extends Store<String> {}

	@StoreRestResource(path="foo")
	public interface TestStoreWithPath extends Store<String> {}

	public interface TestContentStore extends ContentStore<TestEntity, UUID> {}

	@ContentStoreRestResource
	public interface ContentStoreWithDeprecatedAnnotation extends ContentStore<TestEntity, UUID> {}

	@ContentStoreRestResource(path = "some-path")
	public interface ContentStoreWithPath extends ContentStore<TestEntity, UUID> {}

	@StoreRestResource
	public interface ContentStoreWithAnnotation extends ContentStore<TestEntity, UUID> {}

	@StoreRestResource(path = "some-other-path")
	public interface ContentStoreWithAnotherPath extends ContentStore<TestEntity, UUID> {}

	public interface StoreWithRenderable extends ContentStore<TestEntity, UUID>, Store<UUID>, Renderable<TestEntity> {}
}
