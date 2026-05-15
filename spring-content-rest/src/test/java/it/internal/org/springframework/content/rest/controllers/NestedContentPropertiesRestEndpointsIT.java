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

import internal.org.springframework.content.rest.support.*;
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

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
      StoreConfig.class,
      DelegatingWebMvcConfiguration.class,
      RepositoryRestMvcConfiguration.class,
      RestConfiguration.class,
      HypermediaConfiguration.class
})
@Transactional
@ActiveProfiles("store")
public class NestedContentPropertiesRestEndpointsIT {

   @Autowired private TestEntity10Repository repository;
   @Autowired private TestEntity10Store store;

   private TestEntity10 testEntity10;

	@Autowired
   private WebApplicationContext context;

   private Version versionTests;
   private LastModifiedDate lastModifiedDateTests;

   @Nested
   @DisplayName("Nested Content Properties REST Endpoints")
   class NestedContentPropertiesTests {

		private MockMvc mvc;

		@BeforeEach
		void setup() {
		  mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		@Nested
		@DisplayName("given an Entity with a simple content property")
		class WithContentProperty {

		  @BeforeEach
		  void init() {
			  testEntity10 = repository.save(new TestEntity10());
		  }

		  @Nested
		  @DisplayName("given a request to a non-existent entity")
		  class NonExistentEntity {

			  @Test
			  @DisplayName("should return 404")
			  void shouldReturn404() throws Exception {
				  mvc.perform(
						  get("/testEntity10s/9999999/foo"))
						  .andExpect(status().isNotFound());
			  }
		  }

		  @Nested
		  @DisplayName("given a request to a non-existent content property")
		  class NonExistentProperty {

			  @Test
			  @DisplayName("should return 404")
			  void shouldReturn404() throws Exception {
				  mvc.perform(
						  get("/testEntity10s/" + testEntity10.getId() + "/doesnotexist"))
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
							  get("/testEntity10s/" + testEntity10.getId() + "/child"))
							  .andExpect(status().isNotFound());
				  }
			  }

			  @Nested
			  @DisplayName("a PUT to /{repository}/{id}/{property}/{contentProperty}")
			  class PutContentProperty {

				  @Test
				  @DisplayName("should create the content")
				  void shouldCreateContent() throws Exception {
					  mvc.perform(
							  put("/testEntity10s/" + testEntity10.getId() + "/child/content")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity10> fetched = repository.findById(testEntity10.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId,is(not(nullValue())));
					  assertThat(fetched.get().getChild().contentLen, is(31L));
					  assertThat(fetched.get().getChild().contentMimeType, is("text/plain"));
					  try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/content")).getInputStream()) {
					      IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content World!".getBytes()));
					  }

                      mvc.perform(
                                put("/testEntity10s/" + testEntity10.getId() + "/child/preview")
                                        .content("Hello New Spring Content Preview World!")
                                        .contentType("text/plain"))
                                .andExpect(status().is2xxSuccessful());

                      fetched = repository.findById(testEntity10.getId());
                      assertThat(fetched.isPresent(), is(true));
                      assertThat(fetched.get().getChild().getPreviewId(),is(not(nullValue())));
                      assertThat(fetched.get().getChild().getPreviewLen(), is(39L));
                      assertThat(fetched.get().getChild().getPreviewMimeType(), is("text/plain"));
                      try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/preview")).getInputStream()) {
                          IOUtils.contentEquals(actual, new ByteArrayInputStream("Hello New Spring Content Preview World!".getBytes()));
                      }
				  }
			  }

			  @Nested
			  @DisplayName("a PUT to /{store}/{id}/{property}/{contentProperty} with json content")
			  class PutWithJsonContent {

				  @Test
				  @DisplayName("should set the content and return 201")
				  void shouldSetContentAndReturn201() throws Exception {
				      String content = "{\"content\":\"Hello New Spring Content World!\"}";
				      mvc.perform(
                            put("/testEntity10s/" + testEntity10.getId() + "/child/content")
                            .content(content)
                            .contentType("application/json"))
				      .andExpect(status().isCreated());

				      Optional<TestEntity10> fetched = repository.findById(testEntity10.getId());
				      assertThat(fetched.isPresent(), is(true));
				      assertThat(fetched.get().getChild().getContentId(), is(not(nullValue())));
				      assertThat(fetched.get().getChild().getContentLen(), is(45L));
				      assertThat(fetched.get().getChild().getContentMimeType(), is("application/json"));
                      try (InputStream actual = store.getResource(fetched.get(), PropertyPath.from("child/content")).getInputStream()) {
                          IOUtils.contentEquals(actual, new ByteArrayInputStream(content.getBytes()));
					  }
				  }
			  }

