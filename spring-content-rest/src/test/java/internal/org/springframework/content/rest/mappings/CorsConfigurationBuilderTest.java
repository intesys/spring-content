package internal.org.springframework.content.rest.mappings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.UUID;

import org.springframework.content.commons.repository.Store;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class CorsConfigurationBuilderTest {

	private CorsConfigurationBuilder builder;
	private CorsConfiguration config;

	private Class<?> storeInterface;

	private Exception e;

	@Nested
	@DisplayName("CorsConfigurationBuilder")
	class Corsconfigurationbuilder {
		@Nested
		@DisplayName("#build")
		class Build {
			@BeforeEach
			void setUp() throws Exception {
				builder = new CorsConfigurationBuilder();
			}

			void buildConfig() {
				try {
					config = builder.build(storeInterface);
				}
				catch (Exception ex) {
					e = ex;
				}
			}

			@Nested
			@DisplayName("given no CrossOrigin annotation")
			class GivenNoCrossoriginAnnotation {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithNoCrossOrigin.class;
				}

				@Test
				@DisplayName("should not a cors configuration")
				void shouldNotACorsConfiguration() throws Exception {
					buildConfig();
					assertThat(config, is(nullValue()));
				}
			}

			@Nested
			@DisplayName("given an origins value")
			class GivenAnOriginsValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithOrigins.class;
				}

				@Test
				@DisplayName("should create a cors configuration with those origins")
				void shouldCreateACorsConfigurationWithThoseOrigins() throws Exception {
					buildConfig();
					assertThat(config.getAllowedOrigins(),
							hasItems("http://domain1.com", "http://domain2.com"));
				}
			}

			@Nested
			@DisplayName("given an empty origins value")
			class GivenAnEmptyOriginsValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithEmptyOrigins.class;
				}

				@Test
				@DisplayName("should set the default origin")
				void shouldSetTheDefaultOrigin() throws Exception {
					buildConfig();
					assertThat(config.getAllowedOrigins(), hasItems("*"));
				}
			}

			@Nested
			@DisplayName("given an allowedMethods value")
			class GivenAnAllowedmethodsValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithAllowedMethods.class;
				}

				@Test
				@DisplayName("should create a cors configuration with those allowed methods")
				void shouldCreateACorsConfigurationWithThoseAllowedMethods() throws Exception {
					buildConfig();
					assertThat(config.getAllowedMethods(),
							hasItems("GET", "PUT", "POST", "DELETE"));
				}
			}

			@Nested
			@DisplayName("given an empty allowedMethods value")
			class GivenAnEmptyAllowedmethodsValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithEmptyAllowedMethods.class;
				}

				@Test
				@DisplayName("should set the default allowedMethods")
				void shouldSetTheDefaultAllowedmethods() throws Exception {
					buildConfig();
					assertThat(config.getAllowedMethods(), hasItems("GET", "POST", "HEAD"));
				}
			}

			@Nested
			@DisplayName("given an allowedHeaders value")
			class GivenAnAllowedheadersValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithAllowedHeaders.class;
				}

				@Test
				@DisplayName("should create a cors configuration with those allowed headers")
				void shouldCreateACorsConfigurationWithThoseAllowedHeaders() throws Exception {
					buildConfig();
					assertThat(config.getAllowedHeaders(),
							hasItems("header1", "header2"));
				}
			}

			@Nested
			@DisplayName("given an empty allowedHeaders value")
			class GivenAnEmptyAllowedheadersValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithEmptyAllowedHeaders.class;
				}

				@Test
				@DisplayName("should set the default allowedHeaders")
				void shouldSetTheDefaultAllowedheaders() throws Exception {
					buildConfig();
					assertThat(config.getAllowedHeaders(), hasItems("*"));
				}
			}

			@Nested
			@DisplayName("given an exposedHeaders value")
			class GivenAnExposedheadersValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithExposedHeaders.class;
				}

				@Test
				@DisplayName("should create a cors configuration with those exposed headers")
				void shouldCreateACorsConfigurationWithThoseExposedHeaders() throws Exception {
					buildConfig();
					assertThat(config.getExposedHeaders(),
							hasItems("exposed1", "exposed2"));
				}
			}

			@Nested
			@DisplayName("given an empty exposedHeaders value")
			class GivenAnEmptyExposedheadersValue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithEmptyExposedHeaders.class;
				}

				@Test
				@DisplayName("should create a cors configuration with a null exposedHeaders")
				void shouldCreateACorsConfigurationWithANullExposedheaders() throws Exception {
					buildConfig();
					assertThat(config.getExposedHeaders(), is(nullValue()));
				}
			}

			@Nested
			@DisplayName("given an allowCredentials value of 'true'")
			class GivenAnAllowcredentialsValueOfTrue {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithAllowCredentials.class;
				}

				@Test
				@DisplayName("should create a cors configuration with that value")
				void shouldCreateACorsConfigurationWithThatValue() throws Exception {
					buildConfig();
					assertThat(config.getAllowCredentials(), is(true));
				}
			}

			@Nested
			@DisplayName("given an allowCredentials value of 'false'")
			class GivenAnAllowcredentialsValueOfFalse {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithDisallowCredentials.class;
				}

				@Test
				@DisplayName("should create a cors configuration with that value")
				void shouldCreateACorsConfigurationWithThatValue() throws Exception {
					buildConfig();
					assertThat(config.getAllowCredentials(), is(false));
				}
			}

			@Nested
			@DisplayName("given an allowCredentials value of 'something-else'")
			class GivenAnAllowcredentialsValueOfSomethingElse {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithMisconfiguredAllowCredentials.class;
				}

				@Test
				@DisplayName("should create a cors configuration with that value")
				void shouldCreateACorsConfigurationWithThatValue() throws Exception {
					buildConfig();
					assertThat(e, is(not(nullValue())));
				}
			}

			@Nested
			@DisplayName("given an allowCredentials value of ''")
			class GivenAnAllowcredentialsValueOf {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithEmptyAllowCredentials.class;
				}

				@Test
				@DisplayName("should create a cors configuration with the default allow credentials value")
				void shouldCreateACorsConfigurationWithTheDefaultAllowCredentialsValue() throws Exception {
					buildConfig();
					assertThat(config.getAllowCredentials(), is(nullValue()));
				}
			}

			@Nested
			@DisplayName("given a positive max-age specification")
			class GivenAPositiveMaxAgeSpecification {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithMaxAge.class;
				}

				@Test
				@DisplayName("should create a cors configuration with that max age")
				void shouldCreateACorsConfigurationWithThatMaxAge() throws Exception {
					buildConfig();
					assertThat(config.getMaxAge(), is(1000L));
				}
			}

			@Nested
			@DisplayName("given an zero max-age specification")
			class GivenAnZeroMaxAgeSpecification {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithZeroMaxAge.class;
				}

				@Test
				@DisplayName("should create a cors configuration with that max age")
				void shouldCreateACorsConfigurationWithThatMaxAge() throws Exception {
					buildConfig();
					assertThat(config.getMaxAge(), is(0L));
				}
			}

			@Nested
			@DisplayName("given an negative max-age specification")
			class GivenAnNegativeMaxAgeSpecification {
				@BeforeEach
				void setUp() throws Exception {
					storeInterface = StoreWithNegativeMaxAge.class;
				}

				@Test
				@DisplayName("should create a cors configuration with the default max age")
				void shouldCreateACorsConfigurationWithTheDefaultMaxAge() throws Exception {
					buildConfig();
					assertThat(config.getMaxAge(), is(1800L));
				}
			}
		}
	}

	public interface StoreWithNoCrossOrigin extends Store<UUID> {
	}

	@CrossOrigin(origins = { "http://domain1.com", "http://domain2.com" })
	public interface StoreWithOrigins extends Store<UUID> {
	}

	@CrossOrigin(origins = {})
	public interface StoreWithEmptyOrigins extends Store<UUID> {
	}

	@CrossOrigin(allowedHeaders = { "header1", "header2" })
	public interface StoreWithAllowedHeaders extends Store<UUID> {
	}

	@CrossOrigin(allowedHeaders = {})
	public interface StoreWithEmptyAllowedHeaders extends Store<UUID> {
	}

	@CrossOrigin(exposedHeaders = { "exposed1", "exposed2" })
	public interface StoreWithExposedHeaders extends Store<UUID> {
	}

	@CrossOrigin(exposedHeaders = {})
	public interface StoreWithEmptyExposedHeaders extends Store<UUID> {
	}

	@CrossOrigin(methods = { RequestMethod.GET, RequestMethod.PUT, RequestMethod.POST,
			RequestMethod.DELETE })
	public interface StoreWithAllowedMethods extends Store<UUID> {
	}

	@CrossOrigin(methods = {})
	public interface StoreWithEmptyAllowedMethods extends Store<UUID> {
	}

	@CrossOrigin(allowCredentials = "true")
	public interface StoreWithAllowCredentials extends Store<UUID> {
	}

	@CrossOrigin(allowCredentials = "false")
	public interface StoreWithDisallowCredentials extends Store<UUID> {
	}

	@CrossOrigin(allowCredentials = "something-else")
	public interface StoreWithMisconfiguredAllowCredentials extends Store<UUID> {
	}

	@CrossOrigin(allowCredentials = "")
	public interface StoreWithEmptyAllowCredentials extends Store<UUID> {
	}

	@CrossOrigin(maxAge = 1000L)
	public interface StoreWithMaxAge extends Store<UUID> {
	}

	@CrossOrigin(maxAge = 0L)
	public interface StoreWithZeroMaxAge extends Store<UUID> {
	}

	@CrossOrigin(maxAge = -1000L)
	public interface StoreWithNegativeMaxAge extends Store<UUID> {
	}
}
