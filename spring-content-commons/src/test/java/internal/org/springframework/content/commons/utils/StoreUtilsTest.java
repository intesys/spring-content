package internal.org.springframework.content.commons.utils;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.ResourceLoader;

@DisplayName("StoreUtils")
public class StoreUtilsTest {

    private StoreCandidateComponentProvider scanner;
    private Environment env;
    private ResourceLoader loader;
    private String[] basePackages;
    private boolean multiStoreMode;
    private Class<?>[] identifyingTypes;
    private String registrarId;

    private GenericBeanDefinition def1;
    private GenericBeanDefinition def2;

    @BeforeEach
    void setUp() {
        env = mock(Environment.class);
        loader = new FileSystemResourceLoader();
        ((FileSystemResourceLoader)loader).setClassLoader(this.getClass().getClassLoader());
        basePackages = new String[] {StoreUtilsTest.class.getPackage().getName()};
        registrarId = "test";
    }

    @Nested
    @DisplayName("#getStoreCandidates")
    class GetStoreCandidates {

        @Nested
        @DisplayName("when multiple storage beans are found")
        class MultipleBeansFound {
            @BeforeEach
            void setUp() {
                def1 = new GenericBeanDefinition();
                def1.setBeanClassName(StoreBean1.class.getName());
                def2 = new GenericBeanDefinition();
                def2.setBeanClassName(StoreBean2.class.getName());

                Set<BeanDefinition> defs = new HashSet<>();
                defs.add(def1);
                defs.add(def2);

                scanner = mock(StoreCandidateComponentProvider.class);
                when(scanner.findCandidateComponents(org.mockito.ArgumentMatchers.any())).thenReturn(defs);

                identifyingTypes = new Class<?>[] {StoreUtilsTest.StoreType1.class };
            }

            @Nested
            @DisplayName("when multiple storage modules are found on the classpath")
            class MultipleModulesFound {
                @BeforeEach
                void setUp() {
                    multiStoreMode = true;
                }

                @Test
                @DisplayName("should return the store matching the identifying type")
                void returnMatchingStore() {
                    Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, registrarId);
                    assertThat(beans, hasItem(def1));
                    assertThat(beans, not(hasItem(def2)));
                }

                @Nested
                @DisplayName("when the store beans cant be matched against signature types but the registrar matches the default storage type property")
                class RegistrarMatchesDefault {
                    @BeforeEach
                    void setUp() {
                        multiStoreMode = true;
                        identifyingTypes = new Class<?>[] {};
                        when(env.getProperty("spring.content.storage.type.default")).thenReturn("test");
                    }

                    @Test
                    @DisplayName("should return all stores")
                    void returnAllStores() {
                        Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, registrarId);
                        assertThat(beans, hasItem(def1));
                        assertThat(beans, hasItem(def2));
                    }
                }

                @Nested
                @DisplayName("when the store beans cant be matched against signature types and the registrar doesn't match the default storage type property")
                class RegistrarDoesNotMatchDefault {
                    @BeforeEach
                    void setUp() {
                        multiStoreMode = true;
                        identifyingTypes = new Class<?>[] {};
                        registrarId = "other-id";
                        when(env.getProperty("spring.content.storage.type.default")).thenReturn("test");
                    }

                    @Test
                    @DisplayName("shouldn't return any stores")
                    void returnNoStores() {
                        Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, registrarId);
                        assertThat(beans, not(hasItem(def1)));
                        assertThat(beans, not(hasItem(def2)));
                    }
                }
            }

            @Nested
            @DisplayName("when multi-mode is false")
            class MultiModeFalse {
                @BeforeEach
                void setUp() {
                    multiStoreMode = false;
                }

                @Test
                @DisplayName("should return all stores")
                void returnAllStores() {
                    Set<GenericBeanDefinition> beans = StoreUtils.getStoreCandidates(scanner, env, loader, basePackages, multiStoreMode, identifyingTypes, "test");
                    assertThat(beans, hasItem(def1));
                    assertThat(beans, hasItem(def2));
                }
            }
        }
    }

    interface StoreType1 {
    }

    interface StoreType2 {
    }

    interface StoreBean1 extends StoreType1 {}
    interface StoreBean2 extends StoreType2 {}
}
