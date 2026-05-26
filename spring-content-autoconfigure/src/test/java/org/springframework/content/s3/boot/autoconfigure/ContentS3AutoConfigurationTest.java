package org.springframework.content.s3.boot.autoconfigure;

import internal.org.springframework.content.fs.boot.autoconfigure.FilesystemContentAutoConfiguration;
import internal.org.springframework.content.mongo.boot.autoconfigure.MongoContentAutoConfiguration;
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
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.content.s3.store.S3ContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;
import software.amazon.awssdk.services.s3.S3Client;

import static org.mockito.Mockito.mock;

@DisplayName("S3ContentAutoConfiguration")
public class ContentS3AutoConfigurationTest {

	private static S3Client client;

	static {
		client = mock(S3Client.class);
	}

	private ApplicationContextRunner contextRunner;

	@BeforeEach
	void setUp() {
		contextRunner = new ApplicationContextRunner()
				.withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class));
	}

	@Nested
	@DisplayName("given a configuration with beans")
	class ConfigurationWithBeans {
		@Test
		@DisplayName("should load the context")
		void shouldLoadContext() {
			contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
				Assertions.assertThat(context).hasSingleBean(S3Client.class);
			});
		}
	}

	@Nested
	@DisplayName("given a configuration without any beans")
	class ConfigurationWithoutBeans {
		@Test
		@DisplayName("should load the context")
		void shouldLoadContext() {
			contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
				Assertions.assertThat(context).hasSingleBean(S3Client.class);
			});
		}
	}

	@Nested
	@DisplayName("given a configuration with an explicit @EnableS3Stores annotation")
	class ConfigurationWithExplicitEnable {
		@Test
		@DisplayName("should load the context")
		void shouldLoadContext() {
			contextRunner.withUserConfiguration(TestConfigWithExplicitEnableS3Stores.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(TestEntityContentRepository.class);
				Assertions.assertThat(context).hasSingleBean(S3Client.class);
			});
		}
	}

	@Nested
	@DisplayName("given an environment specifying s3 properties")
	class EnvironmentWithProperties {
		@BeforeEach
		void setUp() {
			System.setProperty("spring.content.s3.endpoint", "http://some-endpoint");
			System.setProperty("spring.content.s3.accessKey", "foo");
			System.setProperty("spring.content.s3.secretKey", "bar");
			System.setProperty("spring.content.s3.pathStyleAccess", "true");
		}

		@AfterEach
		void tearDown() {
			System.clearProperty("spring.content.s3.endpoint");
			System.clearProperty("spring.content.s3.accessKey");
			System.clearProperty("spring.content.s3.secretKey");
			System.clearProperty("spring.content.s3.pathStyleAccess");
		}

		@Test
		@DisplayName("should have a filesystem properties bean with the correct root set")
		void shouldHaveCorrectRoot() {
			contextRunner.withUserConfiguration(TestConfigWithProperties.class).run((context) -> {
				Assertions.assertThat(context).hasSingleBean(S3Client.class);
			});
		}
	}

	@SpringBootApplication(exclude={FilesystemContentAutoConfiguration.class, MongoContentAutoConfiguration.class, SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class})
	public static class TestConfig {

		@Bean
		public S3Client s3Client() {
			return client;
		}
	}

	@SpringBootApplication
	public static class TestConfigWithoutBeans {
		// will be supplied by auto-configuration
	}

	@SpringBootApplication
	@EnableS3Stores
	public static class TestConfigWithExplicitEnableS3Stores {
		// will be supplied by auto-configuration
	}

	@SpringBootApplication
    public static class TestConfigWithProperties {
        // will be supplied by auto-configuration
    }

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends S3ContentStore<TestEntity, String> {
	}
}
