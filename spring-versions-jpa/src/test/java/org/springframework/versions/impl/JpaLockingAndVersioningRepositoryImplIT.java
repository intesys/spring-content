package org.springframework.versions.impl;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import javax.sql.DataSource;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.versions.AncestorId;
import org.springframework.versions.AncestorRootId;
import org.springframework.versions.LockOwner;
import org.springframework.versions.LockOwnerException;
import org.springframework.versions.LockingAndVersioningException;
import org.springframework.versions.LockingAndVersioningRepository;
import org.springframework.versions.SuccessorId;
import org.springframework.versions.VersionInfo;
import org.springframework.versions.VersionLabel;
import org.springframework.versions.VersionNumber;
import org.springframework.versions.jpa.config.JpaLockingAndVersioningConfig;

import internal.org.springframework.versions.LockingService;
import lombok.Getter;
import lombok.Setter;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JpaLockingAndVersioningRepositoryImplIT.TestConfig.class})
@DisplayName("given a locking and versioning repository and a security context")
public class JpaLockingAndVersioningRepositoryImplIT {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private LockingService lockingService;

    @Autowired
    private OtherTestRepository otherRepo;

    private TestRepository repo;

    private TestEntity e1, e2, e3, e1v11, e1v12, e2v2, e3wc, entityForDeletion, result;

    private Exception e;

    @BeforeEach
    void init() {
        repo = context.getBean(TestRepository.class);
    }

    @Nested
    @DisplayName("given two entities with two versions each")
    class WithEntities {

        @BeforeEach
        void setup() {
            e1 = new TestEntity();
            e2 = new TestEntity();
            OtherTestEntity ote = otherRepo.save(new OtherTestEntity());
        }

        @Nested
        @DisplayName("#lock")
        class Lock {

            @BeforeEach
            void setupSecurity() {
                setupSecurityContext("some-principal", true);
            }

            @Nested
            @DisplayName("given a null principal")
            class NullPrincipal {

