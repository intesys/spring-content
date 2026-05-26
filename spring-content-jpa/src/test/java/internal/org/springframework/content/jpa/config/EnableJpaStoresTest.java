package internal.org.springframework.content.jpa.config;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.repository.SetContentParams;
import org.springframework.content.commons.repository.UnsetContentParams;
import org.springframework.content.jpa.config.EnableJpaContentRepositories;
import org.springframework.content.jpa.config.EnableJpaStores;
import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.data.annotation.Id;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import internal.org.springframework.content.jpa.io.DelegatingBlobResourceLoader;
import internal.org.springframework.content.jpa.io.MySQLBlobResource;
import internal.org.springframework.content.jpa.io.SQLServerBlobResource;

public class EnableJpaStoresTest {

	private AnnotationConfigApplicationContext context;

	@Nested
	@DisplayName("EnableJpaStores")
	class EnableJpaStoresTests {

		@Nested
		@DisplayName("given a context and a configuration with a jpa content repository bean")
		class GivenAContextAndConfig {

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
			@DisplayName("should have a content repository bean")
			void shouldHaveContentRepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a delegating blob resource loader")
			void shouldHaveDelegatingBlobResourceLoader() throws Exception {
				assertThat(context.getBean(DelegatingBlobResourceLoader.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a generic blob resource loader")
			void shouldHaveGenericBlobResourceLoader() throws Exception {
				assertThat(context.getBean("genericBlobResourceLoader"),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a MySQL blob resource loader")
			void shouldHaveMySQLBlobResourceLoader() throws Exception {
				BlobResourceLoader loader = (BlobResourceLoader) context.getBean("mysqlBlobResourceLoader");
				assertThat(loader, is(not(nullValue())));
				assertThat(loader.getDatabaseName(), is("MySQL"));
				assertThat(loader.getResource("some-id"), is(instanceOf(MySQLBlobResource.class)));
			}

			@Test
			@DisplayName("should have a SQL Server blob resource loader")
			void shouldHaveSQLServerBlobResourceLoader() throws Exception {
				BlobResourceLoader loader = (BlobResourceLoader) context.getBean("sqlServerBlobResourceLoader");
				assertThat(loader, is(not(nullValue())));
				assertThat(loader.getDatabaseName(), is("Microsoft SQL Server"));
				assertThat(loader.getResource("some-id"), is(instanceOf(SQLServerBlobResource.class)));
			}
		}

		@Nested
		@DisplayName("given a context with an empty configuration")
		class GivenAnEmptyConfig {

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
			@DisplayName("should not contain any jpa repository beans")
			void shouldNotContainJpaRepositoryBeans() throws Exception {
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
	@DisplayName("EnableJpaContentRepositories")
	class EnableJpaContentRepositoriesTests {

		@Nested
		@DisplayName("given a context and a configuration with a jpa content repository bean")
		class GivenAContextAndConfig {

			@BeforeEach
			void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(EnableJpaContentRepositoriesConfig.class);
				context.refresh();
			}

			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}

			@Test
			@DisplayName("should have a content repository bean")
			void shouldHaveContentRepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a delegating blob resource loader")
			void shouldHaveDelegatingBlobResourceLoader() throws Exception {
				assertThat(context.getBean(DelegatingBlobResourceLoader.class),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a generic blob resource loader")
			void shouldHaveGenericBlobResourceLoader() throws Exception {
				assertThat(context.getBean("genericBlobResourceLoader"),
						is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a MySQL blob resource loader")
			void shouldHaveMySQLBlobResourceLoader() throws Exception {
				BlobResourceLoader loader = (BlobResourceLoader) context.getBean("mysqlBlobResourceLoader");
				assertThat(loader, is(not(nullValue())));
				assertThat(loader.getDatabaseName(), is("MySQL"));
				assertThat(loader.getResource("some-id"), is(instanceOf(MySQLBlobResource.class)));
			}

			@Test
			@DisplayName("should have a SQL Server blob resource loader")
			void shouldHaveSQLServerBlobResourceLoader() throws Exception {
				BlobResourceLoader loader = (BlobResourceLoader) context.getBean("sqlServerBlobResourceLoader");
				assertThat(loader, is(not(nullValue())));
				assertThat(loader.getDatabaseName(), is("Microsoft SQL Server"));
				assertThat(loader.getResource("some-id"), is(instanceOf(SQLServerBlobResource.class)));
			}
		}
	}

	@Configuration
	@EnableJpaStores(basePackages = "contains.no.jpa.repositories")
	@Import(InfrastructureConfig.class)
	public static class EmptyConfig {
	}

	@Configuration
	@EnableJpaStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
	}

	@Configuration
	@EnableJpaContentRepositories
	@Import(InfrastructureConfig.class)
	public static class EnableJpaContentRepositoriesConfig {
	}

	@Configuration
	@EnableTransactionManagement
	public static class InfrastructureConfig {
		@Bean
		public DataSource dataSource() {
			EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
			return builder.setType(EmbeddedDatabaseType.HSQL).build();
		}

		@Bean
		public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
			HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
			vendorAdapter.setDatabase(Database.HSQL);
			vendorAdapter.setGenerateDdl(true);

			LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
			factory.setJpaVendorAdapter(vendorAdapter);
			factory.setPackagesToScan(getClass().getPackage().getName());
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

	public class TestEntity {
		@Id
		private String id;
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}

	@Repository
	public class JpaTestContentRepository implements TestEntityContentRepository {

		@PersistenceContext
		private EntityManager em;

		@Override
		public TestEntity setContent(TestEntity property, InputStream content) {
			return null;
		}

		@Override
		public TestEntity setContent(TestEntity property, Resource resourceContent) {
			return null;
		}

		@Override
		public TestEntity unsetContent(TestEntity property) {
			return null;
		}

		@Override
		public InputStream getContent(TestEntity property) {
			return null;
		}

		@Override
		public Resource getResource(TestEntity entity) {
			return null;
		}

		@Override
		public void associate(TestEntity entity, String id) {
		}

		@Override
		public void unassociate(TestEntity entity) {
		}

		@Override
		public Resource getResource(String id) {
			return null;
		}

		@Override
		public Resource getResource(TestEntity entity, PropertyPath propertyPath) {
			return null;
		}

		@Override
		public Resource getResource(TestEntity entity, PropertyPath propertyPath, GetResourceParams params) {
			return null;
		}

		@Override
		public void associate(TestEntity entity, PropertyPath propertyPath, String id) {
		}

		@Override
		public void unassociate(TestEntity entity, PropertyPath propertyPath) {
		}

		@Override
		public TestEntity setContent(TestEntity property, PropertyPath propertyPath, InputStream contentm) {
			return null;
		}

		@Override
		public TestEntity setContent(TestEntity entity, PropertyPath propertyPath, InputStream content, long contentLen) {
			return null;
		}

		@Override
		public TestEntity setContent(TestEntity entity, PropertyPath propertyPath, InputStream content, SetContentParams params) {
			return null;
		}

		@Override
		public TestEntity setContent(TestEntity property, PropertyPath propertyPath, Resource resourceContent) {
			return null;
		}

		@Override
		public TestEntity unsetContent(TestEntity property, PropertyPath propertyPath) {
			return null;
		}

		@Override
		public TestEntity unsetContent(TestEntity entity, PropertyPath propertyPath, UnsetContentParams params) {
			return null;
		}

		@Override
		public InputStream getContent(TestEntity property, PropertyPath propertyPath) {
			return null;
		}
	}
}
