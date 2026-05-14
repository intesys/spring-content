package internal.org.springframework.content.solr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.solr.SolrProperties;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SolrFulltextIndexServiceImplTest {

	private SolrFulltextIndexServiceImpl indexer;

	private TEntity entity;
	private InputStream content;

	private Exception e;

	private SolrClient solr;
	private SolrProperties props;

	@Nested
	@DisplayName("#index")
	class Index {

		@BeforeEach
		void setUp() throws Exception {
			solr = mock(SolrClient.class);
			props = new SolrProperties();

			entity = new TEntity("12345");
			content = new ByteArrayInputStream("foo".getBytes());

			indexer = new SolrFulltextIndexServiceImpl(solr, props);
		}

		@Nested
		@DisplayName("when solr throws a SolrServerException")
		class WhenSolrThrowsASolrserverexception {

			@BeforeEach
			void setUp() throws Exception {
				when(solr.request(any(SolrRequest.class), any())).thenThrow(new SolrServerException("badness"));
				try {
					indexer.index(entity, content);
				} catch (Exception ex) {
					e = ex;
				}
			}

			@Test
			@DisplayName("should throw a StoreAccessException")
			void shouldThrowAStoreaccessexception() throws Exception {
				assertThat(e, is(instanceOf(StoreAccessException.class)));
				assertThat(e.getCause().getMessage(), containsString("badness"));
			}
		}

		@Nested
		@DisplayName("when solr throws an IOException")
		class WhenSolrThrowsAnIoexception {

			@BeforeEach
			void setUp() throws Exception {
				when(solr.request(any(SolrRequest.class), any())).thenThrow(new IOException("badness"));
				try {
					indexer.index(entity, content);
				} catch (Exception ex) {
					e = ex;
				}
			}

			@Test
			@DisplayName("should throw a StoreAccessException")
			void shouldThrowAStoreaccessexception() throws Exception {
				assertThat(e, is(instanceOf(StoreAccessException.class)));
				assertThat(e.getCause().getMessage(), containsString("badness"));
			}
		}
	}

	@Nested
	@DisplayName("#unindex")
	class Unindex {

		@BeforeEach
		void setUp() throws Exception {
			solr = mock(SolrClient.class);
			props = new SolrProperties();

			entity = new TEntity("12345");

			indexer = new SolrFulltextIndexServiceImpl(solr, props);
		}

		@Nested
		@DisplayName("when solr throws a SolrServerException")
		class WhenSolrThrowsASolrserverexception {

			@BeforeEach
			void setUp() throws Exception {
				when(solr.request(any(SolrRequest.class), any())).thenThrow(new SolrServerException("badness"));
				try {
					indexer.unindex(entity);
				} catch (Exception ex) {
					e = ex;
				}
			}

			@Test
			@DisplayName("should throw a StoreAccessException")
			void shouldThrowAStoreaccessexception() throws Exception {
				assertThat(e, is(instanceOf(StoreAccessException.class)));
				assertThat(e.getCause().getMessage(), containsString("badness"));
			}
		}

		@Nested
		@DisplayName("when solr throws an IOException")
		class WhenSolrThrowsAnIoexception {

			@BeforeEach
			void setUp() throws Exception {
				when(solr.request(any(SolrRequest.class), any())).thenThrow(new IOException("badness"));
				try {
					indexer.unindex(entity);
				} catch (Exception ex) {
					e = ex;
				}
			}

			@Test
			@DisplayName("should throw a StoreAccessException")
			void shouldThrowAStoreaccessexception() throws Exception {
				assertThat(e, is(instanceOf(StoreAccessException.class)));
				assertThat(e.getCause().getMessage(), containsString("badness"));
			}
		}
	}

	@AllArgsConstructor
	@Getter
	@Setter
	private static class TEntity {

		@ContentId
		private String contentId;
	}
}
