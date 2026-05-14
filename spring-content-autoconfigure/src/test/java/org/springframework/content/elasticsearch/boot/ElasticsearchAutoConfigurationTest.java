package org.springframework.content.elasticsearch.boot;

import internal.org.springframework.content.elasticsearch.ElasticsearchIndexServiceImpl;
import internal.org.springframework.content.elasticsearch.ElasticsearchIndexer;
import internal.org.springframework.content.elasticsearch.IndexManager;
import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.elasticsearch.EnableElasticsearchFulltextIndexing;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ElasticsearchAutoConfigurationTest {

    private static RestHighLevelClient client;

    static {
        client = mock(RestHighLevelClient.class);
    }

    @Nested
    @DisplayName("given a context without a rest high level client configured")
    class ContextWithoutClient {
        @Test
        @DisplayName("should create a client")
        void shouldCreateAClient() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithoutClientBean.class).run((context) -> {
                assertThat(context).hasSingleBean(RestHighLevelClient.class);
                assertThat(context).getBean(RestHighLevelClient.class).isNotEqualTo(client);
                assertThat(context).hasSingleBean(ElasticsearchAutoConfiguration.ElasticsearchProperties.class);
                assertThat(context).hasSingleBean(ElasticsearchIndexer.class);

                assertThat(context).hasSingleBean(ElasticsearchIndexServiceImpl.class);
                assertThat(context).hasSingleBean(IndexManager.class);
            });
        }
    }

    @Nested
    @DisplayName("given a context with a rest high level client configured")
    class ContextWithClient {
        @Test
        @DisplayName("should use that client")
        void shouldUseThatClient() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class).run((context) -> {
                assertThat(context).getBean(RestHighLevelClient.class).isEqualTo(client);
            });
        }
    }

    @Nested
    @DisplayName("given a context with auto indexing disabled")
    class ContextWithAutoIndexingDisabled {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.elasticsearch.autoindex", "false");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.elasticsearch.autoindex");
        }

        @Test
        @DisplayName("should not configure the indexing event handler")
        void shouldNotConfigureTheIndexingEventHandler() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class).run((context) -> {
                assertThat(context).doesNotHaveBean(ElasticsearchIndexer.class);
            });
        }
    }

    @Nested
    @DisplayName("given a context with auto-indexing configured")
    class ContextWithAutoIndexingConfigured {
        @BeforeEach
        void setUp() {
            System.setProperty("spring.content.elasticsearch.autoindex", "true");
        }

        @AfterEach
        void tearDown() {
            System.clearProperty("spring.content.elasticsearch.autoindex");
        }

        @Test
        @DisplayName("should load the context")
        void shouldLoadTheContext() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithClientBean.class).run((context) -> {
                assertThat(context).hasSingleBean(ElasticsearchIndexer.class);
            });
        }
    }

    @Nested
    @DisplayName("given a context that already enables elasticsearch fulltext indexing")
    class ContextWithEnablementAlreadyPresent {
        @Test
        @DisplayName("should load the context and not throw a BeanDefinitionOverrideException")
        void shouldLoadTheContextAndNotThrowABeanDefinitionOverrideException() {
            final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(ElasticsearchAutoConfiguration.class));

            contextRunner.withUserConfiguration(ContextWithEnablement.class).run((context) -> {
                assertThat(context).hasSingleBean(RestHighLevelClient.class);
            });
        }
    }

    @Configuration
    public static class ContextWithoutClientBean {
    }

    @Configuration
    public static class ContextWithClientBean {

        @Bean
        public RestHighLevelClient client() {
            return client;
        }
    }

    @Configuration
    @EnableElasticsearchFulltextIndexing
    public static class ContextWithEnablement {

        @Bean
        public RestHighLevelClient client() {
            return client;
        }
    }
}
