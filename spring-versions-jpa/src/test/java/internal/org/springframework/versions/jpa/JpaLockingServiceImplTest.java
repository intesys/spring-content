package internal.org.springframework.versions.jpa;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.rowset.SqlRowSet;

@DisplayName("JpaLockingServiceImpl")
public class JpaLockingServiceImplTest {

    private JpaLockingServiceImpl locker;

    private JdbcTemplate jdbcTemplate;

    private Object entityId;
    private Principal principal;

    private Object result;
    private Exception e;

    @BeforeEach
    void init() {
        jdbcTemplate = mock(JdbcTemplate.class);
        locker = new JpaLockingServiceImpl(jdbcTemplate);
    }

    @Nested
    @DisplayName("#lock")
    class Lock {

        @BeforeEach
        void setup() {
            entityId = "some-id";
            principal = mock(Principal.class);
            when(principal.getName()).thenReturn("some-principal");
        }

        @Nested
        @DisplayName("given selecting a lock record fails")
        class WhenSelectFails {

            @BeforeEach
            void setupAndCallLock() {
                when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(Class.class)))
                        .thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                try {
                    result = locker.lock(entityId, principal);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw the DataAccessException.class")
            void shouldThrowDataAccessException() {
                assertThat(e, is(instanceOf(DataAccessException.class)));
                assertThat(e.getMessage(), is("connection-error"));
            }
        }

        @Nested
        @DisplayName("given inserting the lock record fails")
        class WhenInsertFails {

            @BeforeEach
            void setupAndCallLock() {
                ResultSet rs = mock(ResultSet.class);
                when(jdbcTemplate.queryForObject(any(String.class), any(Object[].class), any(Class.class))).thenReturn(0);
                when(jdbcTemplate.update(any(String.class), any(Object[].class)))
                        .thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                try {
                    result = locker.lock(entityId, principal);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw the DataAccessException.class")
            void shouldThrowDataAccessException() {
                assertThat(e, is(instanceOf(DataAccessException.class)));
                assertThat(e.getMessage(), is("connection-error"));
            }
        }
    }

    @Nested
    @DisplayName("#unlock")
    class Unlock {

        @BeforeEach
        void setup() {
            entityId = "some-id";
            principal = mock(Principal.class);
            when(principal.getName()).thenReturn("some-principal");
        }

        @Nested
        @DisplayName("given the lock record deletion fails")
        class WhenDeleteFails {

            @BeforeEach
            void setupAndCallUnlock() {
                when(jdbcTemplate.update(any(String.class), any(Object[].class)))
                        .thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                try {
                    result = locker.unlock(entityId, principal);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw a DataAccessException")
            void shouldThrowDataAccessException() {
                assertThat(e, is(instanceOf(DataAccessException.class)));
                assertThat(e.getMessage(), is("connection-error"));
            }
        }
    }

    @Nested
    @DisplayName("#isLockOwner")
    class IsLockOwner {

        @BeforeEach
        void setup() {
            entityId = "some-id";
            principal = mock(Principal.class);
            when(principal.getName()).thenReturn("some-principal");
        }

        @Nested
        @DisplayName("given a null principal")
        class WhenNullPrincipal {

            @BeforeEach
            void setupAndCallIsLockOwner() {
                principal = null;
                try {
                    result = locker.isLockOwner(entityId, principal);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw a SecurityException")
            void shouldThrowSecurityException() {
                assertThat(e, is(instanceOf(SecurityException.class)));
            }
        }

        @Nested
        @DisplayName("given the database fails")
        class WhenDbFails {

            @BeforeEach
            void setupAndCallIsLockOwner() {
                when(jdbcTemplate.queryForRowSet(any(String.class), any(Object[].class)))
                        .thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                try {
                    result = locker.isLockOwner(entityId, principal);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw the DataAccessException")
            void shouldThrowDataAccessException() {
                assertThat(e, is(instanceOf(DataAccessException.class)));
            }
        }

        @Nested
        @DisplayName("given the principal is the lock owner")
        class WhenIsLockOwner {

            @BeforeEach
            void setupAndCallIsLockOwner() {
                SqlRowSet rs = mock(SqlRowSet.class);
                when(rs.next()).thenReturn(true);
                when(jdbcTemplate.queryForRowSet(any(String.class), any(Object[].class))).thenReturn(rs);
                try {
                    result = locker.isLockOwner(entityId, principal);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should return true")
            void shouldReturnTrue() {
                assertThat(result, is(true));
            }
        }

        @Nested
        @DisplayName("given the principal is not the lock owner")
        class WhenNotLockOwner {

            @BeforeEach
            void setupAndCallIsLockOwner() {
                SqlRowSet rs = mock(SqlRowSet.class);
                when(rs.next()).thenReturn(false);
                when(jdbcTemplate.queryForRowSet(any(String.class), any(Object[].class))).thenReturn(rs);
                try {
                    result = locker.isLockOwner(entityId, principal);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should return false")
            void shouldReturnFalse() {
                assertThat(result, is(false));
            }
        }
    }

    @Nested
    @DisplayName("#lockOwner")
    class LockOwner {

        @BeforeEach
        void setup() {
            entityId = "some-id";
        }

        @Nested
        @DisplayName("given the database fails")
        class WhenDbFails {

            @BeforeEach
            void setupAndCallLockOwner() {
                when(jdbcTemplate.query(anyString(), (RowMapper) any()))
                        .thenThrow(new CannotGetJdbcConnectionException("connection-error"));
                try {
                    result = locker.lockOwner(entityId);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw the DataAccessException")
            void shouldThrowDataAccessException() {
                assertThat(e, is(instanceOf(DataAccessException.class)));
            }
        }

        @Nested
        @DisplayName("given there is no lock record")
        class WhenNoLockRecord {

            @BeforeEach
            void setupAndCallLockOwner() {
                when(jdbcTemplate.query(anyString(), (RowMapper) any())).thenReturn(null);
                try {
                    result = locker.lockOwner(entityId);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should return null")
            void shouldReturnNull() {
                assertThat(result, is(nullValue()));
            }
        }

        @Nested
        @DisplayName("given there is a lock record")
        class WhenLockRecordExists {

            @BeforeEach
            void setupAndCallLockOwner() {
                when(jdbcTemplate.query(anyString(), (RowMapper) any()))
                        .thenReturn(Collections.singletonList("some-principal"));
                try {
                    result = locker.lockOwner(entityId);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should return a principal")
            void shouldReturnPrincipal() {
                assertThat(result, is(instanceOf(Principal.class)));
                assertThat(((Principal) result).getName(), is("some-principal"));
            }
        }

        @Nested
        @DisplayName("given there are mulitple lock records")
        class WhenMultipleLockRecords {

            @BeforeEach
            void setupAndCallLockOwner() {
                when(jdbcTemplate.query(anyString(), (RowMapper) any()))
                        .thenReturn(Arrays.asList("some-principal", "some-other-principal"));
                try {
                    result = locker.lockOwner(entityId);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw an IncorrectResultSize exception")
            void shouldThrowIncorrectResultSize() {
                assertThat(e, is(instanceOf(IncorrectResultSizeDataAccessException.class)));
            }
        }
    }
}