		  @Nested
		  @DisplayName("given that it has content")
		  class HasContent {

			  @BeforeEach
			  void init() throws Exception {
				  String content = "Hello Spring Content World!";

				  testEntity10.getChild().contentMimeType = "text/plain";
				  UUID contentId = UUID.randomUUID();
				  store.associate(testEntity10, PropertyPath.from("child/content"), contentId);
				  WritableResource r = (WritableResource)store.getResource(testEntity10, PropertyPath.from("child/content"));
				  try (OutputStream out = r.getOutputStream()) {
				      out.write(content.getBytes());
				  }
				  testEntity10 = repository.save(testEntity10);

				  versionTests.setMvc(mvc);
				  versionTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
				  versionTests.setCollectionUrl("/testEntity10s");
				  versionTests.setContentLinkRel("child/content");
				  versionTests.setRepo(repository);
				  versionTests.setStore(store);
				  versionTests.setEtag(format("\"%s\"", testEntity10.getVersion()));

				  lastModifiedDateTests.setMvc(mvc);
				  lastModifiedDateTests.setUrl("/testEntity10s/" + testEntity10.getId() + "/child/content");
				  lastModifiedDateTests.setLastModifiedDate(testEntity10.getModifiedDate());
				  lastModifiedDateTests.setEtag(testEntity10.getVersion().toString());
				  lastModifiedDateTests.setContent(content);
			  }

			  @Nested
			  @DisplayName("a GET to /{repository}/{id}/{contentProperty}")
			  class GetContentProperty {

				  @Test
				  @DisplayName("should return the content")
				  void shouldReturnContent() throws Exception {
					  MockHttpServletResponse response = mvc
							  .perform(get("/testEntity10s/" + testEntity10.getId() + "/child/content")
									  .accept("text/plain"))
							  .andExpect(status().isOk())
							  .andExpect(header().string("etag", is("\"0\"")))
							  .andExpect(header().string("last-modified", LastModifiedDate
									  .isWithinASecond(testEntity10.getModifiedDate())))
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
									  "/testEntity10s/" + testEntity10.getId()
											  + "/child/content")
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
							  .perform(get("/testEntity10s/"
									  + testEntity10.getId()
									  + "/child/content").accept(
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
							  put("/testEntity10s/" + testEntity10.getId() + "/child/content")
									  .content("Hello New Spring Content World!")
									  .contentType("text/plain"))
							  .andExpect(status().is2xxSuccessful());

					  Optional<TestEntity10> fetched = repository
							  .findById(testEntity10.getId());
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
							  "/testEntity10s/" + testEntity10.getId() + "/child/content"))
							  .andExpect(status().isNoContent());

					  Optional<TestEntity10> fetched = repository.findById(testEntity10.getId());
					  assertThat(fetched.isPresent(), is(true));
					  assertThat(fetched.get().getChild().contentId, is(nullValue()));
					  assertThat(fetched.get().getChild().contentLen, is(nullValue()));
                      assertThat(fetched.get().getChild().contentMimeType, is(nullValue()));
				  }
			  }
}


		@Nested
		@DisplayName("given a POST to the entity endpoint with a multipart/form request")
		class PostMultiPartForm {

		  @Test
		  @DisplayName("should create a new entity and its content and respond with a 201 Created")
		  void shouldCreateEntityAndContent() throws Exception {
			  String content = "This is some new content";
			  String previewContent = "This is some new preview content";

			  MockMultipartFile file1 = new MockMultipartFile("child/content", "filename.txt", "text/plain", content.getBytes());
			  MockMultipartFile file2 = new MockMultipartFile("child/preview", "preview.txt", "text/plain", previewContent.getBytes());

			  MockHttpServletResponse response = mvc.perform(multipart("/testEntity10s")
							  .file(file1)
							  .file(file2)
							  .param("name", "foo")
							  .param("hidden", "bar"))
					  .andExpect(status().isCreated())
					  .andReturn().getResponse();

			  String location = response.getHeader("Location");

			  Optional<TestEntity10> fetchedEntity = repository.findById(Long.valueOf(StringUtils.substringAfterLast(location, "/")));
			  assertThat(fetchedEntity.get().getHidden(), is(nullValue()));

			  response = mvc.perform(get(location + "/child/content")
							  .accept("text/plain"))
					  .andExpect(status().isOk())
					  .andReturn().getResponse();

			  assertThat(response.getContentAsString(), is(content));

			  response = mvc.perform(get(location + "/child/preview")
							  .accept("text/plain"))
					  .andExpect(status().isOk())
					  .andReturn().getResponse();

			  assertThat(response.getContentAsString(), is(previewContent));
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
