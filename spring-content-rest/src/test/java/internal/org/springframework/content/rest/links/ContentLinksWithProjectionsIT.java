package internal.org.springframework.content.rest.links;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import javax.sql.DataSource;

import org.hamcrest.beans.HasPropertyWithValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import com.theoryinpractise.halbuilder.api.ReadableRepresentation;
import com.theoryinpractise.halbuilder.api.RepresentationFactory;
import com.theoryinpractise.halbuilder.standard.StandardRepresentationFactory;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        ContentLinksWithProjectionsIT.StoreConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentLinksWithProjectionsIT {

	@Autowired
	TEntityRepository repository;

	@Autowired
	TEntityStore store;

	@Autowired
	private WebApplicationContext context;

	@Nested
	@DisplayName("Content Links with Entity Projection")
	class ContentLinksWithProjectionTests {

		private MockMvc mvc;
		private TEntity testEntity;

		@BeforeEach
		void setup() {
			mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		@Nested
		@DisplayName("given content is associated")
		class GivenContentAssociated {

			@BeforeEach
			void init() {
				testEntity = new TEntity();
				testEntity.setName("foo");
				testEntity = store.setContent(testEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				testEntity = repository.save(testEntity);
			}

			@Test
			@DisplayName("a GET to /{api}?/{repository}/{id}?projection=id should provide a response with a content link")
			void getWithProjectionShouldProvideContentLink() throws Exception {
				MockHttpServletResponse response = mvc.perform(get("/tEntities/" + testEntity.getId() + "?projection=customTEntity")
						.accept("application/hal+json"))
						.andExpect(status().isOk()).andReturn().getResponse();
				assertThat(response, is(not(nullValue())));

				RepresentationFactory representationFactory = new StandardRepresentationFactory();
				ReadableRepresentation halResponse = representationFactory
						.readRepresentation("application/hal+json",
								new StringReader(response.getContentAsString()));

				assertThat(halResponse, is(not(nullValue())));
				assertThat(halResponse.getLinksByRel("content"), is(not(nullValue())));
				assertThat(halResponse.getLinksByRel("content"), hasItems(new HasPropertyWithValue("href", matchesPattern("http://localhost/tEntities/" + testEntity.getId() + "/content"))));
			}
		}
	}

	@Entity
	@Getter
	@Setter
	@NoArgsConstructor
	public static class TEntity {

	    @Id @GeneratedValue private Long id;
	    private String name;
	    private @ContentId UUID contentId;
	    private @ContentLength Long len;
	    private @MimeType String mimeType;
	}

	@Projection(
        name = "customTEntity",
        types = { TEntity.class })
      public interface CustomTEntity {

          String getName();
    }

	public interface TEntityRepository extends JpaRepository<TEntity,Long> {};
    public interface TEntityStore extends FilesystemContentStore<TEntity,UUID> {};

    @Configuration
    @EnableJpaRepositories(basePackages = "internal.org.springframework.content.rest.links", considerNestedRepositories=true)
    @EnableTransactionManagement
    @EnableFilesystemStores(basePackages = "internal.org.springframework.content.rest.links")
    public static class StoreConfig {

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
            factory.setPackagesToScan(packagesToScan());
            factory.setPersistenceUnitName("content-links-with-projections");
            factory.setDataSource(dataSource());
            factory.afterPropertiesSet();

            return factory;
        }

        protected String[] packagesToScan() {
            return new String[] {
                "internal.org.springframework.content.rest.links"
            };
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            return new JpaTransactionManager();
        }

        @Bean
        FileSystemResourceLoader fileSystemResourceLoader() {
            return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
        }

        @Bean
        public File filesystemRoot() {
            File baseDir = new File(System.getProperty("java.io.tmpdir"));
            File filesystemRoot = new File(baseDir, "spring-content-controller-tests");
            filesystemRoot.mkdirs();
            return filesystemRoot;
        }
    }
}
