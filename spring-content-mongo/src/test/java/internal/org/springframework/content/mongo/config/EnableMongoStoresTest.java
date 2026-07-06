package internal.org.springframework.content.mongo.config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.UUID;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.mongo.config.EnableMongoContentRepositories;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.content.mongo.config.MongoStoreConverter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

public class EnableMongoStoresTest {

	private AnnotationConfigApplicationContext context;

	@Nested
	@DisplayName("EnableMongoStores")
	class EnableMongoStoresTests {

		@Nested
		@DisplayName("given an enabled configuration with a mongo content repository bean")
		class GivenAnEnabledConfigWithContentRepo {

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
			@DisplayName("should have a mongo content repository bean")
			void shouldHaveContentRepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a mongo store converter")
			void shouldHaveMongoStoreConverter() throws Exception {
				assertThat(context.getBean("mongoStorePlacementService"),
						is(not(nullValue())));
			}
		}

		@Nested
		@DisplayName("given a context with a custom converter")
		class GivenContextWithCustomConverter {

			@BeforeEach
			void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(ConverterConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should use that converter")
			void shouldUseConverter() throws Exception {
				ConversionService converters = (ConversionService) context
						.getBean("mongoStorePlacementService");
				assertThat(
						converters.convert(
								UUID.fromString("e49d5464-26ce-11e7-93ae-92361f002671"),
								String.class),
						is("/e49d5464/26ce/11e7/93ae/92361f002671"));
			}
		}

		@Nested
		@DisplayName("given an enabled configuration with no mongo content repository beans")
		class GivenEnabledConfigWithNoContentRepo {

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
			@DisplayName("should load the context but have no mongo repository beans")
			void shouldLoadContextWithNoRepo() throws Exception {
				try {
					context.getBean(TestEntityContentRepository.class);
					fail("expected no such bean");
				} catch (NoSuchBeanDefinitionException e) {
					assertThat(true, is(true));
				}
			}
		}
	}

	@Nested
	@DisplayName("EnableMongoContentRepositories")
	class EnableMongoContentRepositoriesTests {

		@Nested
		@DisplayName("given an enabled configuration with a mongo content repository bean")
		class GivenEnabledConfigWithContentRepo {

			@BeforeEach
			void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(EnableMongoContentRepositoriesConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should have a mongo content repository bean")
			void shouldHaveContentRepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a mongo store converter")
			void shouldHaveMongoStoreConverter() throws Exception {
				assertThat(context.getBean("mongoStorePlacementService"),
						is(not(nullValue())));
			}
		}
	}

	@Configuration
	@EnableMongoStores(basePackages = "contains.no.mongo.repositores")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableMongoStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableMongoContentRepositories
	@Import(InfrastructureConfig.class)
	public static class EnableMongoContentRepositoriesConfig {
	}

	@Configuration
	@EnableMongoContentRepositories
	@Import(InfrastructureConfig.class)
	public static class ConverterConfig {
		@Bean
		public MongoStoreConverter<UUID, String> uuidConverter() {
			return new MongoStoreConverter<UUID, String>() {

				@Override
				public String convert(UUID source) {
					return String.format("/%s", source.toString().replaceAll("-", "/"));
				}

			};
		}
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoClientConfiguration {

		@Override
		protected String getDatabaseName() {
			return "spring-content";
		}

		@Bean
		public MongoClient mongoClient() {
			return MongoClients.create("mongodb://localhost:27017");
		}

		@Bean
		public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) throws Exception {
			return new GridFsTemplate(mongoDbFactory(), mongoConverter);
		}

		@Bean
		public MongoDatabaseFactory mongoDbFactory() {
			return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
		}
	}

	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}
