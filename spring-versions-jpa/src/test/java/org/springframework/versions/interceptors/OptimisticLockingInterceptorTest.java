package org.springframework.versions.interceptors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Version;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ReflectionUtils;

import lombok.Getter;
import lombok.Setter;

@DisplayName("OptimisticLockInterceptor")
public class OptimisticLockingInterceptorTest {

    private OptimisticLockingInterceptor interceptor;

    private Object result;

    private EntityManager em;
    private ProxyMethodInvocation mi;
    private Object entity;

    @BeforeEach
    void init() {
        em = mock(EntityManager.class);
        interceptor = new OptimisticLockingInterceptor(em);
    }

    @Nested
    @DisplayName("#invoke")
    class Invoke {

        @BeforeEach
        void setup() {
            mi = mock(ProxyMethodInvocation.class);
        }

        @Nested
        @DisplayName("when the method invocation is getContent")
        class GetContent {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity});
                when(em.merge(entity)).thenReturn(entity);
                result = interceptor.invoke(mi);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(entity);
                verify(mi).proceed();
            }
        }

        @Nested
        @DisplayName("when the method invocation is getContent with PropertyPath")
        class GetContentWithPropertyPath {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "getContent", Object.class, PropertyPath.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo")});
                when(em.merge(entity)).thenReturn(entity);
                result = interceptor.invoke(mi);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(entity, PropertyPath.from("foo"));
                verify(mi).proceed();
            }
        }

        @Nested
        @DisplayName("when the method invocation is setContent")
        class SetContent {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity, new ByteArrayInputStream("".getBytes())});
                when(em.merge(entity)).thenReturn(entity);
                when(mi.proceed()).thenReturn(entity);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                result = interceptor.invoke(mi);
                assertThat(result, is(not(nullValue())));
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(eq(entity), any());
                verify(mi).proceed();
                assertThat(((TestEntity) entity).getVersion(), is(1L));
            }

            @Nested
            @DisplayName("when the entity is not @Versioned")
            class EntityNotVersioned {

                @BeforeEach
                void setupAndInvoke() throws Throwable {
                    entity = new TestEntityUnversionsed();
                    when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                    when(mi.getArguments()).thenReturn(new Object[]{entity, new ByteArrayInputStream("".getBytes())});
                    result = interceptor.invoke(mi);
                }

                @Test
                @DisplayName("should still proceed")
                void shouldProceed() throws Throwable {
                    verify(mi).proceed();
                }
            }
        }

        @Nested
        @DisplayName("when the method invocation is setContent with PropertyPath")
        class SetContentWithPropertyPath {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, InputStream.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new ByteArrayInputStream("".getBytes())});
                when(em.merge(entity)).thenReturn(entity);
                when(mi.proceed()).thenReturn(entity);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                result = interceptor.invoke(mi);
                assertThat(result, is(not(nullValue())));
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(eq(entity), eq(PropertyPath.from("foo")), any(ByteArrayInputStream.class));
                verify(mi).proceed();
                assertThat(((TestEntity) entity).getVersion(), is(1L));
            }

            @Nested
            @DisplayName("when the entity is not @Versioned")
            class EntityNotVersioned {

                @BeforeEach
                void setupAndInvoke() throws Throwable {
                    entity = new TestEntityUnversionsed();
                    when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, InputStream.class));
                    when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new ByteArrayInputStream("".getBytes())});
                    result = interceptor.invoke(mi);
                }

                @Test
                @DisplayName("should still proceed")
                void shouldProceed() throws Throwable {
                    verify(mi).proceed();
                }
            }
        }

        @Nested
        @DisplayName("when the method invocation is setContent with Resource")
        class SetContentWithResource {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity, new FileSystemResource("")});
                when(em.merge(entity)).thenReturn(entity);
                when(mi.proceed()).thenReturn(entity);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                result = interceptor.invoke(mi);
                assertThat(result, is(not(nullValue())));
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(eq(entity), any());
                verify(mi).proceed();
                assertThat(((TestEntity) entity).getVersion(), is(1L));
            }

            @Nested
            @DisplayName("when the entity is not @Versioned")
            class EntityNotVersioned {

                @BeforeEach
                void setupAndInvoke() throws Throwable {
                    entity = new TestEntityUnversionsed();
                    when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, Resource.class));
                    when(mi.getArguments()).thenReturn(new Object[]{entity, new FileSystemResource("")});
                    result = interceptor.invoke(mi);
                }

                @Test
                @DisplayName("should still proceed")
                void shouldProceed() throws Throwable {
                    verify(mi).proceed();
                }
            }
        }

        @Nested
        @DisplayName("when the method invocation is setContent with PropertyPath and Resource")
        class SetContentWithPropertyPathAndResource {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, Resource.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new FileSystemResource("")});
                when(em.merge(entity)).thenReturn(entity);
                when(mi.proceed()).thenReturn(entity);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                result = interceptor.invoke(mi);
                assertThat(result, is(not(nullValue())));
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(eq(entity), eq(PropertyPath.from("foo")), any(FileSystemResource.class));
                verify(mi).proceed();
                assertThat(((TestEntity) entity).getVersion(), is(1L));
            }

            @Nested
            @DisplayName("when the entity is not @Versioned")
            class EntityNotVersioned {

                @BeforeEach
                void setupAndInvoke() throws Throwable {
                    entity = new TestEntityUnversionsed();
                    when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, PropertyPath.class, Resource.class));
                    when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo"), new FileSystemResource("")});
                    result = interceptor.invoke(mi);
                }

                @Test
                @DisplayName("should still proceed")
                void shouldProceed() throws Throwable {
                    verify(mi).proceed();
                }
            }
        }

        @Nested
        @DisplayName("when the method invocation is unsetContent")
        class UnsetContent {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity});
                when(em.merge(entity)).thenReturn(entity);
                when(mi.proceed()).thenReturn(entity);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                result = interceptor.invoke(mi);
                assertThat(result, is(not(nullValue())));
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(eq(entity));
                verify(mi).proceed();
                assertThat(((TestEntity) entity).getVersion(), is(1L));
            }

            @Nested
            @DisplayName("when the entity is not @Versioned")
            class EntityNotVersioned {

                @BeforeEach
                void setupAndInvoke() throws Throwable {
                    entity = new TestEntityUnversionsed();
                    when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class));
                    when(mi.getArguments()).thenReturn(new Object[]{entity});
                    result = interceptor.invoke(mi);
                }

                @Test
                @DisplayName("should still proceed")
                void shouldProceed() throws Throwable {
                    verify(mi).proceed();
                }
            }
        }

        @Nested
        @DisplayName("when the method invocation is unsetContent with PropertyPath")
        class UnsetContentWithPropertyPath {

            @BeforeEach
            void setup() throws Throwable {
                entity = new TestEntity();
                when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class, PropertyPath.class));
                when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo")});
                when(em.merge(entity)).thenReturn(entity);
                when(mi.proceed()).thenReturn(entity);
            }

            @Test
            @DisplayName("should lock the entity and proceed")
            void shouldLockAndProceed() throws Throwable {
                result = interceptor.invoke(mi);
                assertThat(result, is(not(nullValue())));
                verify(em).lock(entity, LockModeType.OPTIMISTIC);
                verify(mi).setArguments(eq(entity), eq(PropertyPath.from("foo")));
                verify(mi).proceed();
                assertThat(((TestEntity) entity).getVersion(), is(1L));
            }

            @Nested
            @DisplayName("when the entity is not @Versioned")
            class EntityNotVersioned {

                @BeforeEach
                void setupAndInvoke() throws Throwable {
                    entity = new TestEntityUnversionsed();
                    when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "unsetContent", Object.class, PropertyPath.class));
                    when(mi.getArguments()).thenReturn(new Object[]{entity, PropertyPath.from("foo")});
                    result = interceptor.invoke(mi);
                }

                @Test
                @DisplayName("should still proceed")
                void shouldProceed() throws Throwable {
                    verify(mi).proceed();
                }
            }
        }
    }

    @Getter
    @Setter
    private class TestEntity {
        @Version
        private Long version = 0L;
    }

    @Getter
    @Setter
    private class TestEntityUnversionsed {
    }
}
