package it.internal.org.springframework.content.rest.controllers;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import internal.org.springframework.content.rest.support.EventListenerConfig.TestEventListener;
import jakarta.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import internal.org.springframework.content.rest.support.*;
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
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
		StoreConfig.class,
		EntityConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class,
		EventListenerConfig.class
})
@Transactional
@ActiveProfiles("store")
public class ContentEntityRestEndpointsIT {

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
	TestEntity6Store store6;

	@Autowired
	TestEntity9Repository repo9;
	@Autowired
	TestEntity9Store store9;

	@Autowired
	TestEntity11Repository repo11;
	@Autowired
	TestEntity11Store store11;

	@Autowired
	TestStore store;

	@Autowired
	TestEventListener eventListener;

	@Autowired
	private WebApplicationContext context;

	@Nested
	@DisplayName("Content Entity REST Endpoints")
	class ContentEntityRestEndpointsTests {

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

		@Nested
		@DisplayName("given an entity with a single uncorrelated content properties")
		class SingleUncorrelatedContentProperties {

			@Nested
			@DisplayName("given the repository and storage are exported to the same URI")
			class SameUri {

				@BeforeEach
				void init() {
					testEntity3 = repo3.save(new TestEntity3());
					testEntity3.name = "tests";
					testEntity3 = repo3.save(testEntity3);

					entityTests = new Entity();
					entityTests.setMvc(mvc);
					entityTests.setUrl("/testEntity3s/" + testEntity3.id);
					entityTests.setEntity(testEntity3);
					entityTests.setRepository(repo3);
					entityTests.setLinkRel("testEntity3");

					contentTests = new Content();
					contentTests.setMvc(mvc);
					contentTests.setUrl("/testEntity3s/" + testEntity3.getId());
					contentTests.setEntity(testEntity3);
					contentTests.setRepository(repo3);
					contentTests.setStore(store3);
				}

				@Nested
				@DisplayName("a DELETE to /{store}/{id}/softDelete (custom handler)")
				class SoftDelete {

					@Test
					@DisplayName("should return 200")
					void shouldReturn200() throws Exception {
						mvc.perform(delete("/testEntity3s/" + testEntity3.id + "/softDelete"))
								.andExpect(status().is2xxSuccessful());
					}
				}
			}

			@Nested
			@DisplayName("given the repository and storage are exported to different URIs")
			class DifferentUris {

				@BeforeEach
				void init() {
					testEntity = repository.save(new TestEntity());

					contentTests = new Content();
					contentTests.setMvc(mvc);
					contentTests.setUrl("/testEntitiesContent/" + testEntity.getId());
					contentTests.setEntity(testEntity);
					contentTests.setRepository(repository);
					contentTests.setStore(contentRepository);

					corsTests = new Cors();
					corsTests.setMvc(mvc);
					corsTests.setUrl("/testEntitiesContent/" + testEntity.getId());
				}

				@Test
				@DisplayName("an OPTIONS request to the repository from a known host should return the relevant CORS headers and OK")
				void optionsRequestFromKnownHostShouldReturnCorsHeaders() throws Exception {
					mvc.perform(options("/testEntities/" + testEntity.getId())
							.header("Access-Control-Request-Method", "PUT")
							.header("Origin", "http://www.someurl.com"))
							.andExpect(status().isOk())
							.andExpect(header().string("Access-Control-Allow-Origin","http://www.someurl.com"));
				}
			}

			@Nested
			@DisplayName("given an entity with @Version")
			class WithVersion {

				@BeforeEach
				void init() {
					testEntity4 = new TestEntity4();
					testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
					testEntity4.mimeType = "text/plain";
					testEntity4 = repo4.save(testEntity4);
					String url = "/testEntity4s/" + testEntity4.getId();

					version = new Version();
					version.setMvc(mvc);
					version.setUrl(url);
					version.setCollectionUrl("/testEntity4s");
					version.setContentLinkRel("content");
					version.setRepo(repo4);
					version.setStore(store4);
					version.setEtag(format("\"%s\"", testEntity4.getVersion()));
					version.setEntity(testEntity4);
				}
			}

