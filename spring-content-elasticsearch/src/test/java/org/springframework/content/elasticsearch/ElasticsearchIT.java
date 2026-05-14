package org.springframework.content.elasticsearch;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Iterator;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.apache.commons.io.IOUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CloseIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.client.indices.GetIndexResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.renditions.Renderable;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.IndexService;
import org.springframework.content.commons.search.Searchable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class ElasticsearchIT {

    private static final long POLL_TIMEOUT_MS = 30000;

    @Nested
    @DisplayName("Index Strategy GlobalIndexingStrategy")
    class GlobalIndexingStrategyTests {

        private AnnotationConfigApplicationContext context;
        private DocumentRepository repo;
        private DocumentContentStore store;
        private RestHighLevelClient client;
        private String indexName;

        @BeforeEach
        void setup() throws Exception {
            context = new AnnotationConfigApplicationContext();
            context.register(GlobalIndexingStrategy.class);
            context.register(ElasticsearchConfig.class);
            context.refresh();

            repo = context.getBean(DocumentRepository.class);
            store = context.getBean(DocumentContentStore.class);
            client = context.getBean(RestHighLevelClient.class);
            ((IndexingStrategy)context.getBean(GlobalIndexingStrategy.class)).setup();
            indexName = ((IndexingStrategy)context.getBean(GlobalIndexingStrategy.class)).indexName();
        }

        @AfterEach
        void cleanup() throws Exception {
            assertThat(context, is(not(nullValue())));

            try {
                if (client != null) {
                    GetIndexRequest gir = new GetIndexRequest(indexName);
                    GetIndexResponse resp = client.indices().get(gir, RequestOptions.DEFAULT);
                    assertThat(resp.getIndices().length, is(1));
                }
            } catch (ElasticsearchStatusException ese) {}

            try {
                DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                client.indices().delete(dir, RequestOptions.DEFAULT);
            } catch (ElasticsearchStatusException ese) {}
        }

        @Nested
        @DisplayName("given some documents")
        class GivenSomeDocuments {

            private Document doc1, doc2;

            @BeforeEach
            void setup() throws Exception {
                doc1 = new Document();
                doc1.setTitle("doc 1");
                doc1.setAuthor("author@email.com");
                store.setContent(doc1, GivenSomeDocuments.this.getClass().getResourceAsStream("/one.docx"));
                doc1 = repo.save(doc1);

                doc2 = new Document();
                doc2.setTitle("doc 2");
                doc2.setAuthor("author@email.com");
                store.setContent(doc2, GivenSomeDocuments.this.getClass().getResourceAsStream("/two.rtf"));
                doc2 = repo.save(doc2);
            }

            @AfterEach
            void cleanup() throws Exception {
                if (doc1 != null) {
                    store.unsetContent(doc1);
                    repo.delete(doc1);
                }

                if (doc2 != null) {
                    store.unsetContent(doc2);
                    repo.delete(doc2);
                }
            }

            @Test
            @DisplayName("should index the documents")
            void shouldIndexDocuments() throws Exception {
                GetRequest req = new GetRequest(indexName, doc1.getClass().getName(), doc1.getContentId().toString());
                GetResponse res = client.get(req, RequestOptions.DEFAULT);
                assertThat(res.isExists(), is(true));

                req = new GetRequest(indexName, doc1.getClass().getName(), doc2.getContentId().toString());
                res = client.get(req, RequestOptions.DEFAULT);
                assertThat(res.isExists(), is(true));
            }

            @Test
            @DisplayName("should be possible to close the index")
            void shouldBePossibleToCloseIndex() throws Exception {
                IndexService indexer = (context.getBean(IndexService.class));
                indexer.index(doc1, new ByteArrayInputStream("customized index".getBytes()));

                AcknowledgedResponse resp = client.indices().close(new CloseIndexRequest(indexName), RequestOptions.DEFAULT);
                assertThat(resp.isAcknowledged(), is(true));

                String command = format("curl -X GET %s/_cat/indices/%s?h=status", ElasticsearchTestContainer.getUrl(), indexName);
                Process process = Runtime.getRuntime().exec(command);

                InputStream inputStream = process.getInputStream();
                process.waitFor();

                int exitCode = process.exitValue();
                assertThat(exitCode, is(0));

                assertThat(IOUtils.toString(inputStream), containsString("close"));
            }

            @Nested
            @DisplayName("when the content is searched")
            class WhenContentIsSearched {

                @Test
                @DisplayName("should return the matches")
                void shouldReturnMatches() throws Exception {
                    eventually(() -> {
                        assertThat(store.search("one"), allOf(
                                hasItem(doc1.getContentId()),
                                not(hasItem(doc2.getContentId()))
                        ));
                    });

                    eventually(() -> {
                        assertThat(store.search("two"), allOf(
                                not(hasItem(doc1.getContentId())),
                                hasItem(doc2.getContentId())
                        ));
                    });

                    eventually(() -> {
                        assertThat(store.search("one two"), hasItems(doc1.getContentId(), doc2.getContentId()));
                    });

                    eventually(() -> {
                        assertThat(store.search("+document +one -two"), allOf(
                                hasItem(doc1.getContentId()),
                                hasItem(not(doc2.getContentId()))
                        ));
                    });
                }
            }

            @Nested
            @DisplayName("given a text extracting renderer")
            class GivenTextExtractingRenderer {

                @BeforeEach
                void setup() throws Exception {
                    doc1 = new Document();
                    doc1.setTitle("doc 1");
                    doc1.setAuthor("author@email.com");
                    doc1.setMimeType("image/png");
                    doc1 = store.setContent(doc1, GivenTextExtractingRenderer.this.getClass().getResourceAsStream("/image.png"));
                    doc1 = repo.save(doc1);
                }

                @Test
                @DisplayName("should index the documents")
                void shouldIndexDocuments() throws Exception {
                    eventually(() -> {
                        assertThat(store.search("wisdom"), hasItem(doc1.getContentId()));
                    });
                }
            }

            @Nested
            @DisplayName("given that document is deleted")
            class GivenDocumentDeleted {

                private UUID id1, id2;

                @BeforeEach
                void setup() throws Exception {
                    id1 = doc1.getContentId();
                    store.unsetContent(doc1);
                    repo.delete(doc1);

                    id2 = doc2.getContentId();
                    store.unsetContent(doc2);
                    repo.delete(doc2);
                }

                @AfterEach
                void cleanup() throws Exception {
                    doc1 = null;
                    doc2 = null;
                }

                @Test
                @DisplayName("should delete the record of the content from the index")
                void shouldDeleteRecordFromIndex() throws Exception {
                    GetRequest req = new GetRequest(indexName, doc1.getClass().getName(), id1.toString());
                    GetResponse res = client.get(req, RequestOptions.DEFAULT);
                    assertThat(res.isExists(), is(false));

                    req = new GetRequest(indexName, doc1.getClass().getName(), id2.toString());
                    res = client.get(req, RequestOptions.DEFAULT);
                    assertThat(res.isExists(), is(false));
                }
            }
        }
    }

    @Nested
    @DisplayName("Paging")
    class PagingTests {

        private AnnotationConfigApplicationContext context;
        private DocumentRepository repo;
        private DocumentContentStore store;
        private RestHighLevelClient client;

        @BeforeEach
        void setup() throws Exception {
            context = new AnnotationConfigApplicationContext();
            context.register(EntityIndexingStrategy.class);
            context.register(ElasticsearchConfig.class);
            context.refresh();

            repo = context.getBean(DocumentRepository.class);
            store = context.getBean(DocumentContentStore.class);
            client = context.getBean(RestHighLevelClient.class);

            for (int i=0; i < 10; i++) {
                Document doc = new Document();
                doc.setTitle(format("doc %s", i));
                doc = store.setContent(doc, PagingTests.this.getClass().getResourceAsStream("/one.docx"));
                repo.save(doc);
            }
        }

        @AfterEach
        void cleanup() throws Exception {
            assertThat(context, is(not(nullValue())));

            if (client != null) {
                DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                client.indices().delete(dir, RequestOptions.DEFAULT);
            }
        }

        @Test
        @DisplayName("should return results in pages")
        void shouldReturnResultsInPages() throws Exception {
            eventually(() -> {
                assertThat(store.search("one", PageRequest.of(0, 3)).getTotalElements(), is(10L));
            });
            eventually(() -> {
                var page = store.search("one", PageRequest.of(0, 3));
                assertThat(page.getTotalPages(), is(4));
                assertThat(page.getNumberOfElements(), is(3));
                assertThat(page.getContent().size(), is(3));
            });

            eventually(() -> {
                var page = store.search("one", PageRequest.of(1, 3));
                assertThat(page.getTotalElements(), is(10L));
                assertThat(page.getTotalPages(), is(4));
                assertThat(page.getNumberOfElements(), is(3));
                assertThat(page.getContent().size(), is(3));
            });

            eventually(() -> {
                var page = store.search("one", PageRequest.of(2, 3));
                assertThat(page.getTotalElements(), is(10L));
                assertThat(page.getTotalPages(), is(4));
                assertThat(page.getNumberOfElements(), is(3));
                assertThat(page.getContent().size(), is(3));
            });

            eventually(() -> {
                var page = store.search("one", PageRequest.of(3, 3));
                assertThat(page.getTotalElements(), is(10L));
                assertThat(page.getTotalPages(), is(4));
                assertThat(page.getNumberOfElements(), is(1));
                assertThat(page.getContent().size(), is(1));
            });
        }
    }

    @Nested
    @DisplayName("Custom Attributes")
    class CustomAttributesTests {

        private AnnotationConfigApplicationContext context;
        private DocumentRepository repo;
        private DocumentContentStore store;
        private RestHighLevelClient client;
        private Document doc1, doc2;

        @BeforeEach
        void setup() throws Exception {
            context = new AnnotationConfigApplicationContext();
            context.register(EntityIndexingStrategy.class);
            context.register(CustomAttributesConfig.class);
            context.refresh();

            repo = context.getBean(DocumentRepository.class);
            store = context.getBean(DocumentContentStore.class);
            client = context.getBean(RestHighLevelClient.class);

            doc1 = new Document();
            doc1.setTitle(format("doc 1"));
            doc1.setAuthor("Buck Rogers");
            doc1 = store.setContent(doc1, CustomAttributesTests.this.getClass().getResourceAsStream("/one.docx"));
            repo.save(doc1);

            doc2 = new Document();
            doc2.setTitle(format("doc 2"));
            doc1.setAuthor("Wilma Deering");
            doc2 = store.setContent(doc2, CustomAttributesTests.this.getClass().getResourceAsStream("/one.docx"));
            repo.save(doc2);
        }

        @AfterEach
        void cleanup() throws Exception {
            assertThat(context, is(not(nullValue())));

            if (client != null) {
                DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                client.indices().delete(dir, RequestOptions.DEFAULT);
            }
        }

        @Test
        @DisplayName("should return the specified attributes")
        void shouldReturnSpecifiedAttributes() throws Exception {
            eventually(() -> {
                assertThat(store.search("one", PageRequest.of(0, 10)),
                        allOf(
                                hasItem(doc1.getContentId()),
                                not(hasItem(doc2.getContentId()))
                        ));
            });
        }
    }

    @Nested
    @DisplayName("Custom Return Types")
    class CustomReturnTypesTests {

        private AnnotationConfigApplicationContext context;
        private DocumentRepository repo;
        private DocumentStoreSearchable searchableStore;
        private RestHighLevelClient client;
        private Document doc1;

        @BeforeEach
        void setup() throws Exception {
            context = new AnnotationConfigApplicationContext();
            context.register(EntityIndexingStrategy.class);
            context.register(CustomAttributesConfig.class);
            context.refresh();

            repo = context.getBean(DocumentRepository.class);
            searchableStore = context.getBean(DocumentStoreSearchable.class);
            client = context.getBean(RestHighLevelClient.class);

            doc1 = new Document();
            doc1.setTitle(format("A document about one"));
            doc1.setAuthor("Buck Rogers");
            doc1 = searchableStore.setContent(doc1, CustomReturnTypesTests.this.getClass().getResourceAsStream("/one.docx"));
            repo.save(doc1);
        }

        @AfterEach
        void cleanup() throws Exception {
            assertThat(context, is(not(nullValue())));

            if (client != null) {
                DeleteIndexRequest dir = new DeleteIndexRequest("_all");
                client.indices().delete(dir, RequestOptions.DEFAULT);
            }
        }

        @Test
        @DisplayName("should return results using the custom return type")
        void shouldReturnResultsUsingCustomReturnType() throws Exception {
            eventually(() -> {
                Iterator<FulltextInfo> iterator = searchableStore.search("one").iterator();
                assertThat(iterator, is(not(nullValue())));
                assertThat(iterator.hasNext(), is(true));
                assertThat(iterator.next(), allOf(
                        hasProperty("contentId", is(doc1.getContentId())),
                        hasProperty("highlight", containsString("<em>one</em>")),
                        hasProperty("author", containsString("Buck Rogers"))
                    ));
                assertThat(iterator.hasNext(), is(false));
            });
        }
    }

    private static void eventually(Runnable assertion) {
        long deadline = System.currentTimeMillis() + POLL_TIMEOUT_MS;
        Throwable last = null;
        while (System.currentTimeMillis() < deadline) {
            try {
                assertion.run();
                return;
            } catch (Throwable t) {
                last = t;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        if (last instanceof RuntimeException) throw (RuntimeException) last;
        if (last instanceof Error) throw (Error) last;
        throw new RuntimeException(last);
    }

    public interface DocumentRepository extends CrudRepository<Document, Long> {
        //
    }

    public interface DocumentContentStore extends ContentStore<Document, UUID>, Searchable<UUID>, Renderable<Document> {
        //
    }

    public interface DocumentStoreSearchable extends ContentStore<Document, UUID>, Searchable<FulltextInfo> {
    }

    @Entity
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Document {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        @ContentId
        private UUID contentId;

        @MimeType
        private String mimeType;

        private String title;
        private String author;
    }


    @Getter
    @Setter
    public static class FulltextInfo {

        @ContentId
        private UUID contentId;

        @Highlight
        private String highlight;

        @Attribute(name = "author")
        private String author;
    }
}
