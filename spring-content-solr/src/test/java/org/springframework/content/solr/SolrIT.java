package org.springframework.content.solr;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.request.SolrQuery;
import org.apache.solr.client.solrj.request.UpdateRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.fulltext.Attribute;
import org.springframework.content.commons.fulltext.Highlight;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.search.Searchable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SolrITConfig.class })
public class SolrIT {

    @Autowired
    private DocumentRepository docRepo;
    @Autowired
    private DocumentContentRepository docContentRepo;
    @Autowired
    private DocumentStoreSearchable store;
    @Autowired
    private SolrClient solr;
    @Autowired
    private SolrProperties solrProperties;
    private Document doc, doc2, doc3;
    private UUID id = null;

    @Nested
    @DisplayName("Index")
    class Index {

        @BeforeEach
        void setUp() throws Exception {
            solrProperties.setUser("solr");
            solrProperties.setPassword("SolrRocks");

            doc = new Document();
            doc.setTitle("title of document 1");
            doc.setEmail("author@email.com");
            doc = docRepo.save(doc);
            doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
            doc = docRepo.save(doc);
        }

        @AfterEach
        void tearDown() throws Exception {
            if (docContentRepo != null) {
                docContentRepo.unsetContent(doc);
            }
            if (docRepo != null) {
                docRepo.delete(doc);
            }
            if (solr != null) {
                UpdateRequest req = new UpdateRequest();
                req.deleteByQuery("*");
                req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                req.process(solr, null);
                req.commit(solr, null);
            }
        }

        @Test
        @DisplayName("should index the content of that document")
        void shouldIndexTheContentOfThatDocument() throws Exception {
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
            int size = 0;
            do {
                SolrQuery query = new SolrQuery();
                query.setQuery("foo");
                String fq = format("id:%s\\:%s", Document.class.getCanonicalName(), doc.getContentId());
                query.addFilterQuery(fq);
                QueryRequest request = new QueryRequest(query);
                request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                QueryResponse response = null;
                try {
                    response = request.process(solr);
                    size = response.getResults().size();
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
                if (size == 1) break;
                Thread.sleep(500);
            } while (System.currentTimeMillis() < deadline);
            assertThat(size, is(1));
        }

        @Nested
        @DisplayName("when the content is searched")
        class WhenTheContentIsSearched {

            @Test
            @DisplayName("should return the searched content")
            void shouldReturnTheSearchedContent() throws Exception {
                Iterable<UUID> content = docContentRepo.search("one");
                assertThat(content, CoreMatchers.hasItem(doc.getContentId()));
            }
        }

        @Nested
        @DisplayName("given that documents content is updated")
        class GivenThatDocumentsContentIsUpdated {

            @BeforeEach
            void setUp() throws Exception {
                docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/two.rtf"));
                docRepo.save(doc);
            }

            @Test
            @DisplayName("should index the new content")
            void shouldIndexTheNewContent() throws Exception {
                long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
                int size = 0;
                do {
                    SolrQuery query = new SolrQuery();
                    query.setQuery("bar");
                    String fq = format("id:%s\\:%s", Document.class.getCanonicalName(), doc.getContentId());
                    query.addFilterQuery(fq);
                    QueryRequest request = new QueryRequest(query);
                    request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                    QueryResponse response = null;
                    try {
                        response = request.process(solr);
                        size = response.getResults().size();
                    }
                    catch (Exception e) {
                        fail(e.getMessage());
                    }
                    if (size == 1) break;
                    Thread.sleep(500);
                } while (System.currentTimeMillis() < deadline);
                assertThat(size, is(1));
            }
        }

        @Nested
        @DisplayName("given that document is deleted")
        class GivenThatDocumentIsDeleted {

            @BeforeEach
            void setUp() throws Exception {
                id = doc.getContentId();
                docContentRepo.unsetContent(doc);
                docRepo.delete(doc);
            }

            @Test
            @DisplayName("should delete the record of the content from the index")
            void shouldDeleteTheRecordOfTheContentFromTheIndex() throws Exception {
                SolrQuery query = new SolrQuery();
                query.setQuery("one");
                query.addFilterQuery("id:" + "examples.models.Document\\:" + id);
                query.setFields("content");

                QueryRequest request = new QueryRequest(query);
                request.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());

                QueryResponse response = request.process(solr);
                SolrDocumentList results = response.getResults();

                assertThat(results.size(), is(0));
            }
        }
    }

    @Nested
    @DisplayName("Paging")
    class Paging {

        @Nested
        @DisplayName("Paging")
        class PagingInner {

            @BeforeEach
            void setUp() throws Exception {
                solrProperties.setUser(System.getenv("SOLR_USER"));
                solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

                for (int i=0; i < 10; i++) {
                    Document doc = new Document();
                    doc.setTitle(format("doc %s", i));
                    doc.setEmail("author@email.com");
                    doc = docContentRepo.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
                    docRepo.save(doc);
                }
            }

            @AfterEach
            void tearDown() throws Exception {
                if (docRepo != null && docContentRepo != null) {
                    for (Document doc : docRepo.findAll()) {
                        doc = docContentRepo.unsetContent(doc);
                    }
                    for (Document doc : docRepo.findAll()) {
                        docRepo.deleteById(doc.getId());
                    }
                }
                if (solr != null) {
                    UpdateRequest req = new UpdateRequest();
                    req.deleteByQuery("*");
                    req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                    req.process(solr, null);
                    req.commit(solr, null);
                }
            }

