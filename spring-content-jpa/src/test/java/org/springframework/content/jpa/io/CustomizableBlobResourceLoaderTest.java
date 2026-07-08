package org.springframework.content.jpa.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import internal.org.springframework.content.jpa.io.GenericBlobResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CustomizableBlobResourceLoaderTest {

	private CustomizableBlobResourceLoader loader;

	private JdbcTemplate template;
	private PlatformTransactionManager txnMgr;

	private DataSource ds;
	private Connection conn;
	private Statement stmt;

	private Resource customDBResource;

	private Object result;

	@Nested
	@DisplayName("CustomizableBlobResourceLoader")
	class Customizableblobresourceloader {
		@BeforeEach
		void setUp() throws Exception {
			loader = new CustomizableBlobResourceLoader(template, txnMgr);
		}
		@Nested
		@DisplayName("#getDatabaseName")
		class Getdatabasename {
			@BeforeEach
			void setUp() throws Exception {
				result = loader.getDatabaseName();
			}
			@Test
			@DisplayName("should return 'GENERIC'")
			void shouldReturnGeneric() throws Exception {
				assertThat(result.toString(), is("GENERIC"));
			}
		}
		@Nested
		@DisplayName("#getResource")
		class Getresource {
			@BeforeEach
			void setUp() throws Exception {
				ds = mock(DataSource.class);
				template = new JdbcTemplate(ds);
				txnMgr = new DataSourceTransactionManager(ds);

				conn = mock(Connection.class);
				when(ds.getConnection()).thenReturn(conn);
				stmt = mock(Statement.class);
				when(conn.createStatement()).thenReturn(stmt);

				result = loader.getResource("some-id");
			}
			@Test
			@DisplayName("should return a GenericBlobResource")
			void shouldReturnAGenericblobresource() throws Exception {
				assertThat(result, instanceOf(GenericBlobResource.class));
			}
		}
		@Nested
		@DisplayName("#getClassLoader")
		class Getclassloader {
			@BeforeEach
			void setUp() throws Exception {
				result = loader.getClassLoader();
			}
			@Test
			@DisplayName("should return a class loader")
			void shouldReturnAClassLoader() throws Exception {
				assertThat(result, instanceOf(ClassLoader.class));
			}
		}
		@Nested
		@DisplayName("given a resource provider")
		class GivenAResourceProvider {
			@BeforeEach
			void setUp() throws Exception {
				customDBResource = mock(Resource.class);
				loader = new CustomizableBlobResourceLoader(template, txnMgr, "CUSTOM_DB", (l, t, txn) -> { return customDBResource; });
			}
			@Nested
			@DisplayName("#getResource")
			class Getresource {
				@BeforeEach
				void setUp() throws Exception {
					ds = mock(DataSource.class);
					template = new JdbcTemplate(ds);
					txnMgr = new DataSourceTransactionManager(ds);

					result = loader.getResource("some-id");
				}
				@Test
				@DisplayName("should return the resource providers custom resource")
				void shouldReturnTheResourceProvidersCustomResource() throws Exception {
					assertThat(result, is(customDBResource));
				}
			}
		}
	}
}
