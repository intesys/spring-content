package org.springframework.content.solr;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import internal.org.springframework.content.fragments.SearchableImpl;
import internal.org.springframework.content.solr.SolrFulltextIndexServiceImpl;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpJdkSolrClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.search.Searchable;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EnableFullTextSolrIndexingTest.TestConfiguration.class)
public class EnableFullTextSolrIndexingTest {

	@Autowired
	private ApplicationContext context;

	@Nested
	@DisplayName("EnableFullTextSolrIndexing")
	class Enablefulltextsolrindexing {

		@Test
		@DisplayName("should have a SolrProperties bean")
		void shouldHaveASolrPropertiesBean() throws Exception {
			assertThat(context.getBean(SolrProperties.class), is(not(nullValue())));
		}

		@Test
		@DisplayName("should have a Solr indexing store event handler bean")
		void shouldHaveASolrIndexingStoreEventHandlerBean() throws Exception {
			assertThat(context.getBean(SolrIndexerStoreEventHandler.class), is(not(nullValue())));
		}

		@Test
		@DisplayName("should have a Searchable implementation bean")
		void shouldHaveASearchableImplementationBean() throws Exception {
			assertThat(context.getBeansOfType(SearchableImpl.class), is(not(nullValue())));
		}

		@Test
		@DisplayName("should have a solr-based fulltext index service bean")
		void shouldHaveASolrBasedFulltextIndexServiceBean() throws Exception {
			assertThat(context.getBean(IndexService.class), is(instanceOf(SolrFulltextIndexServiceImpl.class)));
		}
	}

	@Configuration
	@EnableFilesystemStores
	@EnableFullTextSolrIndexing
	public static class TestConfiguration {

		@Bean
		public SolrClient solrClient() {
			SolrClient sc = new HttpJdkSolrClient.Builder("http://some/url").build();
			return sc;
		}

		@Bean
		FileSystemResourceLoader fileSystemResourceLoader() throws IOException {
			return new FileSystemResourceLoader(Files.createTempDirectory("").toFile().getAbsolutePath());
		}

		@Bean
		public ConversionService contentConversionService() {
			return mock(ConversionService.class);
		}
	}

	public interface TContentStore extends ContentStore<Object, Serializable>, Searchable<Serializable> {}

}
