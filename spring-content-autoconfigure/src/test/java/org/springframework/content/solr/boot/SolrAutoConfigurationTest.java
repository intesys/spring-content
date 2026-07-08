package org.springframework.content.solr.boot;

import internal.org.springframework.content.elasticsearch.boot.autoconfigure.ElasticsearchAutoConfiguration;
import internal.org.springframework.content.s3.boot.autoconfigure.S3ContentAutoConfiguration;
import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import internal.org.springframework.content.solr.boot.autoconfigure.SolrAutoConfiguration;
import org.apache.solr.client.solrj.SolrClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.solr.DeprecatedSolrIndexerStoreEventHandler;
import org.springframework.content.solr.SolrIndexerStoreEventHandler;
import org.springframework.content.solr.SolrProperties;
import org.springframework.context.annotation.Bean;

@DisplayName("solr")
public class SolrAutoConfigurationTest {

   private ApplicationContextRunner contextRunner;

   @Nested
   @DisplayName("given an application context with a SolrClient bean and SolrAutoConfiguration")
   class ContextWithSolrClient {

      @BeforeEach
      void setUp() {
         contextRunner = new ApplicationContextRunner()
           .withConfiguration(AutoConfigurations.of(SolrAutoConfiguration.class));
      }

      @Test
      @DisplayName("should include the autoconfigured annotated event handler bean")
      void shouldIncludeEventHandler() {
         contextRunner.withUserConfiguration(TestConfig.class).run((context) -> {
            Assertions.assertThat(context).getBean("solrFulltextEventListener").isNotNull();
         });
      }
   }

   @SpringBootApplication(exclude = {ElasticsearchAutoConfiguration.class, S3ContentAutoConfiguration.class})
   public static class TestConfig {

      @Autowired
      private SolrProperties props;
      @Autowired
      private SolrClient solrClient;

      public TestConfig() {
      }

      @Bean
      public IndexService solrIndexService() {
         return new SolrFulltextIndexServiceImpl(solrClient, props);
      }

      @Bean
      public Object deprecatedSolrFulltextEventListener() {
         return new DeprecatedSolrIndexerStoreEventHandler(solrIndexService());
      }

      @Bean
      public Object solrFulltextEventListener() {
         return new SolrIndexerStoreEventHandler(solrIndexService());
      }
   }
}
