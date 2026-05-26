package internal.org.springframework.content.s3.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import net.bytebuddy.utility.RandomString;
import org.mockito.ArgumentCaptor;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.config.S3StoreConfigurer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;


public class S3StoreWithEntityConverterIT {

    private static final String BUCKET = "spring-eg-content-s3";
    private static final String OTHER_BUCKET = "other-other-other-bucket";
    private static final String OTHER_OTHER_BUCKET = "other-other-bucket";

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

	private TestEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;
    private S3Client client;

    private static S3Client s3ClientSpy;

    private String resourceLocation;

	@Nested
	@DisplayName("Default Converter")
	class DefaultConverter {

		@BeforeEach
		void setUp() throws Exception {
			context = new AnnotationConfigApplicationContext();
			context.register(TestConfig.class);
			context.refresh();

			repo = context.getBean(TestEntityRepository.class);
			store = context.getBean(TestEntityStore.class);
			client = context.getBean(S3Client.class);

			try {
				HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
						.bucket(OTHER_BUCKET)
						.build();

				client.headBucket(headBucketRequest);
			} catch (NoSuchBucketException ex) {
				CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
						.bucket(OTHER_BUCKET)
						.build();
				client.createBucket(bucketRequest);
			}

			RandomString random = new RandomString(5);
			resourceLocation = random.nextString();
		}

		@AfterEach
		void tearDown() throws Exception {
			context.close();
		}

		@Nested
		@DisplayName("given an entity with content")
		class GivenAnEntityWithContent {

			@BeforeEach
			void setUp() throws Exception {
				entity = new TestEntity();
				entity.setContentType("text/plain");
				entity = repo.save(entity);

				store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
			}

			@Test
			@DisplayName("should store new content in the correct bucket")
			void shouldStoreNewContentInTheCorrectBucket() throws Exception {
				ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
				verify(s3ClientSpy).putObject(captor.capture(), any(RequestBody.class));
				assertThat(captor.getValue().bucket(), is(OTHER_BUCKET));

				HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
						.bucket(OTHER_BUCKET)
						.key(entity.getContentId().toString())
						.build();

				client.headObject(headObjectRequest);
			}

			@Test
			@DisplayName("should have content metadata")
			void shouldHaveContentMetadata() throws Exception {
				assertThat(entity.getContentId(), is(notNullValue()));
				assertThat(entity.getContentId().toString().trim().length(), greaterThan(0));
				assertThat(entity.getContentLen(), is(27L));
			}

			@Nested
			@DisplayName("when content is deleted")
			class WhenContentIsDeleted {

				@BeforeEach
				void setUp() throws Exception {
					resourceLocation = entity.getContentId().toString();
					entity = store.unsetContent(entity, PropertyPath.from("content"));
					entity = repo.save(entity);
				}

				@Test
				@DisplayName("should delete content from the correct bucket")
				void shouldDeleteContentFromTheCorrectBucket() throws Exception {
					ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
					verify(s3ClientSpy).deleteObject(captor.capture());
					assertThat(captor.getValue().bucket(), is(OTHER_BUCKET));

					HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
							.bucket(OTHER_BUCKET)
							.key(resourceLocation)
							.build();

					try {
						client.headObject(headObjectRequest);
						fail("expected object not to exist");
					} catch (NoSuchKeyException nske) {}
				}
			}
		}
	}

	@Nested
	@DisplayName("Custom Converter")
	class CustomConverter {

		@BeforeEach
		void setUp() throws Exception {
			context = new AnnotationConfigApplicationContext();
			context.register(CustomConverterConfig.class, TestConfig.class);
			context.refresh();

			repo = context.getBean(TestEntityRepository.class);
			store = context.getBean(TestEntityStore.class);
			client = context.getBean(S3Client.class);

			try {
				HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
						.bucket(OTHER_OTHER_BUCKET)
						.build();

				client.headBucket(headBucketRequest);
			} catch (NoSuchBucketException ex) {
				CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
						.bucket(OTHER_OTHER_BUCKET)
						.build();
				client.createBucket(bucketRequest);
			}

			RandomString random = new RandomString(5);
			resourceLocation = random.nextString();
		}

