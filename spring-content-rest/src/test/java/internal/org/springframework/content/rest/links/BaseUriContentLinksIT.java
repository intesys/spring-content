package internal.org.springframework.content.rest.links;

import internal.org.springframework.content.rest.support.BaseUriConfig;
import internal.org.springframework.content.rest.support.TestEntity;
import internal.org.springframework.content.rest.support.TestEntity3;
import internal.org.springframework.content.rest.support.TestEntity3ContentRepository;
import internal.org.springframework.content.rest.support.TestEntity3Repository;
import internal.org.springframework.content.rest.support.TestEntityContentRepository;
import internal.org.springframework.content.rest.support.TestEntityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.HypermediaConfiguration;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.DelegatingWebMvcConfiguration;

import java.io.ByteArrayInputStream;

import static java.lang.String.format;

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
public class BaseUriContentLinksIT {

	@Autowired
	TestEntityRepository repository;
	@Autowired
	TestEntityContentRepository contentRepository;
	@Autowired
	TestEntity3Repository repository3;
	@Autowired
	TestEntity3ContentRepository contentRepository3;

	@Autowired
	private WebApplicationContext context;

	@Nested
	@DisplayName("given the spring content baseUri property is set to contentApi")
	class GivenBaseUriSet {

		private MockMvc mvc;
		private TestEntity3 testEntity3;
		private ContentLinkTests contentLinkTests;

		@BeforeEach
		void setup() {
			mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		@Nested
		@DisplayName("given an Entity and a Store with a default store path")
		class GivenEntityAndStore {

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
				contentLinkTests.setLinkRel("content");
				contentLinkTests.setExpectedLinkRegex(format("http://localhost/contentApi/testEntity3s/%s/content", testEntity3.getId()));
			}
		}
	}

	@SuppressWarnings("unused")
	private void noop() {}
}
