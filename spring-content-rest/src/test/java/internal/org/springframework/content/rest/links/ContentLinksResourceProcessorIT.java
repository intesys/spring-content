package internal.org.springframework.content.rest.links;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;

import java.util.UUID;

import internal.org.springframework.content.rest.support.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.rest.RestResource;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.repository.support.Repositories;
import org.springframework.data.rest.webmvc.PersistentEntityResource;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import jakarta.persistence.*;

@WebAppConfiguration
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
		BaseUriConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentLinksResourceProcessorIT {

	@Autowired
	private Repositories repositories;

	@Autowired
	private PersistentEntities persistentEntities;

	@Autowired
	private ContentLinksResourceProcessor processor;

	@Autowired
	private WebApplicationContext context;

	@Nested
	@DisplayName("given the spring content baseUri property is set to contentApi")
	class GivenBaseUriSet {

		private MockMvc mvc;
		private PersistentEntityResource resource;

		@BeforeEach
		void setup() {
			mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		void processResource() {
			MockHttpServletRequest request = new MockHttpServletRequest();
			RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
			processor.process(resource);
		}

		@Nested
		@DisplayName("given an entity with a single @ContentId property")
		class SingleContentId {

			@BeforeEach
			void init() {
				PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity4.class);

				TestEntity4 obj = new TestEntity4();
				obj.setId(999L);
				obj.setContentId(UUID.randomUUID());

				PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
				resource = build.build();
				processResource();
			}

			@Test
			@DisplayName("should add an entity content links")
			void shouldAddContentLinks() {
				assertThat(resource.getLinks("content"), hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity4s/999/content"))));
			}

			@Nested
			@DisplayName("when fully qualified links are disabled and shortcut links are enabled")
			class ShortcutLinksEnabled {

				@BeforeEach
				void disableFullyQualifiedAndEnableShortcut() {
					processor.getRestConfiguration().setFullyQualifiedLinks(false);
					processor.getRestConfiguration().setShortcutLinks(true);
					processResource();
				}

				@AfterEach
				void restoreDefaults() {
					processor.getRestConfiguration().setFullyQualifiedLinks(true);
				}

				@Test
				@DisplayName("should add original and shortcut links")
				void shouldAddOriginalAndShortcutLinks() {
					assertThat(resource.getLinks("testEntity4s"), hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity4s/999"))));
					assertThat(resource.getLinks("testEntity4"), hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity4s/999"))));
				}
			}

			@Nested
			@DisplayName("when fully qualified links are disabled and shortcut links are disabled")
			class ShortcutLinksDisabled {

				@BeforeEach
				void disableLinks() {
					processor.getRestConfiguration().setFullyQualifiedLinks(false);
					processor.getRestConfiguration().setShortcutLinks(false);
					processResource();
				}

				@AfterEach
				void restoreDefaults() {
					processor.getRestConfiguration().setFullyQualifiedLinks(true);
					processor.getRestConfiguration().setShortcutLinks(true);
				}

				@Test
				@DisplayName("should add original and shortcut links")
				void shouldNotAddShortcutLinks() {
					assertThat(resource.getLinks("testEntity4s"), not(hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity4s/999")))));
					assertThat(resource.getLinks("testEntity4"), not(hasItem(hasProperty("href", is("http://localhost/contentApi/testEntity4s/999")))));
				}
			}
		}

		@Nested
		@DisplayName("given an entity with multiple @ContentId properties")
		class MultipleContentIds {

			@BeforeEach
			void init() {
				PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity5.class);

				TestEntity5 obj = new TestEntity5();
				obj.setId(999L);
				UUID contentId = UUID.randomUUID();
				obj.setContentId(contentId);
				obj.setRenditionId(UUID.randomUUID());

				PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
				resource = build.build();
				processResource();
			}

			@Test
			@DisplayName("should add content property links")
			void shouldAddContentPropertyLinks() {
				assertThat(resource.getLinks("content"), hasItems(hasProperty("href", is("http://localhost/contentApi/testEntity5s/999/content"))));
				assertThat(resource.getLinks("rendition"), hasItems(hasProperty("href", is("http://localhost/contentApi/testEntity5s/999/rendition"))));
			}
		}

		@Nested
		@DisplayName("given an entity with an embedded object containing @ContentId properties")
		class EmbeddedContentIds {

			@BeforeEach
			void init() {
				PersistentEntity<?, ?> persistentEntity = repositories.getPersistentEntity(TestEntity2.class);

				TestEntity2 obj = new TestEntity2();
				obj.setId(999L);
				UUID contentId = UUID.randomUUID();
				TestEntityChild child = new TestEntityChild();
				child.setContentId(contentId);
				obj.setChild(child);

				PersistentEntityResource.Builder build = PersistentEntityResource.build(obj, persistentEntity);
				resource = build.build();
				processResource();
			}

			@Test
			@DisplayName("should add content property links")
			void shouldAddContentPropertyLinks() {
				assertThat(resource.getLinks("child"), hasItems(hasProperty("href", is("http://localhost/contentApi/files/999/child"))));
			}
		}

		@Nested
		@DisplayName("given an entity with embedded object with @RestResource customizations [Issue #1049]")
		class EmbeddedWithRestResourceCustomizations {

			@BeforeEach
			void init() {
				PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(TestEntity11.class);

				TestEntity11 testEntity11 = new TestEntity11();
				testEntity11.setId(999L);

				PersistentEntityResource.Builder build = PersistentEntityResource.build(testEntity11, persistentEntity);
				resource = build.buildNested();
				processResource();
			}

			@Test
			@DisplayName("should add content property links")
			void shouldAddContentPropertyLinks() {
				assertThat(resource.getLinks("package/content"), hasItems(hasProperty("href", is("http://localhost/contentApi/testEntity11s/999/package/content"))));
				assertThat(resource.getLinks("package/preview"), hasItems(hasProperty("href", is("http://localhost/contentApi/testEntity11s/999/package/preview"))));
			}
		}

		@Nested
		@DisplayName("given the embedded object in an entity containing @ContentId properties")
		class EmbeddedObjectContentIds {

			@BeforeEach
			void init() {
				PersistentEntity<?, ?> persistentEntity = persistentEntities.getRequiredPersistentEntity(TestEntityChild.class);

				UUID contentId = UUID.randomUUID();
				TestEntityChild child = new TestEntityChild();
				child.setContentId(contentId);

				PersistentEntityResource.Builder build = PersistentEntityResource.build(child, persistentEntity);
				resource = build.buildNested();
				processResource();
			}

			@Test
			@DisplayName("should not try to generate content property links for the embedded object")
			void shouldNotGenerateLinksForEmbedded() {
				assertThat(resource.getLinks().isEmpty(), is(true));
			}
		}
	}
}