			@Nested
			@DisplayName("given an entity with @LastModifiedDate")
			class WithLastModifiedDate {

				@BeforeEach
				void init() {
					String content = "Hello Spring Content LastModifiedDate World!";

					testEntity4 = new TestEntity4();
					testEntity4 = store4.setContent(testEntity4, new ByteArrayInputStream(content.getBytes()));
					testEntity4.mimeType = "text/plain";
					testEntity4 = repo4.save(testEntity4);
					String url = "/testEntity4s/" + testEntity4.getId();

					lastModifiedDate = new LastModifiedDate();
					lastModifiedDate.setMvc(mvc);
					lastModifiedDate.setUrl(url);
					lastModifiedDate.setLastModifiedDate(testEntity4.getModifiedDate());
					lastModifiedDate.setEtag(testEntity4.getVersion().toString());
					lastModifiedDate.setContent(content);
				}
			}

			@Nested
			@DisplayName("given an entity with a shared Id and ContentId field")
			class SharedIdAndContentId {

				@BeforeEach
				void init() {
					testEntity6 = new TestEntity6();
					testEntity6 = repo6.save(testEntity6);
				}

				@Test
				@DisplayName("should return 404 when no content is set")
				void shouldReturn404WhenNoContentSet() throws Exception {
					mvc.perform(get("/testEntity6s/" + testEntity6.getId())
								.accept("text/plain"))
							.andExpect(status().isNotFound());
				}
			}
		}

		@Nested
		@DisplayName("given a multipart/form POST to an entity with a single uncorrelated content property")
		class MultipartPostToUncorrelatedContentProperty {

			@Test
			@DisplayName("should create a new entity and its content and respond with a 201 Created")
			void shouldCreateEntityAndContent() throws Exception {
				String newContent = "This is some new content";

				MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

				var testEntity4Id = repo4.save(new TestEntity4()).getId();

				MockHttpServletResponse response = mvc.perform(multipart("/testEntity3s")
								.file(file)
								.contentType("multipart/form-data; boundary=c0de8278")
								.param("name", "foo")
								.param("hidden", "bar")
								.param("ying", "yang")
								.param("things", "one", "two")
								.param("testEntity4", "/testEntity4s/" + testEntity4Id))

						.andExpect(status().isCreated())
						.andReturn().getResponse();

				String location = response.getHeader("Location");

				Optional<TestEntity3> fetchedEntity = repo3.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
				assertThat(fetchedEntity.get().getName(), is("foo"));
				assertThat(fetchedEntity.get().getHidden(), is(nullValue()));
				assertThat(fetchedEntity.get().getYang(), is("yang"));
				assertThat(fetchedEntity.get().getThings(), hasItems("one", "two"));
				assertThat(fetchedEntity.get().getTestEntity4(), is(not(nullValue())));
				assertThat(fetchedEntity.get().getLen(), is(file.getSize()));
				assertThat(fetchedEntity.get().getOriginalFileName(), is(file.getOriginalFilename()));

				response = mvc.perform(get(location)
								.accept("text/plain"))
						.andExpect(status().isOk())
						.andReturn().getResponse();

				assertThat(response.getContentAsString(), is(newContent));
			}
		}

		@Nested
		@DisplayName("given a multipart/form POST to an entity with a non-default initialized @Version property (#Issue 2044)")
		class MultipartPostToEntityWithNonDefaultVersion {

			@Nested
			@DisplayName("with content")
			class WithContent {

				@Test
				@DisplayName("should create a new entity and its content and respond with a 201 Created")
				void shouldCreateEntityAndContent() throws Exception {
					String newContent = "This is some new content";

					MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain",
							newContent.getBytes());

					MockHttpServletResponse response = mvc.perform(multipart("/testEntity4s")
									.file(file)
									.param("name", "foo")
									.param("title", "bar"))

							.andExpect(status().isCreated())
							.andReturn().getResponse();

