package internal.org.springframework.versions.jpa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.versions.LockingAndVersioningException;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

@DisplayName("JpaCloningServiceImpl")
public class JpaCloningServiceImplTest {

    private JpaCloningServiceImpl cloner;

    private Object entity;
    private Object result;

    private Exception e;

    @BeforeEach
    void init() {
        cloner = new JpaCloningServiceImpl();
    }

    @Nested
    @DisplayName("#clone")
    class Clone {

        @Nested
        @DisplayName("given an entity with a copy constructor")
        class WithCopyConstructor {

            @BeforeEach
            void setupAndClone() {
                entity = new TestEntity();
                try {
                    result = cloner.clone(entity);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should clone the entity")
            void shouldCloneEntity() {
                assertThat(result, is(not(nullValue())));
                assertThat(result, is(not(entity)));
            }
        }

        @Nested
        @DisplayName("given an entity without a copy constructor")
        class WithoutCopyConstructor {

            @BeforeEach
            void setupAndClone() {
                entity = new NoCopyConstructorTestEntity();
                try {
                    result = cloner.clone(entity);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw LockingAndVersioningException")
            void shouldThrowException() {
                assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                assertThat(e.getMessage(), containsString("no copy constructor"));
            }
        }

        @Nested
        @DisplayName("given an entity with a failing copy constructor")
        class WithFailingCopyConstructor {

            @BeforeEach
            void setupAndClone() {
                entity = new FailingCopyConstructorTestEntity();
                try {
                    result = cloner.clone(entity);
                } catch (Exception ex) {
                    e = ex;
                }
            }

            @Test
            @DisplayName("should throw LockingAndVersioningException")
            void shouldThrowException() {
                assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                assertThat(e.getMessage(), containsString("copy constructor failed"));
            }
        }
    }

    public static class TestEntity {
        public TestEntity() {}

        public TestEntity(TestEntity entity) {}
    }

    public static class NoCopyConstructorTestEntity {
        public NoCopyConstructorTestEntity() {}
    }

    public static class FailingCopyConstructorTestEntity {
        public FailingCopyConstructorTestEntity() {}

        public FailingCopyConstructorTestEntity(FailingCopyConstructorTestEntity entity) {
            throw new RuntimeException();
        }
    }
}
