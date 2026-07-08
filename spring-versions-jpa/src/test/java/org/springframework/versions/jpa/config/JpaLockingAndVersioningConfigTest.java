package org.springframework.versions.jpa.config;

import internal.org.springframework.versions.AuthenticationFacade;
import internal.org.springframework.versions.LockingService;
import internal.org.springframework.versions.jpa.CloningService;
import internal.org.springframework.versions.jpa.EntityInformationFacade;
import internal.org.springframework.versions.jpa.VersioningService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.versions.LockingAndVersioningProxyFactory;

import jakarta.persistence.EntityManager;
import javax.sql.DataSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("JpaLockingAndVersioningConfig")
public class JpaLockingAndVersioningConfigTest {

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void init() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();
    }

    @Test
    @DisplayName("should have an AuthenticationFacade bean")
    void shouldHaveAuthenticationFacade() {
        assertThat(context.getBean(AuthenticationFacade.class), is(not(nullValue())));
    }

    @Test
    @DisplayName("should have an EntityInformationFacade bean")
    void shouldHaveEntityInformationFacade() {
        assertThat(context.getBean(EntityInformationFacade.class), is(not(nullValue())));
    }

    @Test
    @DisplayName("should have a LockingService bean")
    void shouldHaveLockingService() {
        assertThat(context.getBean(LockingService.class), is(not(nullValue())));
    }

    @Test
    @DisplayName("should have a VersioningService bean")
    void shouldHaveVersioningService() {
        assertThat(context.getBean(VersioningService.class), is(not(nullValue())));
    }

    @Test
    @DisplayName("should have a CloningService bean")
    void shouldHaveCloningService() {
        assertThat(context.getBean(CloningService.class), is(not(nullValue())));
    }

    @Test
    @DisplayName("should have a LockingAndVersioningProxyFactory bean")
    void shouldHaveLockingAndVersioningProxyFactory() {
        assertThat(context.getBean(LockingAndVersioningProxyFactory.class), is(not(nullValue())));
    }

    @Configuration
    @Import(JpaLockingAndVersioningConfig.class)
    public static class TestConfig {

        @Bean
        public DataSource ds() {
            return mock(DataSource.class);
        }

        @Bean
        public PlatformTransactionManager txn() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        public EntityManager em() {
            return mock(EntityManager.class);
        }
    }
}
