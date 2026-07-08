package internal.org.springframework.content.jpa;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.HSQLConfig;
import internal.org.springframework.content.jpa.StoreIT.MySqlConfig;
import internal.org.springframework.content.jpa.StoreIT.PostgresConfig;
import internal.org.springframework.content.jpa.StoreIT.TestConfig;
import internal.org.springframework.content.jpa.testsupport.models.Claim;
import internal.org.springframework.content.jpa.testsupport.models.ClaimForm;
import internal.org.springframework.content.jpa.testsupport.repositories.ClaimRepository;
import internal.org.springframework.content.jpa.testsupport.stores.ClaimStore;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.Mockito;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ContentStoreIT {

    private static final Class<?>[] CONFIG_CLASSES = new Class[] {
        H2Config.class, HSQLConfig.class, MySqlConfig.class, PostgresConfig.class
    };

    private AnnotationConfigApplicationContext context;

    private PlatformTransactionManager ptm;
    private ClaimRepository claimRepo;
    private ClaimStore claimFormStore;

    private EmbeddedRepository embeddedRepo;
    private EmbeddedStore embeddedStore;

    private Claim claim;
    private Object id;

    // ==========================================================
    // MULTI DB ENTRY POINT
    // ==========================================================
    @TestFactory
    Stream<DynamicContainer> contentStoreAcrossDatabases() {

        return Stream.of(CONFIG_CLASSES)
                     .map(cfg -> DynamicContainer.dynamicContainer(getContextName(cfg), Stream.of(DynamicTest.dynamicTest("full suite", () -> runSuite(cfg)))));
    }

    // ==========================================================
    // CORE LOGIC
    // ==========================================================
    void runSuite(Class<?> configClass)
        throws Exception {

        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.register(configClass);
        context.refresh();

        ptm = context.getBean(PlatformTransactionManager.class);
        claimRepo = context.getBean(ClaimRepository.class);
        claimFormStore = context.getBean(ClaimStore.class);

        embeddedRepo = context.getBean(EmbeddedRepository.class);
        embeddedStore = context.getBean(EmbeddedStore.class);

        if (ptm == null) {
            ptm = Mockito.mock(PlatformTransactionManager.class);
        }

        try {
            runGivenEntityWithContent();
            runUpdateScenarios();
            runDeletionScenarios();
            runEmbeddedScenarios();
        }
        finally {
            deleteAllClaimFormsContent();
            deleteAllClaims();
            context.close();
        }
    }

    // ==========================================================
    // GIVEN ENTITY WITH CONTENT
    // ==========================================================
    void runGivenEntityWithContent()
        throws Exception {

        claim = new Claim();
        claim.setFirstName("John");
        claim.setLastName("Smith");
        claim.setClaimForm(new ClaimForm());
        claim = claimRepo.save(claim);

        claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

        claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"),
                                  new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));

        // ---- store new content ----
        doInTransaction(ptm, () -> {
            try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {

                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));

            }
            catch (IOException ignored) {
            }
            return null;
        });

        doInTransaction(ptm, () -> {
            try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {

                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));

            }
            catch (IOException ignored) {
            }
            return null;
        });

        // ---- metadata ----
        assertThat(claim.getClaimForm().getContentId(), notNullValue());
        assertThat(claim.getClaimForm().getContentLength(), is(27L));

        assertThat(claim.getClaimForm().getRenditionId(), notNullValue());
        assertThat(claim.getClaimForm().getRenditionLen(), is(40L));
    }

    // ==========================================================
    // UPDATE SCENARIOS
    // ==========================================================
    void runUpdateScenarios()
        throws Exception {

        claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));

        claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"),
                                  new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));

        claim = claimRepo.save(claim);

        doInTransaction(ptm, () -> {
            try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {

                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content), is(true));

            }
            catch (IOException ignored) {
            }
            return null;
        });

        doInTransaction(ptm, () -> {
            try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {

                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content), is(true));

            }
            catch (IOException ignored) {
            }
            return null;
        });
    }

    // ==========================================================
    // DELETE SCENARIOS
    // ==========================================================
    void runDeletionScenarios()
        throws Exception {

        id = claim.getClaimForm().getContentId();

        claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"));
        claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/rendition"));

        claim = claimRepo.save(claim);

        doInTransaction(ptm, () -> {
            try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {

                assertThat(content, nullValue());

            }
            catch (IOException ignored) {
            }
            return null;
        });

        assertThat(claim.getClaimForm().getContentId(), nullValue());
        assertThat(claim.getClaimForm().getContentLength(), nullValue());

        assertThat(claim.getClaimForm().getRenditionId(), nullValue());
        assertThat(claim.getClaimForm().getRenditionLen(), is(0L));
    }

    // ==========================================================
    // EMBEDDED SCENARIOS
    // ==========================================================
    void runEmbeddedScenarios()
        throws Exception {

        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());

        assertThat(embeddedStore.getContent(entity, PropertyPath.from("content")), nullValue());

        embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));

        try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {

            assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes())), is(true));
        }

        EntityWithEmbeddedContent expected = new EntityWithEmbeddedContent(entity.getId(), entity.getContent());

        assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content")), is(expected));
    }

    // ==========================================================
    // TRANSACTION HELPER
    // ==========================================================
    public static <T> T doInTransaction(PlatformTransactionManager ptm, Supplier<T> block) {

        TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());
        try {
            T result = block.get();
            ptm.commit(status);
            return result;
        }
        catch (Exception e) {
            ptm.rollback(status);
            return null;
        }
    }

    protected void deleteAllClaims() {

        claimRepo.deleteAll();
    }

    protected void deleteAllClaimFormsContent() {

        claimRepo.findAll().forEach(c -> {
            if (c.getClaimForm() != null) {
                claimFormStore.unsetContent(c, PropertyPath.from("claimForm/content"));
                claimFormStore.unsetContent(c, PropertyPath.from("claimForm/rendition"));
            }
        });
    }

    // ==========================================================
    // EMBEDDED MODEL
    // ==========================================================
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Entity
    @Table(name = "entity_with_embedded")
    public static class EntityWithEmbeddedContent {

        @Id
        private String id = UUID.randomUUID().toString();

        @Embedded
        private EmbeddedContent content;
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    public static class EmbeddedContent {

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {

    }

    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {

    }
}
