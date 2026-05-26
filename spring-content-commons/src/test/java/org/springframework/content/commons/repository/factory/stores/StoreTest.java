package org.springframework.content.commons.repository.factory.stores;

import java.io.Serializable;
import java.net.URI;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.AssociativeStore;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.commons.repository.factory.testsupport.EnableTestStores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.org.springframework.content.commons.repository.AnnotatedStoreEventInvoker;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StoreTest.StoreTestConfiguration.class)
public class StoreTest {

	@Autowired
	private ApplicationContext context;

	@Nested
	@DisplayName("given a store definition")
	class GivenAStoreDefinition {

		@Nested
		@DisplayName("given the application context")
		class GivenTheApplicationContext {

			@Test
			@DisplayName("should have a store bean")
			void shouldHaveAStoreBean() throws Exception {
				assertThat(context.getBean(TestContentRepository.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should have the core spring content service beans")
			void shouldHaveTheCoreSpringContentServiceBeans() throws Exception {
				assertThat(context.getBean(AnnotatedStoreEventInvoker.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should have a TestStore bean")
			void shouldHaveATeststoreBean() throws Exception {
				assertThat(context.getBean(TestStore.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should have an TestAssociativeStore bean")
			void shouldHaveAnTestassociativestoreBean() throws Exception {
				assertThat(context.getBean(TestAssociativeStore.class), is(not(nullValue())));
			}

			@Test
			@DisplayName("should have an TestAssociativeAndContentStore bean")
			void shouldHaveAnTestassociativeandcontentstoreBean() throws Exception {
				assertThat(context.getBean(TestAssociativeAndContentStore.class), is(not(nullValue())));
			}
		}
	}

	@Configuration
	@EnableTestStores
	public static class StoreTestConfiguration {
	}

	public interface TestContentRepository extends ContentStore<Object, Serializable> {
	}

	public interface TestStore extends Store<URI> {
	}

	public interface TestAssociativeStore extends AssociativeStore<Object, URI> {
	}

	public interface TestAssociativeAndContentStore extends ContentStore<Object, URI> {
	}
}
