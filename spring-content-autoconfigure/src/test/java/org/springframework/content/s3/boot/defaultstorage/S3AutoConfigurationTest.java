package org.springframework.content.s3.boot.defaultstorage;

import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;
import software.amazon.awssdk.services.s3.S3Client;

@DisplayName("S3 auto configuration with default storage")
public class S3AutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        System.setProperty("aws.region", "us-east-1");
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(S3ContentAutoConfiguration.class))
                .withPropertyValues(
                    "spring.content.s3.bucket=test-bucket"
                );
    }

    @AfterEach
    void tearDown() {
        System.clearProperty("aws.region");
    }

    @Nested
    @DisplayName("given a default storage type of s3")
    class DefaultStorageS3 {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.storage.type.default", "s3");
        }
        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.storage.type.default");
        }
        @Test
        @DisplayName("should create an S3Client bean")
        void shouldCreateS3Client() {
            contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(S3Client.class);
            });
        }
    }

    @Nested
    @DisplayName("given a default storage type other than s3")
    class DefaultStorageOther {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.storage.type.default", "fs");
        }
        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.storage.type.default");
        }
        @Test
        @DisplayName("should not create an S3Client bean")
        void shouldNotCreateS3Client() {
            contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                Assertions.assertThat(context).doesNotHaveBean(S3Client.class);
            });
        }
    }

    @Nested
    @DisplayName("given no default storage type")
    class NoDefaultStorage {
        @Test
        @DisplayName("should create an S3Client bean")
        void shouldCreateS3Client() {
            contextRunner.withUserConfiguration(TestConfigWithoutBeans.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(S3Client.class);
            });
        }
    }

    @SpringBootApplication(exclude={ElasticsearchAutoConfiguration.class})
	@EnableS3Stores(basePackageClasses=S3AutoConfigurationTest.class)
	public static class TestConfigWithoutBeans {
		// will be supplied by auto-configuration
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
