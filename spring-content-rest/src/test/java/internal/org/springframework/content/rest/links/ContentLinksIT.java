package internal.org.springframework.content.rest.links;

import static java.lang.String.format;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import internal.org.springframework.content.rest.support.BaseUriConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity10;
import internal.org.springframework.content.rest.support.TestEntity10Repository;
import internal.org.springframework.content.rest.support.TestEntity10Store;
import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntity2Store;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntity5;
import internal.org.springframework.content.rest.support.TestEntity5Repository;
import internal.org.springframework.content.rest.support.TestEntity5Store;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;

@WebAppConfiguration
@ContextConfiguration(classes = {
		BaseUriConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class,
		HypermediaConfiguration.class })
@Transactional
@ActiveProfiles("store")
public class ContentLinksIT {

	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;

    @Autowired
    TestEntity5Repository repository5;
    @Autowired
    TestEntity5Store store5;

    @Autowired
    TestEntity2Repository repository2;
    @Autowired
    TestEntity2Store store2;

	@Autowired
	TestEntity10Repository repository10;
	@Autowired
	TestEntity10Store store10;

    @Autowired
    TestEntity3Repository repository3;
    @Autowired
    TestEntity3ContentRepository contentRepository3;

	@Autowired
	private WebApplicationContext context;

	@Nested
	@DisplayName("no linkrel")
	class NoLinkRelTests {

		private MockMvc mvc;
		private TestEntity3 testEntity3;
		private TestEntity5 testEntity5;
		private TestEntity2 testEntity2;
		private TestEntity10 testEntity10;
		private ContentLinkTests contentLinkTests;

		@BeforeEach
		void setup() {
			mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		@Nested
		@DisplayName("given a store and an entity with a top-level uncorrelated content property")
		class TopLevelUncorrelated {

			@BeforeEach
			void init() {
				testEntity3 = new TestEntity3();
				contentRepository3.setContent(testEntity3, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				testEntity3 = repository3.save(testEntity3);

				contentLinkTests = new ContentLinkTests();
				contentLinkTests.setMvc(mvc);
				contentLinkTests.setRepository(repository3);
				contentLinkTests.setStore(contentRepository3);
				contentLinkTests.setTestEntity(testEntity3);
				contentLinkTests.setUrl("/api/testEntity3s/" + testEntity3.getId());
				contentLinkTests.setLinkRel("testEntity3");
				contentLinkTests.setExpectedLinkRegex("http://localhost/api/testEntity3s/" + testEntity3.getId());
			}
		}

		@Nested
		@DisplayName("given a store and an entity with top-level correlated content properties")
		class TopLevelCorrelated {

			@BeforeEach
			void init() {
				testEntity5 = new TestEntity5();
				store5.setContent(testEntity5, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				testEntity5 = repository5.save(testEntity5);

				contentLinkTests = new ContentLinkTests();
				contentLinkTests.setMvc(mvc);
				contentLinkTests.setRepository(repository5);
				contentLinkTests.setStore(store5);
				contentLinkTests.setTestEntity(testEntity5);
				contentLinkTests.setUrl("/api/testEntity5s/" + testEntity5.getId());
				contentLinkTests.setLinkRel("content");
				contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/testEntity5s/%s/content", testEntity5.getId()));
			}
		}

		@Nested
		@DisplayName("given a store specifying a linkrel and an entity a nested content property")
		class NestedContentProperty {

			@BeforeEach
			void init() {
				testEntity2 = new TestEntity2();
				testEntity2.getChild().setMimeType("text/plain");
				store2.setContent(testEntity2, PropertyPath.from("child"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				testEntity2 = repository2.save(testEntity2);

				contentLinkTests = new ContentLinkTests();
				contentLinkTests.setMvc(mvc);
				contentLinkTests.setRepository(repository2);
				contentLinkTests.setStore(store2);
				contentLinkTests.setTestEntity(testEntity2);
				contentLinkTests.setUrl("/api/files/" + testEntity2.getId());
				contentLinkTests.setLinkRel("child");
				contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/files/%s/child", testEntity2.getId()));
			}
		}

		@Nested
		@DisplayName("given a store specifying a linkrel and an entity with nested content properties")
		class NestedContentProperties {

			@BeforeEach
			void init() {
				testEntity10 = new TestEntity10();
				store10.setContent(testEntity10, PropertyPath.from("child/content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				testEntity10.getChild().setContentMimeType("text/plain");
				testEntity10.getChild().setContentFileName("test");
				store10.setContent(testEntity10, PropertyPath.from("child/preview"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				testEntity10.getChild().setPreviewMimeType("text/plain");
				testEntity10 = repository10.save(testEntity10);

				contentLinkTests = new ContentLinkTests();
				contentLinkTests.setMvc(mvc);
				contentLinkTests.setRepository(repository10);
				contentLinkTests.setStore(store10);
				contentLinkTests.setTestEntity(testEntity10);
				contentLinkTests.setUrl("/api/testEntity10s/" + testEntity10.getId());
				contentLinkTests.setLinkRel("child/content");
				contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/testEntity10s/%s/child/content", testEntity10.getId()));
			}
		}
	}
}
