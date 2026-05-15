package it.internal.org.springframework.content.rest.controllers;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.UUID;

import internal.org.springframework.content.rest.support.StoreConfig;
import internal.org.springframework.content.rest.support.TestStore;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.core.io.Resource;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { StoreConfig.class, DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class, RestConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class StoreRestEndpointsIT {

	@Autowired
	TestStore store;

	@Autowired
	private WebApplicationContext context;

	@Nested
	@DisplayName("Store Rest Endpoints")
	class StoreRestEndpointsTests {

		private MockMvc mvc;

		@BeforeEach
		void setup() {
			mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		@Nested
		@DisplayName("given a root resource path")
		class GivenRootResourcePath {

			private String path;
			private String request;

			@BeforeEach
			void init() {
				path = "/" + UUID.randomUUID() + ".txt";
				request = "/teststore" + path;
			}

			@Nested
			@DisplayName("given a GET request to that path")
			class GetRequest {

				@Test
				@DisplayName("should return 404")
				void shouldReturn404() throws Exception {
					mvc.perform(get(request)).andExpect(status().isNotFound());
				}
			}

			@Nested
			@DisplayName("given a POST to that path with content")
			class PostWithContent {

				@Test
				@DisplayName("should set the content and return 201")
				void shouldSetContentAndReturn201() throws Exception {
					String content = "New multi-part content";

					mvc.perform(multipart(request).file(new MockMultipartFile("file",
							"test-file.txt", "text/plain", content.getBytes())))
							.andExpect(status().isCreated());

					Resource r = store.getResource(path);
					assertThat(IOUtils.contentEquals(
							new ByteArrayInputStream("New multi-part content".getBytes()),
							r.getInputStream()), is(true));
					assertThat(r.contentLength(), equalTo(Long.valueOf(content.length())));
				}
			}

			@Nested
			@DisplayName("given a DELETE request to that path")
			class DeleteRequest {

				@Test
				@DisplayName("should return a 404")
				void shouldReturn404() throws Exception {
					mvc.perform(delete(request)).andExpect(status().isNotFound());
				}
			}
		}

		@Nested
		@DisplayName("given a root resource")
		class GivenRootResource {

			private String path;
			private String request;
			private LastModifiedDate lastModifiedDate;

			@BeforeEach
			void init() throws Exception {
				path = "/" + UUID.randomUUID() + ".txt";
				request = "/teststore" + path;
				Resource r = store.getResource(path);
				if (r instanceof WritableResource) {
					IOUtils.copy(
							new ByteArrayInputStream("Existing content".getBytes()),
							((WritableResource) r).getOutputStream());
				}
				lastModifiedDate = new LastModifiedDate();
				lastModifiedDate.setMvc(mvc);
				lastModifiedDate.setUrl("/teststore" + path);
				lastModifiedDate.setLastModifiedDate(new Date(store.getResource(path).lastModified()));
				lastModifiedDate.setContent("Existing content");
			}

			@Test
			@DisplayName("should return the resource's content")
			void shouldReturnContent() throws Exception {
				MockHttpServletResponse response = mvc.perform(get(request))
						.andExpect(status().isOk()).andReturn().getResponse();

				assertThat(response, is(not(nullValue())));
				assertThat(response.getContentAsString(), is("Existing content"));
			}

			@Test
			@DisplayName("should return a byte range when requested")
			void shouldReturnByteRange() throws Exception {
				MockHttpServletResponse response = mvc
						.perform(get(request).header("range", "bytes=9-12"))
						.andExpect(status().isPartialContent()).andReturn()
						.getResponse();

				assertThat(response, is(not(nullValue())));
				assertThat(response.getContentAsString(), is("cont"));
			}

			@Test
			@DisplayName("should overwrite the resource's content")
			void shouldOverwriteContent() throws Exception {
				mvc.perform(put(request).content("New Existing content")
						.contentType("text/plain")).andExpect(status().isOk());

				Resource r = store.getResource(path);
				assertThat(IOUtils.contentEquals(
						new ByteArrayInputStream("New Existing content".getBytes()),
						r.getInputStream()), is(true));
			}

			@Nested
			@DisplayName("a POST to /{store}/{path} with multi-part form-data")
			class PostMultiPart {

				@Test
				@DisplayName("should overwrite the content and return 200")
				void shouldOverwriteAndReturn200() throws Exception {
					String content = "New multi-part content";

					mvc.perform(multipart(request).file(new MockMultipartFile("file",
							"tests-file.txt", "text/plain", content.getBytes())))
							.andExpect(status().isOk());

					Resource r = store.getResource(path);
					assertThat(IOUtils.contentEquals(
							new ByteArrayInputStream(
									"New multi-part content".getBytes()),
							r.getInputStream()), is(true));
					assertThat(r.contentLength(), equalTo(Long.valueOf(content.length())));
				}
			}

			@Test
			@DisplayName("should delete the resource")
			void shouldDeleteResource() throws Exception {
				mvc.perform(delete(request)).andExpect(status().isNoContent());

				Resource r = store.getResource(path);
				assertThat(r.exists(), is(false));
			}
		}

		@Nested
		@DisplayName("given a nested resource")
		class GivenNestedResource {

			private String path;
			private String request;

			@BeforeEach
			void init() throws Exception {
				path = "/a/b/" + UUID.randomUUID() + ".txt";
				request = "/teststore" + path;
				Resource r = store.getResource(path);
				if (r instanceof WritableResource) {
					IOUtils.copy(
							new ByteArrayInputStream("Existing content".getBytes()),
							((WritableResource) r).getOutputStream());
				}
			}

			@Test
			@DisplayName("should return the resource's content")
			void shouldReturnContent() throws Exception {
				MockHttpServletResponse response = mvc.perform(get(request))
						.andExpect(status().isOk()).andReturn().getResponse();

				assertThat(response, is(not(nullValue())));
				assertThat(response.getContentAsString(), is("Existing content"));
			}

			@Test
			@DisplayName("should return a byte range when requested")
			void shouldReturnByteRange() throws Exception {
				MockHttpServletResponse response = mvc
						.perform(get(request).header("range", "bytes=9-12"))
						.andExpect(status().isPartialContent()).andReturn()
						.getResponse();

				assertThat(response, is(not(nullValue())));
				assertThat(response.getContentAsString(), is("cont"));
			}

			@Nested
			@DisplayName("given a typical browser request")
			class TypicalBrowserRequest {

				@Test
				@DisplayName("should return the resource's content")
				void shouldReturnContent() throws Exception {
					MockHttpServletResponse response = mvc
							.perform(get(request).accept(new String[] { "text/html",
									"application/xhtml+xml", "application/xml;q=0.9",
									"image/webp", "image/apng", "*/*;q=0.8" }))
							.andExpect(status().isOk()).andReturn().getResponse();

					assertThat(response, is(not(nullValue())));
					assertThat(response.getContentAsString(), is("Existing content"));
				}
			}

			@Test
			@DisplayName("should overwrite the resource's content")
			void shouldOverwriteContent() throws Exception {
				mvc.perform(put(request).content("New Existing content")
						.contentType("text/plain")).andExpect(status().isOk());

				Resource r = store.getResource(path);
				assertThat(IOUtils.contentEquals(
						new ByteArrayInputStream("New Existing content".getBytes()),
						r.getInputStream()), is(true));
			}

			@Nested
			@DisplayName("a POST to /{store}/{path} with multi-part form-data")
			class PostMultiPart {

				@Test
				@DisplayName("should overwrite the content and return 200")
				void shouldOverwriteAndReturn200() throws Exception {
					String content = "New multi-part content";

					mvc.perform(multipart(request).file(new MockMultipartFile("file",
							"tests-file.txt", "text/plain", content.getBytes())))
							.andExpect(status().isOk());

					Resource r = store.getResource(path);
					assertThat(IOUtils.contentEquals(
							new ByteArrayInputStream(
									"New multi-part content".getBytes()),
							r.getInputStream()), is(true));
					assertThat(r.contentLength(), equalTo(Long.valueOf(content.length())));
				}
			}

			@Test
			@DisplayName("should delete the resource")
			void shouldDeleteResource() throws Exception {
				mvc.perform(delete(request)).andExpect(status().isNoContent());

				Resource r = store.getResource(path);
				assertThat(r.exists(), is(false));
			}
		}
	}
}