                @BeforeEach
                void setupAndLock() {
                    setupSecurityContext(null, false);
                    try {
                        result = repo.lock(e1);
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
            @DisplayName("given the entity is new")
            class EntityIsNew {

                @BeforeEach
                void lock() {
                    try {
                        result = repo.lock(e1);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }

                @Test
                @DisplayName("should fail")
                void shouldFail() {
                    assertThat(e, is(instanceOf(InvalidDataAccessApiUsageException.class)));
                }
            }

            @Nested
            @DisplayName("given the entity exists")
            class EntityExists {

                @BeforeEach
                void saveEntity() {
                    e1 = repo.save(e1);
                }

                @Nested
                @DisplayName("when the object is not locked")
                class NotLocked {

                    @BeforeEach
                    void lock() {
                        try {
                            result = repo.lock(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should update the entity's @LockOwner field")
                    void shouldUpdateLockOwner() {
                        assertThat(e1.getXLockOwner(), is("some-principal"));
                    }

                    @Test
                    @DisplayName("should save the entity")
                    void shouldSaveEntity() {
                        assertThat(e, is(nullValue()));
                        assertThat(result.getXid(), is(e1.getXid()));
                    }
                }

                @Nested
                @DisplayName("when the lock is already taken")
                class LockAlreadyTaken {

                    @BeforeEach
                    void setupAndLock() {
                        setupSecurityContext("some-other-principal", true);
                        e1 = repo.lock(e1);
                        setupSecurityContext("some-principal", true);
                        try {
                            result = repo.lock(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should return null")
                    void shouldReturnNull() {
                        assertThat(e, is(instanceOf(LockOwnerException.class)));
                        assertThat(result, is(nullValue()));
                    }
                }

                @Nested
                @DisplayName("when the lock is already held")
                class LockAlreadyHeld {

                    @BeforeEach
                    void setupAndLock() {
                        setupSecurityContext("some-principal", true);
                        e1 = repo.lock(e1);
                        setupSecurityContext("some-principal", true);
                        try {
                            result = repo.lock(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should succeed")
                    void shouldSucceed() {
                        assertThat(e, is(nullValue()));
                        assertThat(result, is(not(nullValue())));
                    }
                }

                @Nested
                @DisplayName("when the principal is not authenticated")
                class PrincipalNotAuthenticated {

                    @BeforeEach
                    void setupAndLock() {
                        setupSecurityContext("some-principal", false);
                        try {
                            result = repo.lock(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should return SecurityException")
                    void shouldReturnSecurityException() {
                        assertThat(e, is(instanceOf(SecurityException.class)));
                    }
                }
            }
        }

        @Nested
        @DisplayName("#unlock")
        class Unlock {

            @BeforeEach
            void setupSecurity() {
                setupSecurityContext("some-principal", true);
            }

            @Nested
            @DisplayName("given a null principal")
            class NullPrincipal {

                @BeforeEach
                void setupAndUnlock() {
                    setupSecurityContext(null, false);
                    try {
                        result = repo.unlock(e1);
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
            @DisplayName("given the entity is new")
            class EntityIsNew {

                @BeforeEach
                void unlock() {
                    try {
                        result = repo.unlock(e1);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }

                @Test
                @DisplayName("should fail")
                void shouldFail() {
                    assertThat(e, is(instanceOf(InvalidDataAccessApiUsageException.class)));
                }
            }

            @Nested
            @DisplayName("given the entity exists")
            class EntityExists {

                @BeforeEach
                void saveEntity() {
                    e1 = repo.save(e1);
                }

                @Nested
                @DisplayName("given the principal is the lock owner")
                class PrincipalIsLockOwner {

                    @BeforeEach
                    void setupAndUnlock() {
                        setupSecurityContext("some-principal", true);
                        e1 = repo.lock(e1);
                        try {
                            result = repo.unlock(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should null the @LockOwner field and save")
                    void shouldNullLockOwner() {
                        assertThat(result.getXLockOwner(), is(nullValue()));
                    }

                    @Test
                    @DisplayName("should unlock the entity and return it")
                    void shouldUnlockAndReturn() {
                        assertThat(e, is(nullValue()));
                        assertThat(result.getXid(), is(e1.getXid()));
                    }
                }

                @Nested
                @DisplayName("given the principal is not the lock owner")
                class PrincipalIsNotLockOwner {

                    @BeforeEach
                    void setupAndUnlock() {
                        setupSecurityContext("some-other-principal", true);
                        e1 = repo.lock(e1);
                        setupSecurityContext("some-principal", true);
                        try {
                            result = repo.unlock(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should a SecurityException")
                    void shouldThrowSecurityException() {
                        assertThat(e, is(instanceOf(LockOwnerException.class)));
                        assertThat(e.getMessage(), containsString("not lock owner"));
                    }
                }

                @Nested
                @DisplayName("given the principal is not authenticated")
                class PrincipalNotAuthenticated {

                    @BeforeEach
                    void setupAndUnlock() {
                        setupSecurityContext("some-principal", false);
                        try {
                            result = repo.unlock(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should a SecurityException")
                    void shouldThrowSecurityException() {
                        assertThat(e, is(instanceOf(SecurityException.class)));
                    }
                }
            }
        }

        @Nested
        @DisplayName("#save")
        class Save {

            @BeforeEach
            void setup() {
                e3 = new TestEntity();
            }

            @Nested
            @DisplayName("given an authenticated principal")
            class AuthenticatedPrincipal {

                @BeforeEach
                void setupSecurity() {
                    setupSecurityContext("some-principal", true);
                }

                @Nested
                @DisplayName("given the entity is new")
                class EntityNew {

                    @Nested
                    @DisplayName("given there is no lock owner")
                    class NoLockOwner {

                        @BeforeEach
                        void save() {
                            try {
                                result = repo.save(e3);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should merge and return the entity")
                        void shouldMergeAndReturn() {
                            assertThat(e, is(nullValue()));
                            assertThat(result.getXid(), is(e3.getXid()));
                        }
                    }
                }

                @Nested
                @DisplayName("given the entity exists")
                class EntityExists {

                    @BeforeEach
                    void saveFirst() {
                        e3 = repo.save(e3);
                    }

                    @Nested
                    @DisplayName("given the principal is the lock owner")
                    class PrincipalIsLockOwner {

                        @BeforeEach
                        void setupAndSave() {
                            e3 = repo.lock(e3);
                            try {
                                result = repo.save(e3);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should return the entity")
                        void shouldReturnEntity() {
                            assertThat(e, is(nullValue()));
                            assertThat(result.getXid(), is(e3.getXid()));
                        }
                    }

                    @Nested
                    @DisplayName("given the principal is not the lock owner")
                    class PrincipalIsNotLockOwner {

                        @BeforeEach
                        void setupAndSave() {
                            e3 = repo.lock(e3);
                            setupSecurityContext("some-other-principal", true);
                            try {
                                result = repo.save(e3);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should throw a LockOwnerException")
                        void shouldThrowLockOwnerException() {
                            assertThat(e, is(instanceOf(LockOwnerException.class)));
                        }
                    }

                    @Nested
                    @DisplayName("given there is no lock owner")
                    class NoLockOwner {

                        @BeforeEach
                        void save() {
                            try {
                                result = repo.save(e3);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should merge and return the entity")
                        void shouldMergeAndReturn() {
                            assertThat(e, is(nullValue()));
                            assertThat(result.getXid(), is(e3.getXid()));
                        }
                    }
                }
            }

            @Nested
            @DisplayName("given an unauthenticated principal")
            class UnauthenticatedPrincipal {

                @BeforeEach
                void setupSecurity() {
                    setupSecurityContext("some-principal", false);
                }

                @Nested
                @DisplayName("given the entity is new")
                class EntityNew {

                    @Nested
                    @DisplayName("given there is no lock owner")
                    class NoLockOwner {

                        @BeforeEach
                        void save() {
                            try {
                                result = repo.save(e3);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should merge and return the entity")
                        void shouldMergeAndReturn() {
                            assertThat(e, is(nullValue()));
                            assertThat(result.getXid(), is(e3.getXid()));
                        }
                    }
                }

                @Nested
                @DisplayName("given the entity exists")
                class EntityExists {

                    @BeforeEach
                    void saveFirst() {
                        e3 = repo.save(e3);
                    }

                    @Nested
                    @DisplayName("given there is no lock owner")
                    class NoLockOwner {

                        @BeforeEach
                        void save() {
                            try {
                                result = repo.save(e3);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should merge and return the entity")
                        void shouldMergeAndReturn() {
                            assertThat(e, is(nullValue()));
                            assertThat(result.getXid(), is(e3.getXid()));
                        }
                    }
                }
            }

            @Nested
            @DisplayName("given there is no principal")
            class NoPrincipal {

                @Nested
                @DisplayName("given the entity is new")
                class EntityNew {

                    @Nested
                    @DisplayName("given there is no lock owner")
                    class NoLockOwner {

                        @BeforeEach
                        void save() {
                            try {
                                result = repo.save(e3);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should succeed")
                        void shouldSucceed() {
                            assertThat(e, is(nullValue()));
                            assertThat(result.getXid(), is(e3.getXid()));
                        }
                    }
                }
            }
        }

        @Nested
        @DisplayName("#findAllVersions")
        class FindAllVersions {

            @BeforeEach
            void setup() {
                setupSecurityContext("some-principal", true);

                e1 = repo.save(e1);
                e1 = repo.lock(e1);
                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                e1v11 = repo.unlock(e1v11);

                e2 = repo.save(e2);
                e2 = repo.lock(e2);
                e2v2 = repo.version(e2, new VersionInfo("2.0", "Major"));
                e2v2 = repo.unlock(e2v2);
            }

            @Test
            @DisplayName("should return the version series")
            void shouldReturnVersionSeries() {
                List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                assertThat(results.size(), is(2));
                assertThat(results, hasItems(hasProperty("xid", is(e1.getXid())), hasProperty("xid", is(e1v11.getXid()))));
            }

            @Test
            @DisplayName("should return the ordered version series")
            void shouldReturnOrderedVersionSeries() {
                List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                assertThat(results.size(), is(2));
                assertThat(results, Matchers.contains(hasProperty("xid", is(e1v11.getXid())), hasProperty("xid", is(e1.getXid()))));
            }
        }

        @Nested
        @DisplayName("#findAllLatestVersions")
        class FindAllLatestVersions {

            @BeforeEach
            void setup() {
                setupSecurityContext("some-principal", true);

                e1 = repo.save(e1);
                e1 = repo.lock(e1);
                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                e1v11 = repo.unlock(e1v11);

                e2 = repo.save(e2);
                e2 = repo.lock(e2);
                e2v2 = repo.version(e2, new VersionInfo("2.0", "Major"));
                e2v2 = repo.unlock(e2v2);

                e2v2 = repo.lock(e2v2);
                e3wc = repo.workingCopy(e2v2);
            }

            @Test
            @DisplayName("should return only the latest version of the entities")
            void shouldReturnLatestVersions() {
                List<TestEntity> results = repo.findAllVersionsLatest((Class<TestEntity>) e1.getClass());
                assertThat(results, hasItems(
                        hasProperty("xid", is(e1v11.getXid())),
                        hasProperty("xid", is(e2v2.getXid())),
                        not(hasProperty("xid", is(e3wc.getXid())))
                ));

                results = repo.findAllVersionsLatest(TestEntity.class);
                assertThat(results, hasItems(
                        hasProperty("xid", is(e1v11.getXid())),
                        hasProperty("xid", is(e2v2.getXid())),
                        not(hasProperty("xid", is(e3wc.getXid())))
                ));
            }
        }

        @Nested
        @DisplayName("#workingCopy")
        class WorkingCopy {

            @BeforeEach
            void setupSecurity() {
                setupSecurityContext("some-principal", true);
            }

            @Nested
            @DisplayName("when the entity is new")
            class EntityIsNew {

                @BeforeEach
                void callWorkingCopy() {
                    try {
                        result = repo.workingCopy(e1);
                    } catch (Exception ex) {
                        e = ex;
                    }
                }

                @Test
                @DisplayName("should fail")
                void shouldFail() {
                    assertThat(e, is(instanceOf(InvalidDataAccessApiUsageException.class)));
                }
            }

            @Nested
            @DisplayName("when the entity exists")
            class EntityExists {

                @BeforeEach
                void saveEntity() {
                    e1 = repo.save(e1);
                }

                @Nested
                @DisplayName("given the principal is the lock owner")
                class PrincipalIsLockOwner {

                    @BeforeEach
                    void setupAndCallWorkingCopy() {
                        setupSecurityContext("some-principal", true);
                        e1 = repo.lock(e1);
                        try {
                            result = repo.workingCopy(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Nested
                    @DisplayName("when the entity is not yet part of a version tree")
                    class NotYetVersioned {

                        @Test
                        @DisplayName("should create the pwc with a new id")
                        void shouldCreatePwc() {
                            assertThat(result.getXid(), is(not(nullValue())));
                            assertThat(result.getVersionLabel(), is("~~PWC~~"));
                            assertThat(result.getXAncestorId(), is(e1.getXid()));
                            assertThat(result.getXAncestorRootId(), is(e1.getXid()));
                            assertThat(result.getXSuccessorId(), is(nullValue()));
                        }
                    }
                }

                @Nested
                @DisplayName("given the principal is not the lock owner")
                class PrincipalIsNotLockOwner {

                    @BeforeEach
                    void setupAndCallWorkingCopy() {
                        setupSecurityContext("some-principal", true);
                        e1 = repo.lock(e1);
                        setupSecurityContext("some-other-principal", true);
                        try {
                            result = repo.workingCopy(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should throw LockOwnerException")
                    void shouldThrowLockOwnerException() {
                        assertThat(e, is(instanceOf(LockOwnerException.class)));
                    }
                }

                @Nested
                @DisplayName("given the principal is unauthenticated")
                class PrincipalUnauthenticated {

                    @BeforeEach
                    void setupAndCallWorkingCopy() {
                        setupSecurityContext("some-principal", false);
                        try {
                            result = repo.workingCopy(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should throw a SecurityException")
                    void shouldThrowSecurityException() {
                        assertThat(e, is(instanceOf(SecurityException.class)));
                        assertThat(e.getMessage(), containsString("no principal"));
                    }
                }

                @Nested
                @DisplayName("given the entity is not the current version")
                class NotCurrentVersion {

                    @BeforeEach
                    void setupAndCallWorkingCopy() {
                        setupSecurityContext("some-principal", true);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        try {
                            result = repo.workingCopy(e1);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should throw an exception")
                    void shouldThrowException() {
                        assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                        assertThat(e.getMessage(), containsString("not head"));
                    }
                }
            }
        }

        @Nested
        @DisplayName("#isPrivateWorkingCopy")
        class IsPrivateWorkingCopy {

            @BeforeEach
            void setupSecurity() {
                setupSecurityContext("some-principal", true);
            }

            @Test
            @DisplayName("should return false")
            void shouldReturnFalse() {
                assertThat(repo.isPrivateWorkingCopy(e1), is(false));
            }

            @Test
            @DisplayName("should return true")
            void shouldReturnTrue() {
                e1 = repo.save(e1);
                e1 = repo.lock(e1);
                TestEntity wc = repo.workingCopy(e1);
                assertThat(repo.isPrivateWorkingCopy(wc), is(true));
            }
        }

        @Nested
        @DisplayName("#findWorkingCopy")
        class FindWorkingCopy {

            @BeforeEach
            void setupSecurity() {
                setupSecurityContext("some-principal", true);
            }

            @Test
            @DisplayName("should return the working copy")
            void shouldReturnWorkingCopy() {
                e1 = repo.save(e1);
                e1 = repo.lock(e1);
                TestEntity wc = repo.workingCopy(e1);
                assertThat(repo.findWorkingCopy(wc), hasProperty("xid", is(wc.getXid())));
            }
        }

        @Nested
        @DisplayName("#delete")
        class Delete {

            @Nested
            @DisplayName("given no principal")
            class NoPrincipal {

                @BeforeEach
                void setupAndDelete() {
                    setupSecurityContext(null, false);
                    entityForDeletion = e1;
                    try {
                        repo.delete(entityForDeletion);
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
            @DisplayName("given an unauthenticated principal")
            class UnauthenticatedPrincipal {

                @BeforeEach
                void setupAndDelete() {
                    setupSecurityContext("some-principal", false);
                    entityForDeletion = e1;
                    try {
                        repo.delete(entityForDeletion);
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
            @DisplayName("given a principal")
            class WithPrincipal {

                @BeforeEach
                void setupSecurity() {
                    setupSecurityContext("some-principal", true);
                }

                @Nested
                @DisplayName("given the entity is not in a version tree")
                class NotInVersionTree {

                    @Nested
                    @DisplayName("given the principal is the lock owner")
                    class PrincipalIsLockOwner {

                        @BeforeEach
                        void setupAndDelete() {
                            e1 = repo.save(e1);
                            e1 = repo.lock(e1);
                            entityForDeletion = e1;
                            try {
                                repo.delete(entityForDeletion);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should be deleted")
                        void shouldBeDeleted() {
                            assertThat(e, is(nullValue()));
                            assertThat(repo.findById(e1.getXid()), is(Optional.empty()));
                        }
                    }

                    @Nested
                    @DisplayName("given the principal is not the lock owner")
                    class PrincipalIsNotLockOwner {

                        @BeforeEach
                        void setupAndDelete() {
                            e1 = repo.save(e1);
                            e1 = repo.lock(e1);
                            entityForDeletion = e1;
                            setupSecurityContext("some-other-principal", true);
                            try {
                                repo.delete(entityForDeletion);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should fail to delete the entity")
                        void shouldFailToDelete() {
                            assertThat(e, is(instanceOf(LockOwnerException.class)));
                            assertThat(e.getMessage(), containsString("not lock owner"));
                        }
                    }

                    @Nested
                    @DisplayName("given there is no lock")
                    class NoLock {

                        @BeforeEach
                        void setupAndDelete() {
                            e1 = repo.save(e1);
                            entityForDeletion = e1;
                            try {
                                repo.delete(entityForDeletion);
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should be deleted")
                        void shouldBeDeleted() {
                            assertThat(e, is(nullValue()));
                            assertThat(repo.findById(e1.getXid()), is(Optional.empty()));
                        }
                    }
                }

                @Nested
                @DisplayName("given the entity is not the head")
                class NotHead {

                    @BeforeEach
                    void setupAndDelete() {
                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        entityForDeletion = e1;
                        try {
                            repo.delete(entityForDeletion);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should fail")
                    void shouldFail() {
                        assertThat(e, is(instanceOf(LockingAndVersioningException.class)));
                        assertThat(e.getMessage(), containsString("not head"));
                    }
                }

                @Nested
                @DisplayName("given the entity is the head of a version series of 3 versions and the ancestor is not ancestral root")
                class VersionSeriesOfThree {

                    @BeforeEach
                    void setupAndDelete() {
                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        e1v12 = repo.version(e1v11, new VersionInfo("1.2", "Minor"));
                        entityForDeletion = e1v12;
                        try {
                            repo.delete(entityForDeletion);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should delete the entity")
                    void shouldDeleteEntity() {
                        assertThat(repo.findById(e1v12.getXid()), is(Optional.empty()));
                    }

                    @Test
                    @DisplayName("should re-instate the ancestor as the head")
                    void shouldReinstateAncestorAsHead() {
                        e1v11 = repo.findById(e1v11.getXid()).get();
                        assertThat(e1v11.getXSuccessorId(), is(nullValue()));
                    }

                    @Test
                    @DisplayName("should remove the lock")
                    void shouldRemoveLock() {
                        assertThat(lockingService.lockOwner(e1v12.getXid()), is(nullValue()));
                    }

                    @Test
                    @DisplayName("should re-instate the lock on the new head")
                    void shouldReinstateLockOnNewHead() {
                        assertThat(lockingService.lockOwner(e1v11.getXid()), is(not(nullValue())));
                    }
                }

                @Nested
                @DisplayName("given the entity is the head of a version tree of 2 versions (ancestor is ancestral root)")
                class VersionTreeOfTwo {

                    @BeforeEach
                    void setupAndDelete() {
                        e1 = repo.save(e1);
                        e1 = repo.lock(e1);
                        e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                        entityForDeletion = e1v11;
                        try {
                            repo.delete(entityForDeletion);
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should delete the entity")
                    void shouldDeleteEntity() {
                        assertThat(e, is(nullValue()));
                        assertThat(repo.findById(e1v11.getXid()), is(Optional.empty()));
                    }

                    @Test
                    @DisplayName("should re-instate the ancestor as the head")
                    void shouldReinstateAncestorAsHead() {
                        e1 = repo.findById(e1.getXid()).get();
                        assertThat(e1.getXSuccessorId(), is(nullValue()));
                    }
                }
            }
        }

        @Nested
        @DisplayName("#deleteAllVersions")
        class DeleteAllVersions {

            @BeforeEach
            void setup() {
                setupSecurityContext("some-principal", true);

                e1 = repo.save(e1);
                e1 = repo.lock(e1);
                e1v11 = repo.version(e1, new VersionInfo("1.1", "Minor"));
                e1v11 = repo.unlock(e1v11);
            }

            @Nested
            @DisplayName("given no principal")
            class NoPrincipal {

                @BeforeEach
                void setupSecurity() {
                    setupSecurityContext(null, false);
                }

                @Test
                @DisplayName("should throw a SecurityException")
                void shouldThrowSecurityException() {
                    assertThrows(SecurityException.class, () -> repo.deleteAllVersions(e1v11));
                }
            }

            @Nested
            @DisplayName("given an unauthenticated principal")
            class UnauthenticatedPrincipal {

                @BeforeEach
                void setupSecurity() {
                    setupSecurityContext("some-principal", false);
                }

                @Test
                @DisplayName("should throw a SecurityException")
                void shouldThrowSecurityException() {
                    assertThrows(SecurityException.class, () -> repo.deleteAllVersions(e1v11));
                }
            }

            @Nested
            @DisplayName("given a principal")
            class WithPrincipal {

                @BeforeEach
                void setupSecurity() {
                    setupSecurityContext("some-principal", true);
                }

                @Nested
                @DisplayName("given the entity is not in a version tree")
                class NotInVersionTree {

                    @Nested
                    @DisplayName("given the principal is the lock owner")
                    class PrincipalIsLockOwner {

                        @Test
                        @DisplayName("should delete version series")
                        void shouldDeleteVersionSeries() {
                            e1v11 = repo.lock(e1v11);

                            List<Long> ids = new ArrayList<>();
                            repo.findAllVersions(e1v11).forEach((doc) -> {
                                ids.add(doc.getXid());
                            });

                            repo.deleteAllVersions(e1v11);

                            ids.forEach((id) -> {
                                assertThat(repo.existsById(id), is(false));
                            });
                        }
                    }

                    @Nested
                    @DisplayName("given the principal is not the lock owner")
                    class PrincipalIsNotLockOwner {

                        @BeforeEach
                        void setupSecurity() {
                            e1v11 = repo.lock(e1v11);
                            setupSecurityContext("some-other-principal", true);
                        }

                        @Test
                        @DisplayName("should fail to delete the entity")
                        void shouldFailToDelete() {
                            LockOwnerException ex = assertThrows(LockOwnerException.class, () -> repo.deleteAllVersions(e1v11));
                            assertThat(ex.getMessage(), containsString("not lock owner"));
                        }
                    }

                    @Nested
                    @DisplayName("given there is no lock")
                    class NoLock {

                        @Test
                        @DisplayName("should delete version series")
                        void shouldDeleteVersionSeries() {
                            List<Long> ids = new ArrayList<>();
                            repo.findAllVersions(e1).forEach((doc) -> {
                                ids.add(doc.getXid());
                            });

                            repo.deleteAllVersions(e1v11);

                            ids.forEach((id) -> {
                                assertThat(repo.existsById(id), is(false));
                            });
                        }
                    }
                }

                @Nested
                @DisplayName("given the entity is not the head")
                class NotHead {

                    @Test
                    @DisplayName("should fail")
                    void shouldFail() {
                        LockingAndVersioningException ex = assertThrows(LockingAndVersioningException.class, () -> repo.deleteAllVersions(e1));
                        assertThat(ex.getMessage(), containsString("not head"));
                    }
                }
            }
        }

        @Nested
        @DisplayName("Issue #2039")
        class Issue2039 {

            @BeforeEach
            void setup() {
                setupSecurityContext("some-principal", true);

                e1 = repo.save(new TestEntity());
                e2 = repo.save(new TestEntity());
                e3 = repo.save(new TestEntity());
            }

            @Test
            @DisplayName("should return the provided entity")
            void shouldReturnProvidedEntity() {
                List<TestEntity> results = repo.findAllVersions(e1, Sort.by(Order.desc("id")));
                assertThat(results.size(), is(1));
            }

            @Test
            @DisplayName("should delete just the provided entity")
            void shouldDeleteJustTheProvidedEntity() {
                repo.deleteAllVersions(e1);

                Optional<TestEntity> fetched = repo.findById(e1.getXid());
                assertThat(fetched.isPresent(), is(false));
            }
        }
    }

    @Configuration
    @EnableJpaRepositories(considerNestedRepositories = true)
    @Import({H2Config.class, JpaLockingAndVersioningConfig.class})
    public static class TestConfig {
    }

    @Configuration
    @EnableTransactionManagement
    public static class H2Config {

        @Bean
        public DataSource dataSource() {
            EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
            return builder.setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean
        public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
            HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
            vendorAdapter.setDatabase(Database.H2);
            vendorAdapter.setGenerateDdl(true);

            LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
            factory.setJpaVendorAdapter(vendorAdapter);
            factory.setPackagesToScan(getClass().getPackage().getName());
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {
            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }

        @Value("/org/springframework/versions/jpa/schema-drop-h2.sql")
        private ClassPathResource dropVersionSchema;

        @Value("/org/springframework/versions/jpa/schema-h2.sql")
        private ClassPathResource createVersionSchema;

        @Bean
        public DataSourceInitializer datasourceInitializer() {
            ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();

            databasePopulator.addScript(dropVersionSchema);
            databasePopulator.addScript(createVersionSchema);
            databasePopulator.setIgnoreFailedDrops(true);

            DataSourceInitializer initializer = new DataSourceInitializer();
            initializer.setDataSource(dataSource());
            initializer.setDatabasePopulator(databasePopulator);

            return initializer;
        }
    }

    @Getter
    @Setter
    @Entity
    public static class TestEntity {
        @Id
        @GeneratedValue
        private Long xid;
        @Version
        private Long version;
        @AncestorId
        private Long xAncestorId;
        @AncestorRootId
        private Long xAncestorRootId;
        @SuccessorId
        private Long xSuccessorId;
        @LockOwner
        private String xLockOwner;
        @VersionNumber
        private String versionNo;
        @VersionLabel
        private String versionLabel;

        public TestEntity() {}

        public TestEntity(TestEntity entity) {}
    }

    public interface TestRepository extends JpaRepository<TestEntity, Long>, LockingAndVersioningRepository<TestEntity, Long> {}

    @Getter
    @Setter
    @Entity
    public static class OtherTestEntity {
        @Id
        @GeneratedValue
        private Long id;
    }

    public interface OtherTestRepository extends JpaRepository<OtherTestEntity, Long>, LockingAndVersioningRepository<OtherTestEntity, Long> {}

    private static void setupSecurityContext(String principal, boolean isAuthenticated) {
        SecurityContext sc = new SecurityContext() {
            @Override
            public Authentication getAuthentication() {
                return new MockAuthentication(principal, isAuthenticated);
            }

            @Override
            public void setAuthentication(Authentication authentication) {}
        };

        SecurityContextHolder.setContext(sc);
    }

    private static class MockAuthentication implements Authentication {

        private final String principal;
        private final boolean isAuthenticated;

        public MockAuthentication(String principal, boolean isAuthenticated) {
            this.principal = principal;
            this.isAuthenticated = isAuthenticated;
        }

        @Override
        public String getName() {
            return principal;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public Object getCredentials() {
            return null;
        }

        @Override
        public Object getDetails() {
            return null;
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }

        @Override
        public boolean isAuthenticated() {
            return isAuthenticated;
        }

        @Override
        public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {}
    }
}
