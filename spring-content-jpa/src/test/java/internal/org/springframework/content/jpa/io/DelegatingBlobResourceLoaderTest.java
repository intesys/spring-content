package internal.org.springframework.content.jpa.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import org.springframework.content.jpa.io.BlobResourceLoader;
import org.springframework.content.jpa.io.CustomizableBlobResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class DelegatingBlobResourceLoaderTest {

	private DelegatingBlobResourceLoader service;

	private DataSource ds;
	private List<BlobResourceLoader> loaders;

	private BlobResourceLoader customLoader;

	private JdbcTemplate template;
	private PlatformTransactionManager txnMgr;

	private Resource resource;

	@Nested
	@DisplayName("DelegatingBlobResourceLoader")
	class Delegatingblobresourceloader {
		@Nested
		@DisplayName("#getResource")
		class Getresource {
			void setupService() throws Exception {
				service = new DelegatingBlobResourceLoader(ds, loaders);
				resource = service.getResource("some-id");
			}
			@Nested
			@DisplayName("given a custom blob resource loader")
			class GivenACustomBlobResourceLoader {
				@BeforeEach
				void setUp() throws Exception {
					ds = mock(DataSource.class);
					Connection conn = mock(Connection.class);
					DatabaseMetaData metadata = mock(DatabaseMetaData.class);
					when(ds.getConnection()).thenReturn(conn);
					when(conn.getMetaData()).thenReturn(metadata);
					when(metadata.getDatabaseProductName())
							.thenReturn("my-custom-db");

					customLoader = mock(BlobResourceLoader.class);
					when(customLoader.getDatabaseName()).thenReturn("my-custom-db");

					loaders = new ArrayList<>();
					loaders.add(customLoader);
				}
				@Test
				@DisplayName("should return a PostgresBlobResource")
				void shouldReturnAPostgresblobresource() throws Exception {
					setupService();
					verify(customLoader).getResource(anyString());
				}
			}
			@Nested
			@DisplayName("given a datasource that doesn't have a matching blobresourceloader")
			class GivenADatasourceThatDoesnTHaveAMatchingBlobresourceloader {
				@BeforeEach
				void setUp() throws Exception {
					ds = mock(DataSource.class);
					Connection conn = mock(Connection.class);
					DatabaseMetaData metadata = mock(DatabaseMetaData.class);
					when(ds.getConnection()).thenReturn(conn);
					when(conn.getMetaData()).thenReturn(metadata);
					when(metadata.getDatabaseProductName())
							.thenReturn("SomeOtherDatabase");

					loaders = new ArrayList<>();
					loaders.add(new CustomizableBlobResourceLoader(
							mock(JdbcTemplate.class),
							mock(PlatformTransactionManager.class)));
				}
				@Test
				@DisplayName("should return a GenericBlobResource")
				void shouldReturnAGenericblobresource() throws Exception {
					setupService();
					assertThat(resource,
							instanceOf(GenericBlobResource.class));
				}
			}
		}
	}
}
