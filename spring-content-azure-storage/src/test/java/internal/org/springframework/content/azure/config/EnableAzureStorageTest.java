package internal.org.springframework.content.azure.config;

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
import org.springframework.content.azure.config.AzureStorageConfigurer;
import org.springframework.content.azure.config.BlobId;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import internal.org.springframework.content.azure.it.Azurite;
import lombok.Data;

public class EnableAzureStorageTest {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient client = builder.buildClient().getBlobContainerClient("test");

    static {
        if (!client.exists()) {
            client.create();
        }

        System.setProperty("spring.content.azure.bucket", "azure-test-bucket");
    }

	private AnnotationConfigApplicationContext context;

	static AzureStorageConfigurer configurer;

	@Nested
	@DisplayName("EnableAzureStorage")
	class Enableazurestorage {

		@Nested
		@DisplayName("given a context and a configuration with an Azure ContentStore")
		class GivenAContextAndAConfigurationWithAnAzureContentstore {

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
				assertThat(context.getBean(TestEntityContentStore.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should have an Placement Service")
			void shouldHaveAnPlacementService() throws Exception {
				assertThat(context.getBean("azureStoragePlacementService"), is(not(nullValue())));
			}
		}

		@Nested
		@DisplayName("given a context with a configurer")
		class GivenAContextWithAConfigurer {

			@BeforeEach
			void setUp() throws Exception {
				configurer = mock(AzureStorageConfigurer.class);

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
				verify(configurer).configureAzureStorageConverters(any(ConverterRegistry.class));
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
			@DisplayName("should not contains any Azure Storage beans")
			void shouldNotContainsAnyAzureStorageBeans() throws Exception {
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
	@EnableAzureStorage(basePackages = "contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public AzureStorageConfigurer configurer() {
			return configurer;
		}
	}

	@Configuration
	@EnableAzureStorage
	@Import(InfrastructureConfig.class)
	public static class TestConverterConfig {
		@Bean
		public AzureStorageConfigurer configurer() {
			return new AzureStorageConfigurer() {

				@Override
				public void configureAzureStorageConverters(ConverterRegistry registry) {
				}
			};
		}
	}

	public interface TestEntityStore extends AssociativeStore<TestEntity, BlobId> {
	}

	@Configuration
	public static class InfrastructureConfig {
	    @Bean
	    public BlobServiceClientBuilder builder() {
	        return builder;
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
