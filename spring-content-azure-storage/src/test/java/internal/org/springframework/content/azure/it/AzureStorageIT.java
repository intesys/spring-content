package internal.org.springframework.content.azure.it;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import jakarta.persistence.*;
import lombok.*;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.content.azure.config.EnableAzureStorage;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.GetResourceParams;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("DefaultAzureStorageImpl")
public class AzureStorageIT {

    private static final BlobServiceClientBuilder builder = Azurite.getBlobServiceClientBuilder();
    private static final BlobContainerClient client = builder.buildClient().getBlobContainerClient("test");

    static {
        if (!client.exists()) {
            client.create();
        }

        System.setProperty("spring.content.azure.bucket", "azure-test-bucket");
    }

    private TestEntity entity;
    private Resource genericResource;

    private Exception e;

    private AnnotationConfigApplicationContext context;

    private TestEntityRepository repo;
    private TestEntityStore store;

    private EmbeddedRepository embeddedRepo;
    private EmbeddedStore embeddedStore;

    private String resourceLocation;

    @BeforeEach
    public void setUp() {
        context = new AnnotationConfigApplicationContext();
        context.register(TestConfig.class);
        context.refresh();

        repo = context.getBean(TestEntityRepository.class);
        store = context.getBean(TestEntityStore.class);

        embeddedRepo = context.getBean(EmbeddedRepository.class);
        embeddedStore = context.getBean(EmbeddedStore.class);

        RandomString random  = new RandomString(5);
        resourceLocation = random.nextString();
    }

    @AfterEach
    public void tearDown() {
        context.close();
    }

    @Nested
    @DisplayName("Store")
    class StoreTests {

        @Nested
        @DisplayName("#getResource")
        class GetResource {

            @BeforeEach
            public void setUp() {
                genericResource = store.getResource(resourceLocation);
            }

            @AfterEach
            public void tearDown()
                throws IOException {
                if (genericResource != null) {
                    ((DeletableResource)genericResource).delete();
                }

                PagedIterable<BlobItem> blobs = client.listBlobs();
                for(BlobItem blob : blobs) {
                    client.getBlobClient(blob.getName()).delete();
                }
            }

            @Test
            @DisplayName("should get Resource")
            public void shouldGetResource() {
                assertThat(genericResource, is(instanceOf(Resource.class)));
            }

            @Test
            @DisplayName("should not exist")
            public void shouldNotExist() {
                assertThat(genericResource.exists(), is(false));
            }

            @Nested
            @DisplayName("given content is added to that resource")
            class GivenContentIsAddedToThatResource {

                @BeforeEach
                public void setUp()
                    throws IOException {
                    try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                        try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                            IOUtils.copy(is, os);
                        }
                    }
                }

                @Test
                @DisplayName("should store that content")
                public void shouldStoreThatContent() throws Exception {
                    assertThat(genericResource.exists(), is(true));

                    boolean matches = false;
                    try (InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                        try (InputStream actual = genericResource.getInputStream()) {
                            matches = IOUtils.contentEquals(expected, actual);
                            assertThat(matches, is(true));
                        }
                    }
                }

                @Nested
                @DisplayName("given that resource is then updated")
                class GivenThatResourceIsThenUpdated {

                    @BeforeEach
                    public void setUp()
                        throws IOException {
                        try (InputStream is = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                            try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                IOUtils.copy(is, os);
                            }
                        }
                    }

