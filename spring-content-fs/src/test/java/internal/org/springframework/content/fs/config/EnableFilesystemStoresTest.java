package internal.org.springframework.content.fs.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.config.EnableFilesystemContentRepositories;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.config.FilesystemStoreConfigurer;
import org.springframework.content.fs.config.FilesystemStoreConverter;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.convert.converter.ConverterRegistry;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings("deprecation")
public class EnableFilesystemStoresTest {

	private AnnotationConfigApplicationContext context;

	// mocks
	static FilesystemStoreConfigurer configurer;

	@Nested
	@DisplayName("EnableFilesystemStores")
	class EnableFilesystemStoresTests {

		@Nested
		@DisplayName("given a context and a configuartion with a filesystem content repository bean")
		class GivenAContextAndAConfiguartionWithAFilesystemContentRepositoryBean {
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
			@DisplayName("should have a ContentRepository bean")
			void shouldHaveAContentrepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}
			@Test
			@DisplayName("should have a filesystem placement service bean")
			void shouldHaveAFilesystemPlacementServiceBean() throws Exception {
				assertThat(context.getBean("filesystemStorePlacementService"),
						is(not(nullValue())));
			}
			@Test
			@DisplayName("should have a FileSystemResourceLoader bean")
			void shouldHaveAFilesystemresourceloaderBean() throws Exception {
				assertThat(context.getBean("fileSystemResourceLoader"),
						is(not(nullValue())));
			}
		}

		@Nested
		@DisplayName("given a context with a configurer")
		class GivenAContextWithAConfigurer {
			@BeforeEach
			void setUp() throws Exception {
				configurer = mock(FilesystemStoreConfigurer.class);

				context = new AnnotationConfigApplicationContext();
				context.register(ConverterConfig.class);
				context.refresh();
			}
			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}
			@Test
			@DisplayName("should call that configurer to help customize the store")
			void shouldCallThatConfigurerToHelpCustomizeTheStore() throws Exception {
				verify(configurer).configureFilesystemStoreConverters(any(ConverterRegistry.class));
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
			@DisplayName("should not contain any filesystem repository beans")
			void shouldNotContainAnyFilesystemRepositoryBeans() throws Exception {
				try {
					context.getBean(TestEntityContentRepository.class);
					fail("expected no such bean");
				}
				catch (NoSuchBeanDefinitionException e) {
					assertThat(true, is(true));
				}
			}
		}
	}

	@Nested
	@DisplayName("EnableFilesystemContentRepositories")
	class EnableFilesystemContentRepositoriesTests {

		@Nested
		@DisplayName("given a context and a configuration with a filesystem content repository bean")
		class GivenAContextAndAConfigurationWithAFilesystemContentRepositoryBean {
			@BeforeEach
			void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(BackwardCompatibilityConfig.class);
				context.refresh();
			}
			@AfterEach
			void tearDown() throws Exception {
				context.close();
			}
			@Test
			@DisplayName("should have a ContentRepository bean")
			void shouldHaveAContentrepositoryBean() throws Exception {
				assertThat(context.getBean(TestEntityContentRepository.class),
						is(not(nullValue())));
			}
		}
	}

	@Configuration
	@EnableFilesystemStores(basePackages = "contains.no.fs.repositories")
	@PropertySource("classpath:/test.properties")
	public static class EmptyConfig {
	}

	@Configuration
	@EnableFilesystemStores
	@PropertySource("classpath:/test.properties")
	public static class TestConfig {

		@Value("${spring.content.fs.filesystemRoot:#{null}}")
		private String filesystemRoot;

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot);
		}
	}

	@Configuration
	@EnableFilesystemStores
	@PropertySource("classpath:/test.properties")
	public static class ConverterConfig {

		@Value("${spring.content.fs.filesystemRoot:#{null}}")
		private String filesystemRoot;

		@Bean
		public FilesystemStoreConverter<UUID, String> uuidConverter() {
			return new FilesystemStoreConverter<UUID, String>() {

				@Override
				public String convert(UUID source) {
					return String.format("/%s", source.toString().replaceAll("-", "/"));
				}
			};
		}

		@Bean
		public FilesystemStoreConfigurer configurer() {
			return configurer;
		}

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot);
		}
	}

	@EnableFilesystemContentRepositories
	@PropertySource("classpath:/test.properties")
	public static class BackwardCompatibilityConfig {

		@Value("${spring.content.fs.filesystemRoot:#{null}}")
		private String filesystemRoot;

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader(filesystemRoot);
		}

	}

	public class TestEntity {
		@ContentId
		private String contentId;
	}

	public interface TestEntityContentRepository
			extends ContentStore<TestEntity, String> {
	}
}

