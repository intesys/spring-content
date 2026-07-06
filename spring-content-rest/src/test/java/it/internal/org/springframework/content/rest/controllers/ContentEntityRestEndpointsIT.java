package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.EntityConfig;
import internal.org.springframework.content.rest.support.EventListenerConfig;
import internal.org.springframework.content.rest.support.EventListenerConfig.TestEventListener;
import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity11;
import internal.org.springframework.content.rest.support.TestEntity11Repository;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntity4;
import internal.org.springframework.content.rest.support.TestEntity4ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity4Repository;
import internal.org.springframework.content.rest.support.TestEntity6;
import internal.org.springframework.content.rest.support.TestEntity6Repository;
import internal.org.springframework.content.rest.support.TestEntity9;
import internal.org.springframework.content.rest.support.TestEntity9Repository;
import internal.org.springframework.content.rest.support.TestEntity9Store;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import jakarta.servlet.ServletException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.Charset;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
    StoreConfig.class, EntityConfig.class, org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration.class,
    RepositoryRestMvcConfiguration.class, RestConfiguration.class, HypermediaConfiguration.class, EventListenerConfig.class
})
@Transactional
@ActiveProfiles("store")
@DisplayName("Content Entity REST Endpoints")
class ContentEntityRestEndpointsIT {

    @Autowired
    TestEntityRepository repository;
    @Autowired
    TestEntityContentRepository contentRepository;

    @Autowired
    TestEntity3Repository repo3;
    @Autowired
    TestEntity3ContentRepository store3;

    @Autowired
    TestEntity4Repository repo4;
    @Autowired
    TestEntity4ContentRepository store4;

    @Autowired
    TestEntity6Repository repo6;

    @Autowired
    TestEntity9Repository repo9;
    @Autowired
    TestEntity9Store store9;

    @Autowired
    TestEntity11Repository repo11;

    @Autowired
    TestEventListener eventListener;

    @Autowired
    WebApplicationContext context;

    private MockMvc mvc;

    private TestEntity testEntity;
    private TestEntity3 testEntity3;
    private TestEntity4 testEntity4;
    private TestEntity6 testEntity6;
    private TestEntity9 testEntity9;
    private TestEntity11 testEntity11;

    private Version version;
    private LastModifiedDate lastModifiedDate;

    private Entity entityTests;
    private Content contentTests;
    private Cors corsTests;