                    @Test
                    @DisplayName("should store that updated content")
                    public void shouldStoreThatUpdatedContent() throws Exception {
                        assertThat(genericResource.exists(), is(true));

                        try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                            try (InputStream actual = genericResource.getInputStream()) {
                                assertThat(IOUtils.contentEquals(expected, actual), is(true));
                            }
                        }
                    }
                }

                @Nested
                @DisplayName("given that resource is then deleted")
                class GivenThatResourceIsThenDeleted {

                    @BeforeEach
                    public void setUp() {
                        try {
                            ((DeletableResource) genericResource).delete();
                        } catch (Exception ex) {
                            e = ex;
                        }
                    }

                    @Test
                    @DisplayName("should not exist")
                    public void shouldNotExist() {
                        assertThat(e, is(nullValue()));
                        assertThat(genericResource.exists(), is(false));
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("AssociativeStore")
    class AssociativeStoreTests {

        @Nested
        @DisplayName("given a new entity")
        class GivenANewEntity {

            @BeforeEach
            public void setUp() {
                entity = new TestEntity();
                entity = repo.save(entity);
            }

            @Test
            @DisplayName("should not have an associated resource")
            public void shouldNotHaveAnAssociatedResource() {
                assertThat(entity.getContentId(), is(nullValue()));
                assertThat(store.getResource(entity), is(nullValue()));
            }

            @Nested
            @DisplayName("given a resource")
            class GivenAResource {

                @BeforeEach
                public void setUp() {
                    genericResource = store.getResource(resourceLocation);
                }

                @Nested
                @DisplayName("when the resource is associated")
                class WhenTheResourceIsAssociated {

                    @BeforeEach
                    public void setUp() {
                        store.associate(entity, resourceLocation);
                        store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                    }

                    @Test
                    @DisplayName("should be recorded as such on the entity's @ContentId")
                    public void shouldBeRecordedAsSuchOnTheEntityContentId() {
                        assertThat(entity.getContentId(), is(resourceLocation));
                        assertThat(entity.getRenditionId(), is(resourceLocation));
                    }

                    @Nested
                    @DisplayName("when the resource has content")
                    class WhenTheResourceHasContent {

                        @BeforeEach
                        public void setUp()
                            throws IOException {
                            try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                os.write("Hello Client-side World!".getBytes());
                            }
                        }

                        @Test
                        @DisplayName("should not honor byte ranges")
                        public void shouldNotHonorByteRanges() throws Exception {
                            Resource r = store.getResource(entity, PropertyPath.from("content"), GetResourceParams.builder().range("5-10").build());
                            try (InputStream is = r.getInputStream()) {
                                assertThat(IOUtils.toString(is), is("Hello Client-side World!"));
                            }
                        }
                    }

                    @Nested
                    @DisplayName("when the resource is unassociated")
                    class WhenTheResourceIsUnassociated {

                        @BeforeEach
                        public void setUp() {
                            store.unassociate(entity);
                            store.unassociate(entity, PropertyPath.from("rendition"));
                        }

                        @Test
                        @DisplayName("should reset the entity's @ContentId")
                        public void shouldResetTheEntitysContentId() {
                            assertThat(entity.getContentId(), is(nullValue()));
                            assertThat(entity.getRenditionId(), is(nullValue()));
                        }
                    }

                    @Nested
                    @DisplayName("when a invalid property path is used to associate a resource")
                    class WhenAnInvalidPropertyPathIsUsedToAssociateAResource {

                        @Test
                        @DisplayName("should throw an error")
                        public void shouldThrowAnError() {
                            try {
                                store.associate(entity, PropertyPath.from("does.not.exist"), resourceLocation);
                            } catch (Exception sae) {
                                e = sae;
                            }
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        }
                    }

                    @Nested
                    @DisplayName("when a invalid property path is used to load a resource")
                    class WhenAnInvalidPropertyPathIsUsedToLoadAResource {

                        @Test
                        @DisplayName("should throw an error")
                        public void shouldThrowAnError() {
                            try {
                                store.getResource(entity, PropertyPath.from("does.not.exist"));
                            } catch (Exception sae) {
                                e = sae;
                            }
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        }
                    }

                    @Nested
                    @DisplayName("when a invalid property path is used to unassociate a resource")
                    class WhenAnInvalidPropertyPathIsUsedToUnassociateAResource {

                        @Test
                        @DisplayName("should throw an error")
                        public void shouldThrowAnError() {
                            try {
                                store.unassociate(entity, PropertyPath.from("does.not.exist"));
                            } catch (Exception sae) {
                                e = sae;
                            }
                            assertThat(e, is(instanceOf(StoreAccessException.class)));
                        }
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("ContentStore")
    class ContentStoreTests {

        @BeforeEach
        public void setUp() {
            entity = new TestEntity();
            entity = repo.save(entity);

            store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
            store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
        }

        @Test
        @DisplayName("should be able to store new content")
        public void shouldBeAbleToStoreNewContent() {
            try (InputStream content = store.getContent(entity)) {
                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
            } catch (IOException ioe) {}

            try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));
            } catch (IOException ioe) {}
        }

        @Test
        @DisplayName("should have content metadata")
        public void shouldHaveContentMetadata() {
            assertThat(entity.getContentId(), is(notNullValue()));
            assertThat(entity.getContentId().trim().length(), greaterThan(0));
            assertEquals(Long.valueOf(27L), entity.getContentLen());

            assertThat(entity.getRenditionId(), is(notNullValue()));
            assertThat(entity.getRenditionId().trim().length(), greaterThan(0));
            assertEquals(40L, entity.getRenditionLen());
        }

        @Nested
        @DisplayName("when content is updated")
        class WhenContentIsUpdated {

            @BeforeEach
            public void setUp() {
                store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                entity = repo.save(entity);
            }

            @Test
            @DisplayName("should have the updated content")
            public void shouldHaveTheUpdatedContent() throws Exception {
                boolean matches = false;
                try (InputStream content = store.getContent(entity)) {
                    matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                    assertThat(matches, is(true));
                }

                matches = false;
                try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                    matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                    assertThat(matches, is(true));
                }
            }
        }

        @Nested
        @DisplayName("when content is updated with shorter content")
        class WhenContentIsUpdatedWithShorterContent {

            @BeforeEach
            public void setUp() {
                store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
                entity = repo.save(entity);
            }

            @Test
            @DisplayName("should store only the new content")
            public void shouldStoreOnlyTheNewContent() throws Exception {
                boolean matches = false;
                try (InputStream content = store.getContent(entity)) {
                    matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                    assertThat(matches, is(true));
                }

                matches = false;
                try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                    matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                    assertThat(matches, is(true));
                }
            }
        }

        @Nested
        @DisplayName("when content is updated and not overwritten")
        class WhenContentIsUpdatedAndNotOverwritten {

            @Test
            @DisplayName("should have the updated content")
            public void shouldHaveTheUpdatedContent() throws Exception {
                BlobContainerClient c = builder.buildClient().getBlobContainerClient("azure-test-bucket");

                String contentId = entity.getContentId();
                assertThat(c.getBlobClient(contentId).getBlockBlobClient().exists(), is(true));

                store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().disposition(SetContentParams.ContentDisposition.CreateNew).build());
                entity = repo.save(entity);

                boolean matches = false;
                try (InputStream content = store.getContent(entity)) {
                    matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                    assertThat(matches, is(true));
                }

                assertThat(c.getBlobClient(contentId).getBlockBlobClient().exists(), is(true));

                assertThat(entity.getContentId(), is(not(contentId)));

                assertThat(c.getBlobClient(entity.getContentId()).getBlockBlobClient().exists(), is(true));
            }
        }

        @Nested
        @DisplayName("when content is unset")
        class WhenContentIsUnset {

            @BeforeEach
            public void setUp() {
                resourceLocation = entity.getContentId().toString();
                entity = store.unsetContent(entity);
                entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                entity = repo.save(entity);
            }

            @Test
            @DisplayName("should have no content")
            public void shouldHaveNoContent() throws Exception {
                try (InputStream content = store.getContent(entity)) {
                    assertThat(content, is(nullValue()));
                }

                assertThat(entity.getContentId(), is(nullValue()));
                assertThat(entity.getContentLen(), is(nullValue()));

                BlobContainerClient c = builder.buildClient().getBlobContainerClient("azure-test-bucket");
                assertThat(c.getBlobClient(resourceLocation).getBlockBlobClient().exists(), is(false));

                try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                    assertThat(content, is(nullValue()));
                }

                assertThat(entity.getRenditionId(), is(nullValue()));
                assertEquals(0, entity.getRenditionLen());
            }
        }

        @Nested
        @DisplayName("when content is unset but kept")
        class WhenContentIsUnsetButKept {

            @BeforeEach
            public void setUp() {
                resourceLocation = entity.getContentId().toString();
                entity = store.unsetContent(entity, PropertyPath.from("content"), UnsetContentParams.builder().disposition(UnsetContentParams.Disposition.Keep).build());
                entity = repo.save(entity);
            }

            @Test
            @DisplayName("should have no content")
            public void shouldHaveNoContent() throws Exception {
                try (InputStream content = store.getContent(entity)) {
                    assertThat(content, is(nullValue()));
                }

                assertThat(entity.getContentId(), is(nullValue()));
                assertThat(entity.getContentLen(), is(nullValue()));

                BlobContainerClient c = builder.buildClient().getBlobContainerClient("azure-test-bucket");
                assertThat(c.getBlobClient(resourceLocation).getBlockBlobClient().exists(), is(true));
            }
        }

        @Nested
        @DisplayName("when an invalid property path is used to setContent")
        class WhenAnInvalidPropertyPathIsUsedToSetContent {

            @Test
            @DisplayName("should throw an error")
            public void shouldThrowAnError() {
                try {
                    store.setContent(entity, PropertyPath.from("does.not.exist"), new ByteArrayInputStream("foo".getBytes()));
                } catch (Exception sae) {
                    e = sae;
                }
                assertThat(e, is(instanceOf(StoreAccessException.class)));
            }
        }

        @Nested
        @DisplayName("when an invalid property path is used to getContent")
        class WhenAnInvalidPropertyPathIsUsedToGetContent {

            @Test
            @DisplayName("should throw an error")
            public void shouldThrowAnError() {
                try {
                    store.getContent(entity, PropertyPath.from("does.not.exist"));
                } catch (Exception sae) {
                    e = sae;
                }
                assertThat(e, is(instanceOf(StoreAccessException.class)));
            }
        }

        @Nested
        @DisplayName("when an invalid property path is used to unsetContent")
        class WhenAnInvalidPropertyPathIsUsedToUnsetContent {

            @Test
            @DisplayName("should throw an error")
            public void shouldThrowAnError() {
                try {
                    store.unsetContent(entity, PropertyPath.from("does.not.exist"));
                } catch (Exception sae) {
                    e = sae;
                }
                assertThat(e, is(instanceOf(StoreAccessException.class)));
            }
        }

        @Nested
        @DisplayName("when content is deleted and the content id field is shared with entity id")
        class WhenContentIsDeletedAndTheContentIdFieldIsSharedWithEntityId {

            @Test
            @DisplayName("should not reset the id field")
            public void shouldNotResetTheIdField() {
                SharedIdRepository sharedIdRepository = context.getBean(SharedIdRepository.class);
                SharedIdStore sharedIdStore = context.getBean(SharedIdStore.class);

                SharedIdContentIdEntity sharedIdContentIdEntity = sharedIdRepository.save(new SharedIdContentIdEntity());

                sharedIdContentIdEntity = sharedIdStore.setContent(sharedIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                sharedIdContentIdEntity = sharedIdRepository.save(sharedIdContentIdEntity);
                String id = sharedIdContentIdEntity.getContentId();
                sharedIdContentIdEntity = sharedIdStore.unsetContent(sharedIdContentIdEntity);
                assertThat(sharedIdContentIdEntity.getContentId(), is(id));
                assertThat(sharedIdContentIdEntity.getContentLen(), is(0L));
            }
        }

        @Nested
        @DisplayName("@Embedded content")
        class EmbeddedContentTests {

            @Nested
            @DisplayName("given a entity with a null embedded content object")
            class GivenAEntityWithANullEmbeddedContentObject {

                @Test
                @DisplayName("should return null when content is fetched")
                public void shouldReturnNullWhenContentIsFetched() {
                    EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                    assertThat(embeddedStore.getContent(entity, PropertyPath.from("content")), is(nullValue()));
                }

                @Test
                @DisplayName("should be successful when content is set")
                public void shouldBeSuccessfulWhenContentIsSet() throws Exception {
                    EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                    embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                    try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                        assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes())), is(true));
                    }
                }

                @Test
                @DisplayName("should return null when content is unset")
                public void shouldReturnNullWhenContentIsUnset() {
                    EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                    EntityWithEmbeddedContent expected = new EntityWithEmbeddedContent(entity.getId(), entity.getContent());
                    assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content")), is(expected));
                }
            }
        }
    }

    @Configuration
    @EnableJpaRepositories(basePackages="internal.org.springframework.content.azure.it", considerNestedRepositories = true)
    @EnableAzureStorage(basePackages="internal.org.springframework.content.azure.it")
    @Import(InfrastructureConfig.class)
    public static class TestConfig {
        @Bean
        public BlobServiceClientBuilder blobServiceClientBuilder() {
            return builder;
        }
    }

    @Configuration
    public static class InfrastructureConfig {

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
            factory.setPackagesToScan("internal.org.springframework.content.azure.it");
            factory.setDataSource(dataSource());

            return factory;
        }

        @Bean
        public PlatformTransactionManager transactionManager() {

            JpaTransactionManager txManager = new JpaTransactionManager();
            txManager.setEntityManagerFactory(entityManagerFactory().getObject());
            return txManager;
        }
    }

    @Entity
    @Setter
    @Getter
    @NoArgsConstructor
    public static class TestEntity {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long id;

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        public TestEntity(String contentId) {
            this.contentId = new String(contentId);
        }
    }

    public interface TestEntityRepository extends JpaRepository<TestEntity, Long> {}
    public interface TestEntityStore extends ContentStore<TestEntity, String> {}

    @Entity
    @Setter
    @Getter
    @NoArgsConstructor
    public static class SharedIdContentIdEntity {

        @jakarta.persistence.Id
        @ContentId
        private String contentId = UUID.randomUUID().toString();

        @ContentLength
        private long contentLen;
    }

    public interface SharedIdRepository extends JpaRepository<SharedIdContentIdEntity, String> {}
    public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Entity
    @Table(name="entity_with_embedded")
    public static class EntityWithEmbeddedContent {

        @Id
        private String id = UUID.randomUUID().toString();

        @Embedded
        private EmbeddedContent content;
    }

    @Embeddable
    @NoArgsConstructor
    @Data
    public static class EmbeddedContent {

        @ContentId
        private String contentId;

        @ContentLength
        private Long contentLen;
    }

    public interface EmbeddedRepository extends JpaRepository<EntityWithEmbeddedContent, String> {}
    public interface EmbeddedStore extends ContentStore<EntityWithEmbeddedContent, String> {}
}
