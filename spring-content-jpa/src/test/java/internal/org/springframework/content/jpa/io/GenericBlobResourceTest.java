package internal.org.springframework.content.jpa.io;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@DisplayName("GenericBlobResource")
public class GenericBlobResourceTest {

	private GenericBlobResource resource;

	private String id;
	private JdbcTemplate template;
	private PlatformTransactionManager txnMgr;

	private DataSource ds;
	private Connection conn;
	private Statement statement;
	private ResultSet rs;

	private Object result;

	@BeforeEach
	void setUp() throws Exception {
		ds = mock(DataSource.class);
		template = new JdbcTemplate(ds);
		txnMgr = new DataSourceTransactionManager(ds);
	}

	@Nested
	@DisplayName("#exists")
	class Exists {

		@BeforeEach
		void setUp() throws Exception {
			conn = mock(Connection.class);
			statement = mock(Statement.class);
			rs = mock(ResultSet.class);

			when(ds.getConnection()).thenReturn(conn);
			when(conn.createStatement()).thenReturn(statement);
			when(statement.executeQuery(anyString())).thenReturn(rs);

			resource = new GenericBlobResource(id, template, txnMgr);
			result = resource.exists();
		}

		@Nested
		@DisplayName("given the resultset throws SQLException")
		class GivenTheResultsetThrowsSqlexception {

			@BeforeEach
			void setUp() throws Exception {
				when(rs.next()).thenThrow(new SQLException("badness"));
			}

			@Test
			@DisplayName("should return false")
			void shouldReturnFalse() throws Exception {
				assertThat(result, is(false));
			}
		}
	}

	@Nested
	@DisplayName("#getInputStream")
	class Getinputstream {

		@BeforeEach
		void setUp() throws Exception {
			conn = mock(Connection.class);
			statement = mock(Statement.class);
			rs = mock(ResultSet.class);

			when(ds.getConnection()).thenReturn(conn);
			when(conn.createStatement()).thenReturn(statement);
			when(statement.executeQuery(anyString())).thenReturn(rs);

			resource = new GenericBlobResource(id, template, txnMgr);
			result = resource.getInputStream();
		}

		@Nested
		@DisplayName("given a SQLException is thrown")
		class GivenASqlexceptionIsThrown {

			@BeforeEach
			void setUp() throws Exception {
				when(rs.next()).thenThrow(new SQLException("badness"));
			}

			@Test
			@DisplayName("should return null")
			void shouldReturnNull() throws Exception {
				assertThat(result, is(nullValue()));
			}
		}
	}
}
