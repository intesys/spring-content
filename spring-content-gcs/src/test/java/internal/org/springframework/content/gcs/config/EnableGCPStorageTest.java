package internal.org.springframework.content.gcs.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.gcs.config.EnableGCPStorage;
import org.springframework.content.gcs.config.GCPStorageConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;

import lombok.Data;

public class EnableGCPStorageTest {

	private AnnotationConfigApplicationContext context;

	static GCPStorageConfigurer configurer;
	static Storage client;

	@Nested
	@DisplayName("EnableGCPStorage")
	class Enablegcpstorage {

		@Nested
		@DisplayName("given a context and a configuration with an GCS ContentStore")
		class GivenAContextAndAConfigurationWithAnGcsContentstore {

			@BeforeEach
			void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(TestConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should have a ContentStore bean")
			void shouldHaveAContentStoreBean() throws Exception {
				assertThat(context.getBean(TestEntityContentStore.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have an Placement Service")
			void shouldHaveAnPlacementService() throws Exception {
				assertThat(context.getBean("gcpStoragePlacementService"),
						is(not(nullValue())));
			}
		}

		@Nested
		@DisplayName("given a context with a configurer")
		class GivenAContextWithAConfigurer {

			@BeforeEach
			void setUp() throws Exception {
				configurer = mock(GCPStorageConfigurer.class);

				context = new AnnotationConfigApplicationContext();
				context.register(ConverterConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should call that configurer to help setup the store")
			void shouldCallThatConfigurerToHelpSetupTheStore() throws Exception {
				verify(configurer).configureGCPStorageConverters(any(ConverterRegistry.class));
			}
		}

		@Nested
		@DisplayName("given a context with an empty configuration")
		class GivenAContextWithAnEmptyConfiguration {

			@BeforeEach
			void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(EmptyConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should not contains any S3 repository beans")
			void shouldNotContainsAnyS3RepositoryBeans() throws Exception {
				try {
					context.getBean(TestEntityContentStore.class);
					fail("expected no such bean");
				}
				catch (NoSuchBeanDefinitionException e) {
					assertThat(true, is(true));
				}
			}
		}
	}

	@Configuration
	@EnableGCPStorage(basePackages = "contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableGCPStorage
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableGCPStorage
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public GCPStorageConfigurer configurer() {
			return configurer;
		}
	}

	@Configuration
	@EnableGCPStorage
	@Import(InfrastructureConfig.class)
	public static class TestConverterConfig {
		@Bean
		public GCPStorageConfigurer configurer() {
			return new GCPStorageConfigurer() {

				@Override
				public void configureGCPStorageConverters(ConverterRegistry registry) {
				}
			};
		}
	}

	public interface TestEntityStore extends AssociativeStore<TestEntity, BlobId> {
	}

	@Configuration
	public static class InfrastructureConfig {

        @Bean
        public static Storage storage() {
            return LocalStorageHelper.getOptions().getService();
        }
	}

	@Data
	public class TestEntity {
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentStore
			extends ContentStore<TestEntity, String> {
	}
}
