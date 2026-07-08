package it.internal.org.springframework.content.rest.controllers;

import internal.org.springframework.content.rest.support.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.rest.config.RestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestMvcConfiguration;
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
		FullyQualifiedLinksConfig.class,
		DelegatingWebMvcConfiguration.class,
		RepositoryRestMvcConfiguration.class,
		RestConfiguration.class })
@Transactional
public class FullyQualifiedLinksIT {

	@Autowired
	private TestEntityRepository repo3;

	@Autowired
	private TestEntityContentRepository store3;

	@Autowired
	private WebApplicationContext context;

	@Nested
	@DisplayName("ContextPath Content Tests")
	class ContextPathContentTests {

		private MockMvc mvc;
		private TestEntity testEntity3;
		private Content contentTests;

		@BeforeEach
		void setup() {
			mvc = MockMvcBuilders.webAppContextSetup(context).build();
		}

		@Nested
		@DisplayName("given an entity is the subject of a repository and storage")
		class GivenEntity {

			@BeforeEach
			void init() {
				testEntity3 = repo3.save(new TestEntity());
				testEntity3 = repo3.save(testEntity3);

				contentTests = new Content();
				contentTests.setMvc(mvc);
				contentTests.setUrl("/testEntitiesContent/" + testEntity3.getId() + "/content");
				contentTests.setEntity(testEntity3);
				contentTests.setRepository(repo3);
				contentTests.setStore(store3);
			}
		}
	}
}