					String location = response.getHeader("Location");

					Optional<TestEntity4> fetchedEntity = repo4.findById(
							Long.valueOf(StringUtils.substringAfterLast(location, "/")));
					assertThat(fetchedEntity.get().getName(), is("foo"));
					assertThat(fetchedEntity.get().getTitle(), is("bar"));
					assertThat(fetchedEntity.get().getContentId(), is(not(nullValue())));
					assertThat(fetchedEntity.get().getLen(), is(file.getSize()));
					assertThat(fetchedEntity.get().getOriginalFileName(), is(file.getOriginalFilename()));

					response = mvc.perform(get(location)
									.accept("text/plain"))
							.andExpect(status().isOk())
							.andReturn().getResponse();

					assertThat(response.getContentAsString(), is(newContent));
				}
			}

			@Nested
			@DisplayName("without content")
			class WithoutContent {

				@Test
				@DisplayName("should create a new entity and respond with a 201 Created")
				void shouldCreateEntity() throws Exception {
					MockHttpServletResponse response = mvc.perform(multipart("/testEntity4s")
									.param("name", "foo")
									.param("title", "bar"))

							.andExpect(status().isCreated())
							.andReturn().getResponse();

					String location = response.getHeader("Location");

					Optional<TestEntity4> fetchedEntity = repo4.findById(
							Long.valueOf(StringUtils.substringAfterLast(location, "/")));
					assertThat(fetchedEntity.get().getName(), is("foo"));
					assertThat(fetchedEntity.get().getTitle(), is("bar"));
					assertThat(fetchedEntity.get().getContentId(), is(nullValue()));
					assertThat(fetchedEntity.get().getLen(), is(nullValue()));
					assertThat(fetchedEntity.get().getOriginalFileName(), is(nullValue()));
				}
			}
		}

		@Nested
		@DisplayName("given an entity with a single correlated content property")
		class SingleCorrelatedContentProperty {

			@BeforeEach
			void init() {
				testEntity9 = repo9.save(new TestEntity9());
			}

			@Test
			@DisplayName("should support content operations")
			void shouldSupportContentOperations() throws Exception {
				String content = "Hello Spring Content World!";
				mvc.perform(
						put("/testEntity9s/" + testEntity9.id)
						.contextPath("")
						.content(content)
						.contentType("text/plain"))
				.andExpect(status().isCreated());

				Optional<TestEntity9> fetched = repo9.findById(testEntity9.getId());
				assertThat(fetched.isPresent(), is(true));
				assertThat(fetched.get().getContentId(), is(not(nullValue())));
				assertThat(fetched.get().getContentLen(), is(27L));
				assertThat(fetched.get().getContentMimeType(), is("text/plain"));
				assertThat(IOUtils.toString(store9.getContent(fetched.get()), Charset.defaultCharset()), is(content));

				MockHttpServletResponse response = mvc
						.perform(get("/testEntity9s/" + testEntity9.id)
								.contextPath("")
								.accept("text/plain"))
						.andExpect(status().isOk()).andReturn().getResponse();

				assertThat(response, is(not(nullValue())));
				assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
			}
		}

		@Nested
		@DisplayName("given a multipart/form POST to an entity with a single correlated content property")
		class MultipartPostToCorrelatedContentProperty {

			@Test
			@DisplayName("should create a new entity and its content and respond with a 201 Created")
			void shouldCreateEntityAndContent() throws Exception {
				String newContent = "This is some new content";

				MockMultipartFile file = new MockMultipartFile("content", "filename.txt", "text/plain", newContent.getBytes());

				MockHttpServletResponse response = mvc.perform(multipart("/testEntity9s")
								.file(file)
								.param("name", "foo")
								.param("hidden", "bar"))
						.andExpect(status().isCreated())
						.andReturn().getResponse();

				String location = response.getHeader("Location");

				Optional<TestEntity9> fetchedEntity = repo9.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
				assertThat(fetchedEntity.get().getHidden(), is(nullValue()));

				mvc.perform(head(location))
						.andExpect(status().is2xxSuccessful());

				response = mvc.perform(get(location + "/content")
								.accept("text/plain"))
						.andExpect(status().isOk())
						.andReturn().getResponse();
				assertThat(response.getContentAsString(), is(newContent));
			}
		}

