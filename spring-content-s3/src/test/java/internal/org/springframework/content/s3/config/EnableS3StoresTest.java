package internal.org.springframework.content.s3.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.URISyntaxException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.EnableS3ContentRepositories;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

import internal.org.springframework.content.s3.it.LocalStack;
import internal.org.springframework.content.s3.io.S3StoreResource;
import lombok.Data;
import software.amazon.awssdk.services.s3.S3Client;

public class EnableS3StoresTest {

	private static final String BUCKET = "aws-test-bucket";

	static {
		System.setProperty("spring.content.s3.bucket", BUCKET);
		System.setProperty("aws.region", "us-east-1");
	}

	private AnnotationConfigApplicationContext context;

	static S3StoreConfigurer configurer;
	static S3Client client;

	@Nested
	@DisplayName("EnableS3Stores")
	class EnableS3StoresTests {

		@Nested
		@DisplayName("given a context and a configuration with an S3 content repository bean")
		class GivenContextWithS3ContentRepo {

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
			@DisplayName("should have a Content Repository bean")
			void shouldHaveContentRepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a Placement Service")
			void shouldHavePlacementService() throws Exception {
				assertThat(context.getBean("s3StorePlacementService"),
						is(not(nullValue())));
			}
		}

		@Nested
		@DisplayName("given a context with a configurer")
		class GivenContextWithConfigurer {

			@BeforeEach
			void setUp() throws Exception {
				configurer = mock(S3StoreConfigurer.class);

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
			void shouldCallConfigurer() throws Exception {
				verify(configurer).configureS3StoreConverters(any(ConverterRegistry.class));
			}
		}

		@Nested
		@DisplayName("given a context with an empty configuration")
		class GivenContextWithEmptyConfig {

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
			@DisplayName("should not contain any S3 repository beans")
			void shouldNotContainS3RepoBeans() throws Exception {
				try {
					context.getBean(TestEntityContentRepository.class);
					fail("expected no such bean");
				} catch (NoSuchBeanDefinitionException e) {
					assertThat(true, is(true));
				}
			}
		}

		@Nested
		@DisplayName("given a context with a multi-tenant configuration")
		class GivenContextWithMultiTenantConfig {

			@BeforeEach
			void setUp() throws Exception {
				client = mock(S3Client.class);

				context = new AnnotationConfigApplicationContext();
				context.register(MultiTenantConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should use the correct client")
			void shouldUseCorrectClient() throws Exception {
				TestEntityContentRepository repo = context.getBean(TestEntityContentRepository.class);
				TestEntity tentity = new TestEntity();
				tentity.setContentId("12345");
				Resource r = repo.getResource(tentity);
				assertThat(((S3StoreResource) r).getClient(), is(client));
			}
		}
	}

	@Nested
	@DisplayName("EnableS3ContentRepositories")
	class EnableS3ContentRepositoriesTests {

		@Nested
		@DisplayName("given a context and a configuration with an S3 content repository bean")
		class GivenContextWithS3ContentRepo {

			@BeforeEach
			void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(EnableS3ContentRepositoriesConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should have a Content Repository bean")
			void shouldHaveContentRepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a Placement Service")
			void shouldHavePlacementService() throws Exception {
				assertThat(context.getBean("s3StorePlacementService"),
						is(not(nullValue())));
			}
		}
	}

	@Configuration
	@EnableS3Stores(basePackages = "contains.no.fs.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableS3Stores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableS3Stores
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public S3StoreConfigurer configurer() {
			return configurer;
		}
	}

	@Configuration
	@EnableS3Stores
	@Import(InfrastructureConfig.class)
	public static class TestConverterConfig {
		@Bean
		public S3StoreConfigurer configurer() {
			return new S3StoreConfigurer() {

				@Override
				public void configureS3StoreConverters(ConverterRegistry registry) {
				}
			};
		}
	}

	public interface TestEntityStore extends AssociativeStore<TestEntity, S3ObjectId> {
	}

	@Configuration
	@EnableS3ContentRepositories
	@Import(InfrastructureConfig.class)
	public static class EnableS3ContentRepositoriesConfig {
	}

	@Configuration
	@EnableS3Stores
	@Import(InfrastructureConfig.class)
	public static class MultiTenantConfig {
		@Bean
		public MultiTenantS3ClientProvider s3Provider() {
			return new MultiTenantS3ClientProvider() {
				@Override
				public S3Client getS3Client() {
					return client;
				}
			};
		}
	}

	@Configuration
	public static class InfrastructureConfig {

		@Autowired
		private Environment env;

		@Bean
		public S3Client client() throws URISyntaxException {
			return LocalStack.getAmazonS3Client();
		}
	}

	@Data
	public class TestEntity {
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
