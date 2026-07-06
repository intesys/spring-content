package org.springframework.content.fs.boot.autoconfigure;

import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrExtensionAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

@DisplayName("FilesystemContentAutoConfiguration")
public class ContentFilesystemAutoConfigurationTest {

	private ApplicationContextRunner contextRunner;

	@BeforeEach
	void setUp() {
		contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(FilesystemContentAutoConfiguration.class));
	}

	@Nested
	@DisplayName("given a default configuration")
	class DefaultConfiguration {
		@Test
		@DisplayName("should load the context")
		void shouldLoadTheContext() {
			contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
			});
		}
	}

	@Nested
	@DisplayName("given an environment specifying a filesystem root using spring prefix")
	class EnvironmentWithRoot {
		@BeforeEach
		void setUp() {
			System.setProperty("spring.content.fs.filesystem-root",
					"${java.io.tmpdir}/UPPERCASE/NOTATION/");
		}

		@AfterEach
		void tearDown() {
			System.clearProperty("spring.content.fs.filesystem-root");
		}

		@Test
		@DisplayName("should have a filesystem properties bean with the correct root set")
		void shouldHaveCorrectRoot() {
			contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(FilesystemContentAutoConfiguration.FilesystemProperties.class);
				Assertions.assertThat(context).getBean(FilesystemContentAutoConfiguration.FilesystemProperties.class).extracting("filesystemRoot").matches((val) -> val.toString().endsWith("/UPPERCASE/NOTATION/"));
			});
		}
	}

	@Nested
	@DisplayName("given a configuration that contributes a loader bean")
	class ConfigurationWithLoaderBean {
		@Test
		@DisplayName("should have that loader bean in the context")
		void shouldHaveLoaderBean() {
			contextRunner.withUserConfiguration(ConfigWithLoaderBean.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
				Assertions.assertThat(context).getBean(FileSystemResourceLoader.class).extracting("filesystemRoot").matches((val) -> val.toString().endsWith("/some/random/path/"));
			});
		}
	}

	@Nested
	@DisplayName("given a configuration with explicit @EnableFilesystemStores annotation")
	class ConfigurationWithExplicitEnable {
		@Test
		@DisplayName("should load the context")
		void shouldLoadTheContext() {
			contextRunner.withUserConfiguration(ConfigWithExplicitEnableFilesystemStores.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
				Assertions.assertThat(context).getBean(FileSystemResourceLoader.class);
			});
		}
	}

	@SpringBootApplication(exclude={SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	public static class TestConfig {
	}

	@SpringBootApplication
	public static class ConfigWithLoaderBean {

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() {
			return new FileSystemResourceLoader("/some/random/path/");
		}

	}

	@SpringBootApplication
	@EnableFilesystemStores
	public static class ConfigWithExplicitEnableFilesystemStores {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, String> {
	}
}