    @BeforeEach
    void setup() {

        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // =====================================================
    // UNCORRELATED
    // =====================================================
    @Nested
    @DisplayName("given an entity with a single uncorrelated content properties")
    class Uncorrelated {

        @Nested
        @DisplayName("given the repository and storage are exported to the same URI")
        class SameUri {

            @BeforeEach
            void setup() {

                testEntity3 = repo3.save(new TestEntity3());
                testEntity3.name = "tests";
                testEntity3 = repo3.save(testEntity3);

                entityTests = Entity.tests();
                contentTests = Content.tests();
            }

            @Test
            @DisplayName("a DELETE to /{store}/{id}/softDelete (custom handler)")
            void testDelete()
                throws Exception {

                mvc.perform(delete("/testEntity3s/" + testEntity3.id + "/softDelete")).andExpect(status().is2xxSuccessful());
            }
        }

        @Nested
        @DisplayName("given the repository and storage are exported to different URIs")
        class DifferentUri {

            @BeforeEach
            void setup() {

                testEntity = repository.save(new TestEntity());

                contentTests = Content.tests();
                contentTests.setMvc(mvc);
                contentTests.setUrl("/testEntitiesContent/" + testEntity.getId());
                contentTests.setEntity(testEntity);
                contentTests.setRepository(repository);
                contentTests.setStore(contentRepository);

                corsTests = Cors.tests();
                corsTests.setMvc(mvc);
                corsTests.setUrl("/testEntitiesContent/" + testEntity.getId());
            }

            @Test
            @DisplayName("an OPTIONS request to the repository from a known host")
            void cors()
                throws Exception {

                mvc.perform(
                       options("/testEntities/" + testEntity.getId()).header("Access-Control-Request-Method", "PUT").header("Origin", "http://www.someurl.com"))
                   .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin", "http://www.someurl.com"));
            }
        }

        @Nested
        @DisplayName("given an entity with @Version")
        class VersionOnlySetup {

            @BeforeEach
            void setup() {

                testEntity4 = repo4.save(new TestEntity4());
                version = Version.tests();
            }
        }

        @Nested
        @DisplayName("given an entity with @LastModifiedDate")
        class LastModifiedOnlySetup {

            @BeforeEach
            void setup() {

                testEntity4 = repo4.save(new TestEntity4());
                lastModifiedDate = LastModifiedDate.tests();
            }
        }

        @Nested
        @DisplayName("given an entity with a shared Id and ContentId field")
        class SharedId {

            @BeforeEach
            void setup() {

                testEntity6 = repo6.save(new TestEntity6());
            }

            @Test
            @DisplayName("should return 404 when no content is set")
            void notFound()
                throws Exception {

                mvc.perform(get("/testEntity6s/" + testEntity6.getId()).accept("text/plain")).andExpect(status().isNotFound());
            }
        }
    }

    // =====================================================
    // MULTIPART UNC
    // =====================================================
    @Test
    @DisplayName("given a multipart/form POST to an entity with a single uncorrelated content property")
    void multipartUncorrelated()
        throws Exception {

        // assert content does not exist
        String newContent = "This is some new content";

        MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

        var testEntity4Id = repo4.save(new TestEntity4()).getId();

        // POST the new content
        MockHttpServletResponse response = mvc.perform(
                                                  multipart("/testEntity3s").file(file).contentType("multipart/form-data; boundary=c0de8278").param("name", "foo").param("hidden", "bar")
                                                                            .param("ying", "yang").param("things", "one", "two").param("testEntity4", "/testEntity4s/" + testEntity4Id))

                                              .andExpect(status().isCreated()).andReturn().getResponse();

        String location = response.getHeader("Location");

        Optional<TestEntity3> fetchedEntity = repo3.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
        assertThat(fetchedEntity.get().getName(), is("foo"));
        assertThat(fetchedEntity.get().getHidden(), is(nullValue()));
        assertThat(fetchedEntity.get().getYang(), is("yang"));
        assertThat(fetchedEntity.get().getThings(), hasItems("one", "two"));
        assertThat(fetchedEntity.get().getTestEntity4(), is(not(nullValue())));
        assertThat(fetchedEntity.get().getLen(), is(file.getSize()));
        assertThat(fetchedEntity.get().getOriginalFileName(), is(file.getOriginalFilename()));

        // assert that it now exists
        response = mvc.perform(get(location).accept("text/plain")).andExpect(status().isOk()).andReturn().getResponse();

        assertThat(response.getContentAsString(), is(newContent));
    }

    // =====================================================
    // VERSION ISSUE 2044 (FULL)
    // =====================================================
    @Nested
    @DisplayName("given a multipart/form POST to an entity with a non-default initialized @Version property (#Issue 2044)")
    class VersionIssue2044 {

        @Test
        @DisplayName("with content")
        void withContent()
            throws Exception {

            String newContent = "This is some new content";

            MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

            // POST the entity
            MockHttpServletResponse response = mvc.perform(multipart("/testEntity4s").file(file).param("name", "foo").param("title", "bar"))

                                                  .andExpect(status().isCreated()).andReturn().getResponse();

            String location = response.getHeader("Location");

            // assert that the entity exists
            Optional<TestEntity4> fetchedEntity = repo4.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
            assertThat(fetchedEntity.get().getName(), is("foo"));
            assertThat(fetchedEntity.get().getTitle(), is("bar"));
            assertThat(fetchedEntity.get().getContentId(), is(not(nullValue())));
            assertThat(fetchedEntity.get().getLen(), is(file.getSize()));
            assertThat(fetchedEntity.get().getOriginalFileName(), is(file.getOriginalFilename()));

            // assert that the content now exists
            response = mvc.perform(get(location).accept("text/plain")).andExpect(status().isOk()).andReturn().getResponse();

            assertThat(response.getContentAsString(), is(newContent));
        }

        @Test
        @DisplayName("without content")
        void withoutContent()
            throws Exception {

            // POST the entity
            MockHttpServletResponse response = mvc.perform(multipart("/testEntity4s").param("name", "foo").param("title", "bar"))

                                                  .andExpect(status().isCreated()).andReturn().getResponse();

            String location = response.getHeader("Location");

            // assert that the entity exists
            Optional<TestEntity4> fetchedEntity = repo4.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
            assertThat(fetchedEntity.get().getName(), is("foo"));
            assertThat(fetchedEntity.get().getTitle(), is("bar"));
            assertThat(fetchedEntity.get().getContentId(), is(nullValue()));
            assertThat(fetchedEntity.get().getLen(), is(nullValue()));
            assertThat(fetchedEntity.get().getOriginalFileName(), is(nullValue()));
        }
    }

    // =====================================================
    // CORRELATED
    // =====================================================
    @Test
    @DisplayName("given an entity with a single correlated content property")
    void correlated()
        throws Exception {

        testEntity9 = repo9.save(new TestEntity9());

        String content = "Hello Spring Content World!";
        mvc.perform(put("/testEntity9s/" + testEntity9.id).contextPath("").content(content).contentType("text/plain")).andExpect(status().isCreated());

        Optional<TestEntity9> fetched = repo9.findById(testEntity9.getId());
        assertThat(fetched.isPresent(), is(true));
        assertThat(fetched.get().getContentId(), is(not(nullValue())));
        assertThat(fetched.get().getContentLen(), is(27L));
        assertThat(fetched.get().getContentMimeType(), is("text/plain"));
        assertThat(IOUtils.toString(store9.getContent(fetched.get()), Charset.defaultCharset()), is(content));

        MockHttpServletResponse response =
            mvc.perform(get("/testEntity9s/" + testEntity9.id).contextPath("").accept("text/plain")).andExpect(status().isOk()).andReturn().getResponse();

        assertThat(response, is(not(nullValue())));
        assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
    }

    // =====================================================
    // MULTIPART CORRELATED
    // =====================================================
    @Test
    @DisplayName("given a multipart/form POST to an entity with a single correlated content property")
    void multipartCorrelated()
        throws Exception {

        // assert content does not exist
        String newContent = "This is some new content";

        MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

        // POST the new content
        MockHttpServletResponse response =
            mvc.perform(multipart("/testEntity9s").file(file).param("name", "foo").param("hidden", "bar")).andExpect(status().isCreated()).andReturn()
               .getResponse();

        String location = response.getHeader("Location");

        Optional<TestEntity9> fetchedEntity = repo9.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
        assertThat(fetchedEntity.get().getHidden(), is(nullValue()));

        // assert entity now exists
        mvc.perform(head(location)).andExpect(status().is2xxSuccessful());

        // assert content now exists
        response = mvc.perform(get(location + "/content").accept("text/plain")).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getContentAsString(), is(newContent));
    }

