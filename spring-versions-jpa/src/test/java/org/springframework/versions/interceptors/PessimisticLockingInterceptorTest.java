package org.springframework.versions.interceptors;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import jakarta.persistence.Id;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.security.core.Authentication;
import org.springframework.util.ReflectionUtils;
import org.springframework.versions.LockOwnerException;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@DisplayName("PessimisticLockingInterceptor")
public class PessimisticLockingInterceptorTest {

    private PessimisticLockingInterceptor interceptor;

    private LockingService locker;
    private AuthenticationFacade auth;
    private ProxyMethodInvocation mi;

    private Object result;
    private Throwable e;

    private TestEntity entity;

    private Authentication principal, lockOwner;

    @BeforeEach
    void init() {
        locker = mock(LockingService.class);
        auth = mock(AuthenticationFacade.class);
        interceptor = new PessimisticLockingInterceptor(locker, auth);
    }

    @Nested
    @DisplayName("#invoke")
    class Invoke {

        @BeforeEach
        void setup() {
            mi = mock(ProxyMethodInvocation.class);
        }

        @Nested
        @DisplayName("given a method invocation")
        class MethodInvocation {

            @Nested
            @DisplayName("given the method is setContent")
            class SetContent {

                @BeforeEach
                void setup() {
                    when(mi.getMethod()).thenReturn(ReflectionUtils.findMethod(ContentStore.class, "setContent", Object.class, InputStream.class));
                    when(mi.getArguments()).thenReturn(new Object[]{new TestEntity(), new ByteArrayInputStream("".getBytes())});
                }

                @Nested
                @DisplayName("when there is no lock owner")
                class NoLockOwner {

                    @BeforeEach
                    void setupAndInvoke() {
                        when(locker.lockOwner(0L)).thenReturn(null);
                        try {
                            result = interceptor.invoke(mi);
                        } catch (Throwable ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should proceed")
                    void shouldProceed() throws Throwable {
                        verify(locker).lockOwner(0L);
                        verify(mi).proceed();
                    }
                }

                @Nested
                @DisplayName("when the principal is the lock owner")
                class PrincipalIsLockOwner {

                    @BeforeEach
                    void setupAndInvoke() {
                        principal = mock(Authentication.class);
                        when(auth.getAuthentication()).thenReturn(principal);
                        when(locker.lockOwner(0L)).thenReturn(principal);
                        when(locker.isLockOwner(eq(0L), any())).thenReturn(true);
                        try {
                            result = interceptor.invoke(mi);
                        } catch (Throwable ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should proceed")
                    void shouldProceed() throws Throwable {
                        verify(locker).lockOwner(0L);
                        verify(locker).isLockOwner(0L, principal);
                        verify(mi).proceed();
                    }
                }

                @Nested
                @DisplayName("when the principal is not the lock owner")
                class PrincipalIsNotLockOwner {

                    @BeforeEach
                    void setupAndInvoke() {
                        lockOwner = mock(Authentication.class);
                        principal = mock(Authentication.class);
                        when(auth.getAuthentication()).thenReturn(principal);
                        when(locker.lockOwner(0L)).thenReturn(lockOwner);
                        when(locker.isLockOwner(eq(0L), any())).thenReturn(false);
                        try {
                            result = interceptor.invoke(mi);
                        } catch (Throwable ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should throw LockOwnerException")
                    void shouldThrowLockOwnerException() throws Throwable {
                        assertThat(e, is(instanceOf(LockOwnerException.class)));
                    }
                }

                @Nested
                @DisplayName("when the entity doesn't have an ID")
                class EntityWithoutId {

                    @BeforeEach
                    void setupAndInvoke() {
                        when(mi.getArguments()).thenReturn(new Object[]{new Object(), new ByteArrayInputStream("".getBytes())});
                        try {
                            result = interceptor.invoke(mi);
                        } catch (Throwable ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should proceed")
                    void shouldProceed() throws Throwable {
                        verify(mi).proceed();
                    }
                }
            }
        }
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public class TestEntity {
        @Id
        private Long id = 0L;
    }

    @NoArgsConstructor
    @Getter
    @Setter
    public class TestEntity2 {
        @org.springframework.data.annotation.Id
        private Long id = 0L;
    }
}