            @Test
            @DisplayName("should return results in pages")
            void shouldReturnResultsInPages() throws Exception {
                long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
                org.springframework.data.domain.Page<UUID> page;
                do {
                    page = docContentRepo.search("foo", PageRequest.of(0, 3));
                    if (page.getNumberOfElements() == 3) break;
                    Thread.sleep(500);
                } while (System.currentTimeMillis() < deadline);
                assertThat(page.getNumberOfElements(), is(3));

                deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
                do {
                    page = docContentRepo.search("foo", PageRequest.of(1, 3));
                    if (page.getNumberOfElements() == 3) break;
                    Thread.sleep(500);
                } while (System.currentTimeMillis() < deadline);
                assertThat(page.getNumberOfElements(), is(3));

                deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
                do {
                    page = docContentRepo.search("foo", PageRequest.of(2, 3));
                    if (page.getNumberOfElements() == 3) break;
                    Thread.sleep(500);
                } while (System.currentTimeMillis() < deadline);
                assertThat(page.getNumberOfElements(), is(3));

                deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
                do {
                    page = docContentRepo.search("foo", PageRequest.of(3, 3));
                    if (page.getNumberOfElements() == 1) break;
                    Thread.sleep(500);
                } while (System.currentTimeMillis() < deadline);
                assertThat(page.getNumberOfElements(), is(1));
            }

            @Test
            @DisplayName("should return specific result page")
            void shouldReturnSpecificResultPage() throws Exception {
                long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
                org.springframework.data.domain.Page<UUID> page;
                do {
                    page = docContentRepo.search("foo", PageRequest.of(3, 3));
                    if (page.getNumberOfElements() == 1) break;
                    Thread.sleep(500);
                } while (System.currentTimeMillis() < deadline);
                assertThat(page.getNumberOfElements(), is(1));
            }
        }
    }

    @Nested
    @DisplayName("Custom Attributes")
    class CustomAttributes {

        @BeforeEach
        void setUp() throws Exception {
            solrProperties.setUser(System.getenv("SOLR_USER"));
            solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

            doc = new Document();
            doc.setTitle("title of document 1");
            doc.setEmail("author@email.com");
            store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
            doc = docRepo.save(doc);

            doc2 = new Document();
            doc2.setTitle("title of document 2");
            doc2.setEmail("author@abc.com");
            store.setContent(doc2, this.getClass().getResourceAsStream("/one.docx"));
            doc2 = docRepo.save(doc2);
        }

        @AfterEach
        void tearDown() throws Exception {
            if (docContentRepo != null) {
                docContentRepo.unsetContent(doc);
            }
            if (docRepo != null) {
                docRepo.delete(doc);
            }
            if (solr != null) {
                UpdateRequest req = new UpdateRequest();
                req.deleteByQuery("*");
                req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                req.process(solr, null);
                req.commit(solr, null);
            }
        }

        @Test
        @DisplayName("should apply the provided attributes and filter query")
        void shouldApplyTheProvidedAttributesAndFilterQuery() throws Exception {
            Iterable<UUID> tmp = docContentRepo.search("one");
            assertThat(tmp, is(not(nullValue())));

            List<UUID> results = new ArrayList<UUID>();
            tmp.forEach(results::add);

            assertThat(results, hasItem(is(doc.getContentId())));
            assertThat(results, not(hasItem(is(doc2.getContentId()))));
        }
    }

    @Nested
    @DisplayName("Custom Return Types")
    class CustomReturnTypes {

        @BeforeEach
        void setUp() throws Exception {
            solrProperties.setUser(System.getenv("SOLR_USER"));
            solrProperties.setPassword(System.getenv("SOLR_PASSWORD"));

            doc = new Document();
            doc.setTitle("title of document 1");
            doc.setEmail("author@email.com");
            doc = docRepo.save(doc);
            doc = store.setContent(doc, this.getClass().getResourceAsStream("/one.docx"));
        }

        @AfterEach
        void tearDown() throws Exception {
            if (docContentRepo != null) {
                docContentRepo.unsetContent(doc);
            }
            if (docRepo != null) {
                docRepo.delete(doc);
            }
            if (solr != null) {
                UpdateRequest req = new UpdateRequest();
                req.deleteByQuery("*");
                req.setBasicAuthCredentials(solrProperties.getUser(), solrProperties.getPassword());
                req.process(solr, null);
                req.commit(solr, null);
            }
        }

        @Test
        @DisplayName("should return results using the return type")
        void shouldReturnResultsUsingTheReturnType() throws Exception {
            long deadline = System.currentTimeMillis() + Duration.ofSeconds(10).toMillis();
            Iterable<FulltextInfo> result = null;
            do {
                result = store.search("one");
                if (result.iterator().hasNext()) break;
                Thread.sleep(500);
            } while (System.currentTimeMillis() < deadline);

            Iterator<FulltextInfo> iterator = result.iterator();
            assertThat(iterator.hasNext(), is(true));
            assertThat(iterator.next(), allOf(
                    hasProperty("contentId", is(doc.getContentId())),
                    hasProperty("highlight", containsString("<em>one</em>")),
                    hasProperty("email", containsString("author@email.com"))
                ));
            assertThat(iterator.hasNext(), is(false));
        }
    }

    @Entity
    @Getter
    @Setter
    @NoArgsConstructor
    public static class Document {

        @Id
        @GeneratedValue(strategy = GenerationType.AUTO)
        private Long id;

        private String title;
        private String email;

        @ContentId
        private UUID contentId;
    }

    public interface DocumentRepository extends CrudRepository<Document, Long> {
    }

    public interface DocumentContentRepository extends ContentStore<Document, UUID>, Searchable<UUID> {
    }

    public interface DocumentStoreSearchable extends ContentStore<Document, UUID>, Searchable<FulltextInfo> {
    }

    @Getter
    @Setter
    public static class FulltextInfo {

        @Id
        private Long id;

        @ContentId
        private UUID contentId;

        @Highlight
        private String highlight;

        @Attribute(name = "email")
        private String email;
    }
}
