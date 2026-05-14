package internal.org.springframework.content.s3.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

import java.io.Serializable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.context.support.GenericApplicationContext;

import software.amazon.awssdk.services.s3.S3Client;

public class S3StoreFactoryBeanTest {

	private S3StoreFactoryBean factory;

	private GenericApplicationContext context = new GenericApplicationContext();
	private S3Client client;
	private PlacementService placer;

	private Store store;

	@Nested
	@DisplayName("S3StoreFactoryBean")
	class S3StoreFactoryBeanTests {

		@BeforeEach
		void setUp() throws Exception {
			client = mock(S3Client.class);
			placer = mock(PlacementService.class);

			context.registerBean("amazonS3", S3Client.class, () -> client);
			context.refresh();

			factory = new S3StoreFactoryBean(S3StoreFactoryBeanTest.TestStore.class);
			factory.setContext(context);
			factory.setClient(client);
			factory.setS3StorePlacementService(placer);
		}

		@Nested
		@DisplayName("#getStore")
		class GetStore {

			@BeforeEach
			void setUp() throws Exception {
				factory.setBeanClassLoader(Thread.currentThread().getContextClassLoader());
			}

			@Nested
			@DisplayName("given a Store")
			class GivenAStore {

				@Test
				@DisplayName("should return a store implementation")
				void shouldReturnStoreImpl() throws Exception {
					store = factory.getStore();
					assertThat(store, is(not(nullValue())));
				}
			}

			@Nested
			@DisplayName("given an AssociativeStore")
			class GivenAnAssociativeStore {

				@Test
				@DisplayName("should return a store implementation")
				void shouldReturnStoreImpl() throws Exception {
					store = factory.getStore();
					assertThat(store, is(not(nullValue())));
				}
			}
		}
	}

	public interface TestStore extends Store<Serializable> {
	}

	private interface TestAssociativeStore extends AssociativeStore<Object, Serializable> {
	}

	private interface TestContentStore extends ContentStore<Object, Serializable> {
	}
}
