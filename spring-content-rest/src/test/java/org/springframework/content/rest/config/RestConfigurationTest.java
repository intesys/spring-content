package org.springframework.content.rest.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.IOException;
import java.nio.file.Files;



import org.junit.runner.RunWith;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.content.fs.store.FilesystemContentStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;





import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;



public class RestConfigurationTest {

   private AnnotationConfigWebApplicationContext context;

   // mocks
   private static ContentRestConfigurer configurer;

   {
      @Nested
    @DisplayName("RestConfiguration")
    class Restconfiguration {
         @BeforeEach
        void setUp() throws Exception {
            configurer = mock(ContentRestConfigurer.class);
         }
         @Nested
    @DisplayName("given a context with a ContentRestConfiguration")
    class GivenAContextWithAContentrestconfiguration {
            @BeforeEach
        void setUp() throws Exception {
               context = new AnnotationConfigWebApplicationContext();
               context.setServletContext(new MockServletContext());
               context.register(TestConfig.class,
                     DelegatingWebMvcConfiguration.class,
                     RepositoryRestMvcConfiguration.class,
                     RestConfiguration.class);
               context.refresh();
            }

            @Test
        @DisplayName("should have a content handler mapping bean")
        void shouldHaveAContentHandlerMappingBean() throws Exception {
               assertThat(context.getBean("contentHandlerMapping"),
                     is(not(nullValue())));
            }

            @Test
        @DisplayName("should have the content rest controllers")
        void shouldHaveTheContentRestControllers() throws Exception {
               assertThat(
                     context.getBean("storeRestController"), is(not(nullValue())));
            }

            @Test
        @DisplayName("should be configurable")
        void shouldBeConfigurable() throws Exception {
               RestConfiguration config = context.getBean(RestConfiguration.class);
               assertThat(config, is(not(nullValue())));

               verify(configurer).configure(config);
            }
         }
      }
   }

   @Configuration
   @EnableFilesystemStores
   public static class TestConfig {

      @Bean
      public FileSystemResourceLoader filesystemRoot() throws IOException {
         return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
      }

      @Bean
      public ContentRestConfigurer configurer() {
         return configurer;
      }
   }

   @Document
   public class TestEntity {
      @Id
      private String id;
      @ContentId
      private String contentId;
   }

   public interface TestEntityRepository extends MongoRepository<TestEntity, String> {
   }

   public interface TestEntityContentRepository extends FilesystemContentStore<TestEntity, String> {
   }
}