		@Nested
		@DisplayName("given a multipart/form POST that doesn't include the content property")
		class MultipartPostWithoutContentProperty {

			@Test
			@DisplayName("should create a new entity with no content and respond with a 201 Created")
			void shouldCreateEntityWithoutContent() throws Exception {
				var testEntity4Id = repo4.save(new TestEntity4()).getId();

				MockHttpServletResponse response = mvc.perform(multipart("/testEntity3s")
								.param("name", "foo")
								.param("hidden", "bar")
								.param("ying", "yang")
								.param("things", "one", "two")
								.param("testEntity4", "/testEntity4s/" + testEntity4Id))

						.andExpect(status().isCreated())
						.andReturn().getResponse();

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
		}

		@Nested
		@DisplayName("given a multipart/form POST and an event listener")
		class MultipartPostWithEventListener {

			@BeforeEach
			void clearEvents() {
				eventListener.clear();
			}

			@Test
			@DisplayName("should create a new entity and fire the onBeforeCreate/onAfterCreate events")
			void shouldFireEvents() throws Exception {
				MockHttpServletResponse response = mvc.perform(multipart("/testEntity3s")
								.param("name", "foo foo")
								.param("hidden", "bar bar")
								.param("ying", "yang")
								.param("things", "one", "two"))

						.andExpect(status().isCreated())
						.andReturn().getResponse();

				assertThat(eventListener.getBeforeCreate().size(), is(1));
				assertThat(eventListener.getAfterCreate().size(), is(1));
				assertThat(((TestEntity3) eventListener.getBeforeCreate().get(0)).getName(), is("foo foo"));
				assertThat(((TestEntity3) eventListener.getAfterCreate().get(0)).getName(), is("foo foo"));
			}
		}

		@Nested
		@DisplayName("given a multipart/form POST but with the wrong content property name")
		class MultipartPostWrongContentProperty {

			@Test
			@DisplayName("should return an error and not make the entity")
			void shouldReturnError() throws Exception {
				MockMultipartFile file = new MockMultipartFile("oopsDoesntExist", "filename.txt", "text/plain",
						"foo".getBytes());

				var before = repo3.count();

				assertThrows(ServletException.class, () ->
						mvc.perform(multipart("/testEntity3s")
										.file(file)
										.param("name", "foo foo")
										.param("hidden", "bar bar")
										.param("ying", "yang")
										.param("things", "one", "two"))

								.andExpect(status().isCreated())
								.andReturn().getResponse()
				);

				assertThat(repo3.count(), is(before + 1L));
			}
		}

		@Nested
		@DisplayName("given a multipart/form POST to an entity with a mapped content property")
		class MultipartPostToMappedContentProperty {

			@Test
			@DisplayName("should create a new entity and its content and respond with a 201 Created")
			void shouldCreateEntityAndContent() throws Exception {
				String newContent = "This is some new content";

				MockMultipartFile file = new MockMultipartFile("package/content", "filename.txt", "text/plain", newContent.getBytes());

				MockHttpServletResponse response = mvc.perform(multipart("/testEntity11s")
								.file(file))
						.andExpect(status().isCreated())
						.andReturn().getResponse();

				String location = response.getHeader("Location");

				Optional<TestEntity11> fetchedEntity = repo11.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
				assertThat(fetchedEntity.get().get_package().getContentId(), is(not(nullValue())));

				mvc.perform(head(location))
						.andExpect(status().is2xxSuccessful());

				response = mvc.perform(get(location + "/package/content")
								.accept("text/plain"))
						.andExpect(status().isOk())
						.andReturn().getResponse();
				assertThat(response.getContentAsString(), is(newContent));
			}
		}
	}
}
