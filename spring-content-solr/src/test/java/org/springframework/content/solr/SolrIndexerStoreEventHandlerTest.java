package org.springframework.content.solr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.io.InputStream;
import java.io.Serializable;
import java.util.UUID;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.events.AfterSetContentEvent;
import org.springframework.content.commons.store.events.BeforeUnsetContentEvent;
import org.springframework.content.commons.search.IndexService;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SolrIndexerStoreEventHandlerTest {

	private SolrIndexerStoreEventHandler handler;

	private ContentStore<Object, Serializable> store;
	private IndexService indexer;

	private Object contentEntity;
	private AfterSetContentEvent afterSetEvent;
	private BeforeUnsetContentEvent beforeUnsetEvent;
	private InputStream content;
	private StoreAccessException sae;
	private Throwable e;

	private void onAfterSetContent() {
		try {
			afterSetEvent = new AfterSetContentEvent(contentEntity, store);
			handler.onAfterSetContent(afterSetEvent);
		} catch (Throwable ex) {
			e = ex;
		}
	}

	private void onBeforeUnsetContent() {
		try {
			beforeUnsetEvent = new BeforeUnsetContentEvent(contentEntity, store);
			handler.onBeforeUnsetContent(beforeUnsetEvent);
		} catch (Exception ex) {
			e = ex;
		}
	}

	@Nested
	@DisplayName("SolrIndexerStoreEventHandler")
	class Solrindexerstoreeventhandler {

		@BeforeEach
		void setUp() throws Exception {
			store = mock(ContentStore.class);
			content = mock(InputStream.class);
			indexer = mock(IndexService.class);
			handler = new SolrIndexerStoreEventHandler(indexer);
		}

		@Nested
		@DisplayName("#onAfterSetContent")
		class Onaftersetcontent {

			@Nested
			@DisplayName("given a content entity")
			class GivenAContentEntity {

				@BeforeEach
				void setUp() throws Exception {
					contentEntity = new ContentEntity();
					((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
					((ContentEntity) contentEntity).contentLen = 128L;
					((ContentEntity) contentEntity).mimeType = "text/plain";

					when(store.getContent(eq(contentEntity))).thenReturn(content);

					onAfterSetContent();
				}

				@Test
				@DisplayName("should use the indexer to index the content")
				void shouldUseTheIndexerToIndexTheContent() throws Exception {
					assertThat(e, is(nullValue()));
					verify(indexer).index(eq(contentEntity), eq(content));
				}

				@Nested
				@DisplayName("given the indexer throws an Exception")
				class GivenTheIndexerThrowsAnException {

					@BeforeEach
					void setUp() throws Exception {
						sae = new StoreAccessException("badness");
						doThrow(sae).when(indexer).index(any(Object.class), any(InputStream.class));

						onAfterSetContent();
					}

					@Test
					@DisplayName("should re-throw that exception")
					void shouldReThrowThatException() throws Exception {
						assertThat(e, is(sae));
					}
				}
			}

			@Nested
			@DisplayName("given a content entity with a null contentId")
			class GivenAContentEntityWithANullContentid {

				@BeforeEach
				void setUp() throws Exception {
					contentEntity = new ContentEntity();

					onAfterSetContent();
				}

				@Test
				@DisplayName("should call update")
				void shouldCallUpdate() throws Exception {
					assertThat(e, is(nullValue()));
					verify(indexer, never()).index(any(Object.class), any(InputStream.class));
				}
			}

			@Nested
			@DisplayName("given a bogus content entity")
			class GivenABogusContentEntity {

				@BeforeEach
				void setUp() throws Exception {
					contentEntity = new NotAContentEntity();

					onAfterSetContent();
				}

				@Test
				@DisplayName("should not call index")
				void test() throws Exception {
					assertThat(e, is(nullValue()));
					verify(indexer, never()).index(any(Object.class), any(InputStream.class));
				}
			}
		}

		@Nested
		@DisplayName("#onBeforeUnsetContent")
		class Onbeforeunsetcontent {

			@Nested
			@DisplayName("given a content entity")
			class GivenAContentEntity {

				@BeforeEach
				void setUp() throws Exception {
					contentEntity = new ContentEntity();
					((ContentEntity) contentEntity).contentId = UUID.randomUUID().toString();
					((ContentEntity) contentEntity).contentLen = 128L;
					((ContentEntity) contentEntity).mimeType = "text/plain";

					onBeforeUnsetContent();
				}

				@Test
				@DisplayName("should use the indexer to unindex the content")
				void shouldUseTheIndexerToUnindexTheContent() throws Exception {
					assertThat(e, is(nullValue()));
					verify(indexer).unindex(eq(contentEntity));
				}

				@Nested
				@DisplayName("given an IOException")
				class GivenAIoexception {

					@BeforeEach
					void setUp() throws Exception {
						sae = new StoreAccessException("badness");
						doThrow(sae).when(indexer).unindex(any(Object.class));

						onBeforeUnsetContent();
					}

					@Test
					@DisplayName("should throw a StoreAccessException")
					void shouldThrowAStoreaccessexception() throws Exception {
						assertThat(e, is(sae));
					}
				}
			}

			@Nested
			@DisplayName("given a content entity with a null contentId")
			class GivenAContentEntityWithANullContentid {

				@BeforeEach
				void setUp() throws Exception {
					contentEntity = new ContentEntity();

					onBeforeUnsetContent();
				}

				@Test
				@DisplayName("should not call unindex")
				void shouldCallUpdate() throws Exception {
					assertThat(e, is(nullValue()));
					verify(indexer, never()).unindex(any(Object.class));
				}
			}

			@Nested
			@DisplayName("given a bogus content entity")
			class GivenABogusContentEntity {

				@BeforeEach
				void setUp() throws Exception {
					contentEntity = new NotAContentEntity();

					onBeforeUnsetContent();
				}

				@Test
				@DisplayName("should never attempt deletion")
				void shouldNeverAttemptDeletion() throws Exception {
					assertThat(e, is(nullValue()));
					verify(indexer, never()).unindex(any(Object.class));
				}
			}
		}
	}

	public static class ContentEntity {
		@ContentId
		public String contentId;
		@ContentLength
		public Long contentLen;
		@MimeType
		public String mimeType;
	}

	public static class NotAContentEntity {
	}
}