		@AfterEach
		void tearDown() throws Exception {
			context.close();
		}

		@Nested
		@DisplayName("given an entity with content")
		class GivenAnEntityWithContent {

			@BeforeEach
			void setUp() throws Exception {
				entity = new TestEntity();
				entity.setContentType("text/plain");
				entity = repo.save(entity);

				store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
			}

			@Test
			@DisplayName("should store new content in the correct bucket")
			void shouldStoreNewContentInTheCorrectBucket() throws Exception {
				ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
				verify(s3ClientSpy).putObject(captor.capture(), any(RequestBody.class));
				assertThat(captor.getValue().bucket(), is(OTHER_OTHER_BUCKET));

				HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
						.bucket(OTHER_OTHER_BUCKET)
						.key(entity.getContentId().toString())
						.build();

				client.headObject(headObjectRequest);
			}

			@Test
			@DisplayName("should have content metadata")
			void shouldHaveContentMetadata() throws Exception {
				assertThat(entity.getContentId(), is(notNullValue()));
				assertThat(entity.getContentId().toString().trim().length(), greaterThan(0));
				assertThat(entity.getContentLen(), is(27L));
			}

			@Nested
			@DisplayName("when content is deleted")
			class WhenContentIsDeleted {

				@BeforeEach
				void setUp() throws Exception {
					resourceLocation = entity.getContentId().toString();
					entity = store.unsetContent(entity, PropertyPath.from("content"));
					entity = repo.save(entity);
				}

				@Test
				@DisplayName("should delete content from the correct bucket")
				void shouldDeleteContentFromTheCorrectBucket() throws Exception {
					ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
					verify(s3ClientSpy).deleteObject(captor.capture());
					assertThat(captor.getValue().bucket(), is(OTHER_OTHER_BUCKET));

					HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
							.bucket(OTHER_OTHER_BUCKET)
							.key(resourceLocation)
							.build();

					try {
						client.headObject(headObjectRequest);
						fail("expected object not to exist");
					} catch (NoSuchKeyException nske) {}
				}
			}
		}
	}

    @Configuration
    @EnableJpaRepositories(basePackages="internal.org.springframework.content.s3.it", considerNestedRepositories = true)
    @EnableS3Stores(basePackages="internal.org.springframework.content.s3.it")
    @Import(InfrastructureConfig.class)
    public static class TestConfig {

        @Bean
        public S3Client client() throws URISyntaxException {
            s3ClientSpy = spy(LocalStack.getAmazonS3Client());
            return s3ClientSpy;
        }
    }

    @Configuration
    public static class CustomConverterConfig {

      @Bean
      public S3StoreConfigurer configurer() {
          return new S3StoreConfigurer() {

              @Override
              public void configureS3StoreConverters(ConverterRegistry registry) {

                  registry.addConverter(new Converter<ContentPropertyInfo<TestEntity, Serializable>, S3ObjectId>() {
                      @Override
                      public S3ObjectId convert(ContentPropertyInfo<TestEntity, Serializable> info) {
                          return new S3ObjectId(OTHER_OTHER_BUCKET, info.contentId().toString());
                      }
                  });

                  registry.addConverter(new Converter<ContentPropertyInfo<FakeEntity, Serializable>, S3ObjectId>() {
                      @Override
                      public S3ObjectId convert(ContentPropertyInfo<FakeEntity, Serializable> info) {
                          throw new IllegalStateException("wrong converter called");
                      }
                  });
              }
          };
       }
    }

    @Configuration
    public static class InfrastructureConfig {

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan("internal.org.springframework.content.s3.it");
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {

            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    public static class FakeEntity {
    }

    @Entity
    @Setter
    @Getter
    @NoArgsConstructor
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        @Bucket
        private String bucket = "other-other-other-bucket";

        @ContentId
        private String contentId;

        @ContentLength
        private long contentLen;

        @MimeType
        private String contentType;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        @MimeType
        private String renditionContentType;

        public TestEntity(String contentId) {
            this.contentId = contentId;
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
    public interface TestEntityStore extends ContentStore<TestEntity, String> {}
}
