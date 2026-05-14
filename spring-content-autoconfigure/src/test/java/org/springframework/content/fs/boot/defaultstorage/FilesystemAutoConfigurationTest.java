package org.springframework.content.fs.boot.defaultstorage;

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
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.support.TestEntity;

@DisplayName("FilesystemContentAutoConfiguration (Default Storage)")
public class FilesystemAutoConfigurationTest {

    private ApplicationContextRunner contextRunner;

    @BeforeEach
    void setUp() {
        contextRunner = new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FilesystemContentAutoConfiguration.class));
    }

    @Nested
    @DisplayName("given a default storage type of fs")
    class DefaultStorageFs {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.storage.type.default", "fs");
        }
        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.storage.type.default");
        }
        @Test
        @DisplayName("should create an FileSystemResourceLoader bean")
        void shouldCreateLoader() {
            contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
            });
        }
    }

    @Nested
    @DisplayName("given a default storage type other than fs")
    class DefaultStorageOther {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.storage.type.default", "s3");
        }
        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.storage.type.default");
        }
        @Test
        @DisplayName("should not create an FileSystemResourceLoader bean")
        void shouldNotCreateLoader() {
            contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                Assertions.assertThat(context).doesNotHaveBean(FileSystemResourceLoader.class);
            });
        }
    }

    @Nested
    @DisplayName("given no default storage type")
    class NoDefaultStorage {
        @Test
        @DisplayName("should create an FileSystemResourceLoader bean")
        void shouldCreateLoader() {
            contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
                Assertions.assertThat(context).hasSingleBean(FileSystemResourceLoader.class);
            });
        }
    }

    @SpringBootApplication(exclude={SolrAutoConfiguration.class, SolrExtensionAutoConfiguration.class, S3ContentAutoConfiguration.class})
	public static class TestConfig {
	}

	public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {
	}

	public interface TestEntityContentRepository extends ContentStore<TestEntity, String> {
	}
}
