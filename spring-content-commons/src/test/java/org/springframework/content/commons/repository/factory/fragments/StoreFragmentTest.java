package org.springframework.content.commons.repository.factory.fragments;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.factory.testsupport.EnableTestStores;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = StoreFragmentTest.StoreTestConfiguration.class)
public class StoreFragmentTest {

	@Autowired
	private ApplicationContext context;

	@Nested
	@DisplayName("given a store definition")
	class GivenAStoreDefinition {

		@Nested
		@DisplayName("given the application context")
		class GivenTheApplicationContext {

			@Test
			@DisplayName("should support the extension")
			void shouldSupportTheExtension() throws Exception {

				assertThat(context.getBean(TestContentStore.class), is(not(nullValue())));
				assertThat(context.getBean(CustomizationImpl.class).getBean(), is("Spring Content"));
				assertThat(context.getBean(CustomizationImpl.class).getDomainClass(), is(Object.class));
				assertThat(context.getBean(CustomizationImpl.class).getIdClass(), is(Serializable.class));
				assertThat(context.getBean(TestContentStore.class).greet("World"), is("Hello Spring Content World"));
			}
		}
	}

	@Configuration
	@EnableTestStores
	public static class StoreTestConfiguration {

		@Bean
		public String bean() {
			return "Spring Content";
		}
	}

	public interface TestContentStore extends ContentStore<Object, Serializable>, Customization {
	}

	public interface Customization {
		String greet(String name);
	}

	@Getter
	@Setter
	public static class CustomizationImpl implements Customization {

		@Autowired
		private String bean;

		private Class<?> domainClass;
		private Class<?> idClass;

		@Override
		public String greet(String name) {
			return "Hello " + bean + " " + name;
		}
	}
}
