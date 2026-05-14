package internal.org.springframework.content.commons.storeservice;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import internal.org.springframework.content.commons.store.factory.StoreFactory;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.Stores;
import org.springframework.context.ApplicationContext;

@DisplayName("StoresImpl")
public class StoresImplTest {

	private StoresImpl contentRepoService;

	private ApplicationContext context;
	private StoreFactory mockFactory;

	@BeforeEach
	void setUp() throws Exception {
		context = mock(ApplicationContext.class);
		contentRepoService = new StoresImpl(context);
	}

	@Nested
	@DisplayName("given no factories")
	class NoFactories {
		@BeforeEach
		void setUp() throws Exception {
			when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{});
			contentRepoService.afterPropertiesSet();
		}
		@Test
		@DisplayName("should always return empty")
		void alwaysReturnEmpty() {
			assertThat(contentRepoService.getStores(Store.class),
					is(new StoreInfo[] {}));
		}
	}

	@Nested
	@DisplayName("given a ContentStore factory")
	class ContentStoreFactory {
		@BeforeEach
		void setUp() throws Exception {
			mockFactory = mock(StoreFactory.class);
			Store store = mock(ContentStore.class);
			when(mockFactory.getStore()).thenReturn(store);
			when(mockFactory.getStoreInterface())
					.thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return ContentRepositoryInterface.class;
						}
					});

			when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
			when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
			when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);
			contentRepoService.afterPropertiesSet();
		}
		@Test
		@DisplayName("should return store info")
		void returnStoreInfo() {
			StoreInfo[] infos = contentRepoService.getStores(Store.class);
			assertThat(infos.length, is(1));
		}
		@Test
		@DisplayName("should return associativestore info")
		void returnAssociativeStoreInfo() {
			StoreInfo[] infos = contentRepoService
					.getStores(AssociativeStore.class);
			assertThat(infos.length, is(1));
		}
		@Test
		@DisplayName("should return content store info")
		void returnContentStoreInfo() {
			StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
			assertThat(infos.length, is(1));
		}
	}

	@Nested
	@DisplayName("given a Store factory")
	class StoreFactoryTest {
		@BeforeEach
		void setUp() throws Exception {
			mockFactory = mock(StoreFactory.class);
			Store store = mock(Store.class);
			when(mockFactory.getStore()).thenReturn(store);
			when(mockFactory.getStoreInterface())
					.thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return StoreInterface.class;
						}
					});

			when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
			when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
			when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);
			contentRepoService.afterPropertiesSet();
		}
		@Test
		@DisplayName("should return store info")
		void returnStoreInfo() {
			StoreInfo[] infos = contentRepoService.getStores(Store.class);
			assertThat(infos.length, is(1));
		}
		@Test
		@DisplayName("should return associativestore info")
		void returnAssociativeStoreInfo() {
			StoreInfo[] infos = contentRepoService
					.getStores(AssociativeStore.class);
			assertThat(infos.length, is(0));
		}
		@Test
		@DisplayName("should return no content store info")
		void noContentStoreInfo() {
			StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
			assertThat(infos.length, is(0));
		}
	}

	@Nested
	@DisplayName("given an AssociativeStore factory")
	class AssociativeStoreFactory {
		@BeforeEach
		void setUp() throws Exception {
			mockFactory = mock(StoreFactory.class);
			Store store = mock(AssociativeStore.class);
			when(mockFactory.getStore()).thenReturn(store);
			when(mockFactory.getStoreInterface())
					.thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return AssociativeStoreInterface.class;
						}
					});

			when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory"});
			when(context.getBean("&testStoreFactory", StoreFactory.class)).thenReturn(mockFactory);
			when(context.getBean("testStoreFactory", Store.class)).thenReturn(store);
			contentRepoService.afterPropertiesSet();
		}
		@Test
		@DisplayName("should return no content store info")
		void noContentStoreInfo() {
			StoreInfo[] infos = contentRepoService.getStores(ContentStore.class);
			assertThat(infos.length, is(0));
		}
		@Test
		@DisplayName("should return store info")
		void returnStoreInfo() {
			StoreInfo[] infos = contentRepoService.getStores(Store.class);
			assertThat(infos.length, is(1));
		}
		@Test
		@DisplayName("should return associativestore info")
		void returnAssociativeStoreInfo() {
			StoreInfo[] infos = contentRepoService
					.getStores(AssociativeStore.class);
			assertThat(infos.length, is(1));
		}
	}

	@Nested
	@DisplayName("given multiple stores")
	class MultipleStores {
		@BeforeEach
		void setUp() throws Exception {
			mockFactory = mock(StoreFactory.class);
			Store store = mock(AssociativeStore.class);
			when(mockFactory.getStore()).thenReturn(store);
			when(mockFactory.getStoreInterface())
					.thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return EntityStoreInterface.class;
						}
					});

			StoreFactory mockFactory2 = mock(StoreFactory.class);
			Store store2 = mock(AssociativeStore.class);
			when(mockFactory2.getStore()).thenReturn(store2);
			when(mockFactory2.getStoreInterface())
					.thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return OtherEntityStoreInterface.class;
						}
					});

			when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory1", "&testStoreFactory2"});
			when(context.getBean("&testStoreFactory1", StoreFactory.class)).thenReturn(mockFactory);
			when(context.getBean("&testStoreFactory2", StoreFactory.class)).thenReturn(mockFactory2);
			when(context.getBean("testStoreFactory1", Store.class)).thenReturn(store);
			when(context.getBean("testStoreFactory2", Store.class)).thenReturn(store2);
			contentRepoService.afterPropertiesSet();
		}
		@Test
		@DisplayName("should return stores that match the filter")
		void matchFilter() {
			StoreInfo[] infos = contentRepoService.getStores(
					AssociativeStore.class, Stores.MATCH_ALL);
			assertThat(infos.length, is(2));
		}
		@Test
		@DisplayName("should not return stores that dont match the filter")
		void noMatchFilter() {
			StoreInfo[] infos = contentRepoService
					.getStores(AssociativeStore.class, new StoreFilter() {
						@Override
						public String name() {
							return "test";
						}

						@Override
						public boolean matches(StoreInfo info) {
							return false;
						}
					});
			assertThat(infos.length, is(0));
		}
	}

	@Nested
	@DisplayName("given multiple stores for the same Entity")
	class MultipleStoresSameEntity {
		@BeforeEach
		void setUp() throws Exception {
			mockFactory = mock(StoreFactory.class);
			Store store = mock(ContentStore.class);
			when(mockFactory.getStore()).thenReturn(store);
			when(mockFactory.getStoreInterface())
					.thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return FsEntityStoreInterface.class;
						}
					});

			StoreFactory mockFactory2 = mock(StoreFactory.class);
			Store store2 = mock(ContentStore.class);
			when(mockFactory2.getStore()).thenReturn(store2);
			when(mockFactory2.getStoreInterface())
					.thenAnswer(new Answer<Object>() {
						@Override
						public Object answer(InvocationOnMock invocation)
								throws Throwable {
							return JpaEntityStoreInterface.class;
						}
					});

			when(context.getBeanNamesForType(StoreFactory.class)).thenReturn(new String[]{"&testStoreFactory1", "&testStoreFactory2"});
			when(context.getBean("&testStoreFactory1", StoreFactory.class)).thenReturn(mockFactory);
			when(context.getBean("&testStoreFactory2", StoreFactory.class)).thenReturn(mockFactory2);
			when(context.getBean("testStoreFactory1", Store.class)).thenReturn(store);
			when(context.getBean("testStoreFactory2", Store.class)).thenReturn(store2);
			contentRepoService.afterPropertiesSet();
		}

		@Test
		@DisplayName("should return stores that match the filter")
		void matchFilter() {
			StoreInfo[] infos = contentRepoService.getStores(ContentStore.class, Stores.MATCH_ALL);
			assertThat(infos.length, is(2));
		}
	}

	public interface StoreInterface extends Store<String> {
	}

	public interface AssociativeStoreInterface extends AssociativeStore<Object, String> {
	}

	public interface ContentRepositoryInterface extends ContentStore<Object, String> {
	}

	public static class Entity {
	};

	public static class OtherEntity {
	};

	public interface EntityStoreInterface extends AssociativeStore<Entity, String> {
	}

	public interface OtherEntityStoreInterface
			extends AssociativeStore<OtherEntity, String> {
	}

	public interface FsEntityStoreInterface extends ContentStore<Entity, String> {
	}

	public interface JpaEntityStoreInterface extends ContentStore<Entity, String> {
	}
}
