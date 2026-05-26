package org.springframework.content.commons.repository.factory.stores;

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
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "c")
@ContextConfiguration(classes = StoreCandidateComponentProviderEnvironmentTest.StoreTestConfiguration.class)
public class StoreCandidateComponentProviderEnvironmentTest {

	@Autowired(required=false)
	private TestStore store;

	@Autowired(required=false)
	private TestAssociativeStore associativeStore;

	@Autowired(required=false)
	private TestContentStore contentStore;

	@Nested
	@DisplayName("given two stores with profiles")
	class GivenTwoStoresWithProfiles {

		@Test
		@DisplayName("should have a store bean")
		void shouldHaveAStoreBean() throws Exception {
			assertThat(store, is(not(nullValue())));
			assertThat(associativeStore, is(nullValue()));
			assertThat(contentStore, is(not(nullValue())));
		}
	}

	@Configuration
	@EnableTestStores
	public static class StoreTestConfiguration {
	}

	public interface TestStore extends Store<URI> {
	}

	@Profile("b")
	public interface TestAssociativeStore extends AssociativeStore<Object, URI> {
	}

	@Profile("c")
	public interface TestContentStore extends ContentStore<Object, URI> {
	}
}
