package internal.org.springframework.content.jpa;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.testsupport.models.Document;
import internal.org.springframework.content.jpa.testsupport.repositories.DocumentRepository;
import internal.org.springframework.content.jpa.testsupport.stores.DocumentAssociativeStore;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

class AssociativeStoreIT {

    private static final Class<?>[] CONFIG_CLASSES = new Class<?>[] {
        H2Config.class, HSQLConfig.class, MySqlConfig.class, PostgresConfig.class
        // SqlServerConfig.class,
        // StoreIT.OracleConfig.class
    };

    private AnnotationConfigApplicationContext context;
    private DocumentRepository repo;
    private DocumentAssociativeStore store;
    private PlatformTransactionManager txn;

    private Document document;
    private Resource resource;
    private String resourceId;
    private Exception e;

    @TestFactory
    Stream<DynamicContainer> associativeStoreAcrossDatabases() {

        return Arrays.stream(CONFIG_CLASSES).map(configClass -> DynamicContainer.dynamicContainer(getContextName(configClass), Stream.of(
            DynamicTest.dynamicTest("full scenario", () -> runScenario(configClass)))));
    }

    private void runScenario(Class<?> configClass)
        throws Exception {

        // ---- context setup ----
        context = new AnnotationConfigApplicationContext();
        context.register(StoreIT.TestConfig.class);
        context.register(configClass);
        context.refresh();

        repo = context.getBean(DocumentRepository.class);
        store = context.getBean(DocumentAssociativeStore.class);
        txn = context.getBean(PlatformTransactionManager.class);

        // ==========================================================
        // given a new entity
        // ==========================================================
        document = new Document();
        document = repo.save(document);

        assertThat(document.getContentId(), is(nullValue()));
        assertThat(store.getResource(document), is(nullValue()));

        // ==========================================================
        // given a resource
        // ==========================================================
        resourceId = UUID.randomUUID().toString();
        resource = store.getResource(resourceId);

        // ==========================================================
        // when resource is associated
        // ==========================================================
        store.associate(document, resourceId);
        store.associate(document, PropertyPath.from("rendition"), resourceId);

        assertThat(document.getContentId(), is(resourceId));
        assertThat(document.getRenditionId(), is(resourceId));

        // ==========================================================
        // when resource has content
        // ==========================================================
        TransactionStatus status = txn.getTransaction(new DefaultTransactionDefinition());

        store.getResource(document, PropertyPath.from("content"), GetResourceParams.builder().build());

        try (OutputStream os = ((WritableResource) resource).getOutputStream()) {
            os.write("Hello Client-side World!".getBytes());
        }

        txn.commit(status);

        Resource r = store.getResource(document, PropertyPath.from("content"), GetResourceParams.builder().range("5-10").build());

        try (InputStream is = r.getInputStream()) {
            assertThat(IOUtils.toString(is), is("Hello Client-side World!"));
        }

        // ==========================================================
        // when resource is unassociated
        // ==========================================================
        store.unassociate(document);
        store.unassociate(document, PropertyPath.from("rendition"));

        assertThat(document.getContentId(), is(nullValue()));
        assertThat(document.getRenditionId(), is(nullValue()));

        // ==========================================================
        // invalid property path tests
        // ==========================================================
        assertThrowsStoreAccess(() -> store.associate(document, PropertyPath.from("does.not.exist"), resourceId));

        assertThrowsStoreAccess(() -> store.getResource(document, PropertyPath.from("does.not.exist")));

        assertThrowsStoreAccess(() -> store.unassociate(document, PropertyPath.from("does.not.exist")));

        context.close();
    }

    private void assertThrowsStoreAccess(Runnable r) {

        try {
            r.run();
        }
        catch (Exception ex) {
            e = ex;
        }
        assertThat(e, is(instanceOf(StoreAccessException.class)));
        e = null;
    }
}
