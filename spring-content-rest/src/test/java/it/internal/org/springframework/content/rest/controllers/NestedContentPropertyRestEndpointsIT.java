package it.internal.org.springframework.content.rest.controllers;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.WritableResource;
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

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestEntity8;
import internal.org.springframework.content.rest.support.TestEntity8Repository;
import internal.org.springframework.content.rest.support.TestEntity8Store;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
      StoreConfig.class,
      DelegatingWebMvcConfiguration.class,
      RepositoryRestMvcConfiguration.class,
      RestConfiguration.class,
      HypermediaConfiguration.class})
@Transactional
@ActiveProfiles("store")
public class NestedContentPropertyRestEndpointsIT {

   @Autowired private TestEntity8Repository repository2;
   @Autowired private TestEntity8Store store;

   private TestEntity8 testEntity8;

	@Autowired
   private WebApplicationContext context;

   private Version versionTests;
   private LastModifiedDate lastModifiedDateTests;

   @Nested
   @DisplayName("Nested Content Property REST Endpoints")
   class NestedContentPropertyTests {

		private MockMvc mvc;

		@BeforeEach
		void setup() {
		  mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		@Nested
		@DisplayName("given an Entity with a simple content property")
		class WithSimpleContentProperty {

		  @BeforeEach
		  void init() {
			  testEntity8 = repository2.save(new TestEntity8());
		  }

		  @Nested
		  @DisplayName("given a request to a non-existent entity")
		  class NonExistentEntity {

			  @Test
			  @DisplayName("should return 404")
			  void shouldReturn404() throws Exception {
				  mvc.perform(
						  get("/testEntity8s/9999999/foo"))
						  .andExpect(status().isNotFound());
			  }
		  }

		  @Nested
		  @DisplayName("given a POST to the entity endpoint with a multipart/form request")
		  class PostMultiPartForm {

			  @Test
			  @DisplayName("should create a new entity and its content and respond with a 201 Created")
			  void shouldCreateEntityAndContent() throws Exception {
				  String newContent = "This is some new content";

				  MockMultipartFile file = new MockMultipartFile("child", "filename.txt", "text/plain", newContent.getBytes());

				  MockHttpServletResponse response = mvc.perform(multipart("/testEntity8s")
						  .file(file)
						  .param("name", "foo")
						  .param("hidden", "bar"))
						  .andExpect(status().isCreated())
						  .andReturn().getResponse();

				  String location = response.getHeader("Location");

				  Optional<TestEntity8> fetchedEntity = repository2.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
				  assertThat(fetchedEntity.get().getHidden(), is(nullValue()));

				  response = mvc.perform(get(location + "/child")
						  .accept("text/plain"))
						  .andExpect(status().isOk())
						  .andReturn().getResponse();

				  assertThat(response.getContentAsString(), is(newContent));
			  }
		  }

		  @Nested
		  @DisplayName("given a request to a non-existent content property")
		  class NonExistentProperty {

			  @Test
			  @DisplayName("should return 404")
			  void shouldReturn404() throws Exception {
				  mvc.perform(
						  get("/testEntity8s/" + testEntity8.getId() + "/doesnotexist"))
						  .andExpect(status().isNotFound());
			  }
		  }

		  @Nested
		  @DisplayName("given that is has no content")
		  class HasNoContent {

			  @Nested
			  @DisplayName("a GET to /{repository}/{id}/{contentProperty}")
			  class GetContentProperty {

				  @Test
				  @DisplayName("should return 404")
				  void shouldReturn404() throws Exception {
					  mvc.perform(
							  get("/testEntity8s/" + testEntity8.getId() + "/child"))
							  .andExpect(status().isNotFound());
				  }
			  }

			  @Nested
			  @DisplayName("a PUT to /{repository}/{id}/{contentProperty}")
			  class PutContentProperty {

				  @Test
				  @DisplayName("should create the content")
				  void shouldCreateContent() throws Exception {
					  mvc.perform(
							  put("/testEntity8s/" + testEntity8.getId() + "/child")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity8> fetched = repository2.findById(testEntity8.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId,is(not(nullValue())));
					  assertThat(fetched.get().getChild().contentLen, is(31L));
					  assertThat(fetched.get().getChild().contentMimeType, is("text/plain"));
					  try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child")).getInputStream()) {
					      IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
					  }
				  }
			  }

			  @Nested
			  @DisplayName("a PUT to /{store}/{id} with json content")
			  class PutWithJsonContent {

				  @Test
				  @DisplayName("should set the content and return 201")
				  void shouldSetContentAndReturn201() throws Exception {
				      String content = "{\"content\":\"Hello New Spring Content World!\"}";
				      mvc.perform(
                            put("/testEntity8s/" + testEntity8.getId() + "/child")
                            .content(content)
                            .contentType("application/json"))
				      .andExpect(status().isCreated());

				      Optional<TestEntity8> fetched = repository2.findById(testEntity8.getId());
				      assertThat(fetched.isPresent(), is(true));
				      assertThat(fetched.get().getChild().getContentId(), is(not(nullValue())));
				      assertThat(fetched.get().getChild().getContentLen(), is(45L));
				      assertThat(fetched.get().getChild().getContentMimeType(), is("application/json"));
                      try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child")).getInputStream()) {
                          IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
                      }
				  }
			  }
		  }

		  @Nested
		  @DisplayName("given that it has content")
		  class HasContent {

			  @BeforeEach
			  void init() throws Exception {
				  String content = "Hello Spring Content World!";

				  testEntity8.getChild().contentMimeType = "text/plain";
				  UUID contentId = UUID.randomUUID();
				  store.associate(testEntity8, PropertyPath.from("child"), contentId);
				  WritableResource r = (WritableResource)store.getResource(testEntity8, PropertyPath.from("child"));
				  try (OutputStream out = r.getOutputStream()) {
				      out.write(content.getBytes());
				  }
				  testEntity8 = repository2.save(testEntity8);

				  versionTests.setMvc(mvc);
				  versionTests.setUrl("/testEntity8s/" + testEntity8.getId() + "/child");
				  versionTests.setCollectionUrl("/testEntity8s");
				  versionTests.setContentLinkRel("child");
				  versionTests.setRepo(repository2);
				  versionTests.setStore(store);
				  versionTests.setEtag(format("\"%s\"", testEntity8.getVersion()));

				  lastModifiedDateTests.setMvc(mvc);
				  lastModifiedDateTests.setUrl("/testEntity8s/" + testEntity8.getId() + "/child");
				  lastModifiedDateTests.setLastModifiedDate(testEntity8.getModifiedDate());
				  lastModifiedDateTests.setEtag(testEntity8.getVersion().toString());
				  lastModifiedDateTests.setContent(content);
			  }

			  @Nested
			  @DisplayName("a GET to /{repository}/{id}/{contentProperty}")
			  class GetContentProperty {

				  @Test
				  @DisplayName("should return the content")
				  void shouldReturnContent() throws Exception {
					  MockHttpServletResponse response = mvc
							  .perform(get("/testEntity8s/" + testEntity8.getId() + "/child")
									  .accept("text/plain"))
							  .andExpect(status().isOk())
							  .andExpect(header().string("etag", is("\"0\"")))
							  .andExpect(header().string("last-modified", LastModifiedDate
									  .isWithinASecond(testEntity8.getModifiedDate())))
							  .andReturn().getResponse();

					  assertThat(response, is(not(nullValue())));
					  assertThat(response.getContentAsString(), is("Hello Spring Content World!"));
				  }
			  }

			  @Nested
			  @DisplayName("a GET to /{repository}/{id}/{contentProperty} with a mime type that matches a renderer")
			  class GetWithRenderer {

				  @Test
				  @DisplayName("should return the rendition and 200")
				  void shouldReturnRendition() throws Exception {
					  MockHttpServletResponse response = mvc
							  .perform(get(
									  "/testEntity8s/" + testEntity8.getId()
											  + "/child")
									  .accept("text/html"))
							  .andExpect(status().isOk()).andReturn()
							  .getResponse();

					  assertThat(response, is(not(nullValue())));
					  assertThat(response.getContentAsString(), is(
							  "<html><body>Hello Spring Content World!</body></html>"));
				  }
			  }

			  @Nested
			  @DisplayName("a GET to /{repository}/{id}/{contentProperty} with multiple mime types the last of which matches the content")
			  class GetMultipleMimeTypes {

				  @Test
				  @DisplayName("should return the original content and 200")
				  void shouldReturnOriginalContent() throws Exception {
					  MockHttpServletResponse response = mvc
							  .perform(get("/testEntity8s/"
									  + testEntity8.getId()
									  + "/child").accept(
									  new String[] {"text/xml",
											  "text/plain"}))
							  .andExpect(status().isOk()).andReturn()
							  .getResponse();

					  assertThat(response, is(not(nullValue())));
					  assertThat(response.getContentAsString(),
							  is("Hello Spring Content World!"));
				  }
			  }

			  @Nested
			  @DisplayName("a PUT to /{repository}/{id}/{contentProperty}")
			  class PutContentProperty {

				  @Test
				  @DisplayName("should create the content")
				  void shouldCreateContent() throws Exception {
					  mvc.perform(
							  put("/testEntity8s/" + testEntity8.getId() + "/child")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity8> fetched = repository2
							  .findById(testEntity8.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId,is(not(nullValue())));
					  assertThat(fetched.get().getChild().contentLen, is(31L));
					  assertThat(fetched.get().getChild().contentMimeType, is("text/plain"));
				  }
			  }

			  @Nested
			  @DisplayName("a DELETE to /{repository}/{id}/{contentProperty}")
			  class DeleteContentProperty {

				  @Test
				  @DisplayName("should delete the content")
				  void shouldDeleteContent() throws Exception {
					  mvc.perform(delete(
							  "/testEntity8s/" + testEntity8.getId() + "/child"))
							  .andExpect(status().isNoContent());

					  Optional<TestEntity8> fetched = repository2.findById(testEntity8.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId, is(nullValue()));
					  assertThat(fetched.get().getChild().contentLen, is(nullValue()));
                      assertThat(fetched.get().getChild().contentMimeType, is(nullValue()));
				  }
			  }
		  }
		}
	}

	{
		versionTests = Version.tests();
		lastModifiedDateTests = LastModifiedDate.tests();
	}
}