    // =====================================================
    // NO CONTENT FIELD
    // =====================================================
    @Test
    @DisplayName("given a multipart/form POST that doesn't include the content property")
    void noContent()
        throws Exception {

        var testEntity4Id = repo4.save(new TestEntity4()).getId();

        // POST the entity
        MockHttpServletResponse response = mvc.perform(
                                                  multipart("/testEntity3s").param("name", "foo").param("hidden", "bar").param("ying", "yang").param("things", "one", "two")
                                                                            .param("testEntity4", "/testEntity4s/" + testEntity4Id))

                                              .andExpect(status().isCreated()).andReturn().getResponse();

        String location = response.getHeader("Location");

        Optional<TestEntity3> fetchedEntity = repo3.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
        assertThat(fetchedEntity.get().getName(), is("foo"));
        assertThat(fetchedEntity.get().getHidden(), is(nullValue()));
        assertThat(fetchedEntity.get().getYang(), is("yang"));
        assertThat(fetchedEntity.get().getThings(), hasItems("one", "two"));
        assertThat(fetchedEntity.get().getTestEntity4(), is(not(nullValue())));
        assertThat(fetchedEntity.get().getContentId(), is(nullValue()));
        assertThat(fetchedEntity.get().getLen(), is(nullValue()));
        assertThat(fetchedEntity.get().getOriginalFileName(), is(nullValue()));
    }

    // =====================================================
    // EVENTS
    // =====================================================
    @Test
    @DisplayName("given a multipart/form POST and an event listener")
    void events()
        throws Exception {

        eventListener.clear();

        mvc.perform(multipart("/testEntity3s").param("name", "foo foo").param("hidden", "bar bar")).andExpect(status().isCreated());

        assertThat(eventListener.getBeforeCreate().size(), is(1));
        assertThat(eventListener.getAfterCreate().size(), is(1));
        assertThat(((TestEntity3) eventListener.getBeforeCreate().getFirst()).getName(), is("foo foo"));
        assertThat(((TestEntity3) eventListener.getAfterCreate().getFirst()).getName(), is("foo foo"));
    }

    // =====================================================
    // WRONG CONTENT PROPERTY
    // =====================================================
    @Test
    @DisplayName("given a multipart/form POST but with the wrong content property name")
    void wrongProperty() {

        MockMultipartFile file = new MockMultipartFile("oopsDoesntExist", "filename.txt", "text/plain", "foo".getBytes());

        assertThrows(ServletException.class, () -> mvc.perform(multipart("/testEntity3s").file(file).param("name", "foo")));
    }

    // =====================================================
    // MAPPED PROPERTY
    // =====================================================
    @Test
    @DisplayName("given a multipart/form POST to an entity with a mapped content property")
    void mapped()
        throws Exception {
        // assert content does not exist
        String newContent = "This is some new content";

        MockMultipartFile file = new MockMultipartFile("package/content", "filename.txt", "text/plain", newContent.getBytes());

        // POST the new content
        MockHttpServletResponse response = mvc.perform(multipart("/testEntity11s").file(file)).andExpect(status().isCreated()).andReturn().getResponse();

        String location = response.getHeader("Location");

        Optional<TestEntity11> fetchedEntity = repo11.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
        assertThat(fetchedEntity.get().get_package().getContentId(), is(not(nullValue())));

        // assert entity now exists
        mvc.perform(head(location)).andExpect(status().is2xxSuccessful());

        // assert content now exists
        response = mvc.perform(get(location + "/package/content").accept("text/plain")).andExpect(status().isOk()).andReturn().getResponse();
        assertThat(response.getContentAsString(), is(newContent));
    }
}
