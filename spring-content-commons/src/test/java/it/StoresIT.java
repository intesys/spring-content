package it;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.commons.repository.Store;
import internal.org.springframework.content.commons.store.factory.StoreFactory;
import org.springframework.content.commons.repository.factory.testsupport.TestContentStore;
import org.springframework.content.commons.repository.factory.testsupport.TestStoreFactoryBean;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.StoreAccessException;
import org.springframework.content.commons.store.StoreExceptionTranslator;
import org.springframework.content.commons.storeservice.StoreFilter;
import org.springframework.content.commons.storeservice.StoreInfo;
import org.springframework.content.commons.storeservice.StoreResolver;
import org.springframework.context.support.GenericApplicationContext;

import internal.org.springframework.content.commons.storeservice.StoresImpl;

@DisplayName("StoresIT")
public class StoresIT {

    private StoresImpl stores;

    private GenericApplicationContext context;
    private List<StoreFactory> factories = new ArrayList<>();

    @Nested
    @DisplayName("#getStore")
    class GetStore {

        @Nested
        @DisplayName("when there are two stores that the filter matches but no store resolver")
        class NoResolver {
            @BeforeEach
            void setUp() throws Exception {
                factories.clear();
                TestStoreFactoryBean factory1 = new TestStoreFactoryBean(WrongStore.class);
                factory1.setBeanClassLoader(this.getClass().getClassLoader());
                factories.add(factory1);

                TestStoreFactoryBean factory2 = new TestStoreFactoryBean(RightStore.class);
                factory2.setBeanClassLoader(this.getClass().getClassLoader());
                factories.add(factory2);

                context = new GenericApplicationContext();
                context.registerBean("factory1", StoreFactory.class, () -> factory1);
                context.registerBean("factory2", StoreFactory.class, () -> factory2);
                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();
            }

            @Test
            @DisplayName("should throw an exception")
            void throwException() {
                try {
                    stores.getStore(Store.class, new StoreFilter() {
                        @Override
                        public String name() {
                            return "test";
                        }
                        @Override
                        public boolean matches(StoreInfo info) {
                            return true;
                        }
                    });
                    fail("exception not thrown");
                } catch (Exception e) {
                    assertThat(e.getMessage(), containsString("unable to resolve store"));
                }
            }
        }

        @Nested
        @DisplayName("when there are two stores that the filter matches and a store resolver")
        class WithResolver {
            @BeforeEach
            void setUp() throws Exception {
                factories.clear();
                TestStoreFactoryBean factory1 = new TestStoreFactoryBean(WrongStore.class);
                factory1.setBeanClassLoader(this.getClass().getClassLoader());
                factories.add(factory1);

                TestStoreFactoryBean factory2 = new TestStoreFactoryBean(RightStore.class);
                factory2.setBeanClassLoader(this.getClass().getClassLoader());
                factories.add(factory2);

                context = new GenericApplicationContext();
                context.registerBean("factory1", StoreFactory.class, () -> factory1);
                context.registerBean("factory2", StoreFactory.class, () -> factory2);
                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();

                stores.addStoreResolver("test", new StoreResolver() {
                    @Override
                    public StoreInfo resolve(StoreInfo... stores) {
                        for (StoreInfo info : stores) {
                            if (info.getInterface().equals(RightStore.class)) {
                                return info;
                            }
                        }
                        return null;
                    }
                });
            }

            @Test
            @DisplayName("should return the right store")
            void returnRightStore() {
                StoreInfo info = stores.getStore(Store.class, new StoreFilter() {
                    @Override
                    public String name() {
                        return "test";
                    }
                    @Override
                    public boolean matches(StoreInfo info) {
                        return true;
                    }
                });
                assertThat(info.getInterface(), is(RightStore.class));
            }
        }
    }

    @Nested
    @DisplayName("StoreExceptionTranslatorInterceptor")
    class StoreExceptionTranslatorInterceptor {
        @BeforeEach
        void setUp() {
            factories.clear();
            TestStoreFactoryBean factory = new TestStoreFactoryBean(RuntimeExceptionThrowingStore.class);
            factory.setBeanClassLoader(this.getClass().getClassLoader());
            factories.add(factory);

            context = new GenericApplicationContext();
            context.registerBean("factory", StoreFactory.class, () -> factory);
        }

        @Nested
        @DisplayName("given there is no store exception translator registered")
        class NoTranslator {
            @BeforeEach
            void setUp() throws Exception {
                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();
            }
            @Test
            @DisplayName("should re-throw RuntimeException as UnsupportedOperationException")
            void throwRuntimeException() {
                StoreInfo storeInfo = stores.getStore(Store.class, new StoreFilter() {
                    @Override
                    public String name() {
                        return "test";
                    }
                    @Override
                    public boolean matches(StoreInfo info) {
                        return true;
                    }
                });
                ContentStore store = storeInfo.getImplementation(ContentStore.class);
                try {
                    store.setContent(new Object(), new ByteArrayInputStream("".getBytes()));
                } catch (Exception e) {
                    assertThat(e, isA(UnsupportedOperationException.class));
                }
            }
        }

        @Nested
        @DisplayName("given there is a store exception translator registered")
        class WithTranslator {
            @BeforeEach
            void setUp() throws Exception {
                context.registerBean("translator", StoreExceptionTranslator.class, () -> new StoreExceptionTranslator() {
                    @Override
                    public StoreAccessException translate(RuntimeException re) {
                        return new StoreAccessException(re.getMessage(), re);
                    }
                });
                context.refresh();
                stores = new StoresImpl(context);
                stores.afterPropertiesSet();
            }
            @Test
            @DisplayName("should re-throw RuntimeException as StoreAccessException")
            void throwStoreAccessException() {
                StoreInfo storeInfo = stores.getStore(Store.class, new StoreFilter() {
                    @Override
                    public String name() {
                        return "test";
                    }
                    @Override
                    public boolean matches(StoreInfo info) {
                        return true;
                    }
                });
                ContentStore store = storeInfo.getImplementation(ContentStore.class);
                try {
                    store.setContent(new Object(), new ByteArrayInputStream("".getBytes()));
                } catch (Exception e) {
                    assertThat(e, isA(StoreAccessException.class));
                }
            }
        }
    }

    public interface RightStore extends TestContentStore<Object, Serializable>{};
    public interface WrongStore extends TestContentStore<Object, Serializable>{};
    public interface RuntimeExceptionThrowingStore extends TestContentStore<Object, Serializable>{};
}
