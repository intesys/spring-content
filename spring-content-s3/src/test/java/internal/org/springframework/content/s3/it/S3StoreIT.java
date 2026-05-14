package internal.org.springframework.content.s3.it;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.*;
import java.util.Arrays;
import lombok.*;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.content.s3.config.EnableS3Stores;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest()
public class S3StoreIT {

    private static final String BUCKET = "test-bucket";

    static {
        System.setProperty("spring.content.s3.bucket", BUCKET);
    }

    private static Object mutex = new Object();

    @Autowired
    private TestEntityRepository repo;

    @Autowired
    private TestEntityStore store;

    @Autowired
    private S3Client client;

    private String resourceLocation;

    private Resource genericResource;

    private TestEntity entity;

    private Exception e;

    @Autowired
    private SharedIdRepository sharedIdRepository;
    @Autowired
    private SharedIdStore sharedIdStore;

    @Autowired
    private EmbeddedRepository embeddedRepo;
    @Autowired
    private EmbeddedStore embeddedStore;

    @Nested
    @DisplayName("S3 Storage")
    class S3Storage {

        @BeforeEach
        void setUp() throws Exception {
            synchronized(mutex) {
                HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                        .bucket("test-bucket")
                        .build();

                try {
                    client.headBucket(headBucketRequest);
                } catch (NoSuchBucketException e) {

                    CreateBucketRequest bucketRequest = CreateBucketRequest.builder()
                            .bucket("test-bucket")
                            .build();
                    client.createBucket(bucketRequest);

                    boolean found = false;
                    while (!found) {
                        headBucketRequest = HeadBucketRequest.builder()
                                .bucket(BUCKET)
                                .build();
                        try {
                            client.headBucket(headBucketRequest);
                            found = true;
                        } catch (NoSuchBucketException e2) {
                        }

                        System.out.println("sleeping...");
                        Thread.sleep(100);
                    }
                }
            }

            RandomString random  = new RandomString(5);
            resourceLocation = random.nextString();
        }

        @Nested
        @DisplayName("Store")
        class Store {

            @Nested
            @DisplayName("#getResource")
            class Getresource {

                @BeforeEach
                void setUp() throws Exception {
                    genericResource = store.getResource(resourceLocation);
                }

                @AfterEach
                void tearDown() throws Exception {
                    ((DeletableResource)genericResource).delete();
                }

                @Test
                @DisplayName("should get Resource")
                void shouldGetResource() throws Exception {
                    assertThat(genericResource, is(instanceOf(Resource.class)));
                }

                @Test
                @DisplayName("should not exist")
                void shouldNotExist() throws Exception {
                    assertThat(genericResource.exists(), is(false));
                }

                @Test
                @DisplayName("should be a RangeableResource")
                void shouldBeARangeableResource() throws Exception {
                    assertThat(genericResource, is(instanceOf(RangeableResource.class)));
                }

                @Nested
                @DisplayName("given content is added to that resource")
                class GivenContentIsAddedToThatResource {

                    @BeforeEach
                    void setUp() throws Exception {
                        try (InputStream is = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                            try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                IOUtils.copy(is, os);
                            }
                        }
                    }

                    @Test
                    @DisplayName("should store that content")
                    void shouldStoreThatContent() throws Exception {
                        assertThat(genericResource.exists(), is(true));

                        boolean matches = false;
                        try (InputStream expected = new ByteArrayInputStream("Hello Spring Content World!".getBytes())) {
                            try (InputStream actual = genericResource.getInputStream()) {
                                matches = IOUtils.contentEquals(expected, actual);
                                assertThat(matches, Matchers.is(true));
                            }
                        }
                    }

                    @Nested
                    @DisplayName("given that resource is then updated")
                    class GivenThatResourceIsThenUpdated {

                        @BeforeEach
                        void setUp() throws Exception {
                            try (InputStream is = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                                try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
                                    IOUtils.copy(is, os);
                                }
                            }
                        }

                        @Test
                        @DisplayName("should store that updated content")
                        void shouldStoreThatUpdatedContent() throws Exception {
                            assertThat(genericResource.exists(), is(true));

                            try (InputStream expected = new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes())) {
                                try (InputStream actual = genericResource.getInputStream()) {
                                    assertThat(IOUtils.contentEquals(expected, actual), is(true));
                                }
                            }
                        }
                    }

                    @Nested
                    @DisplayName("given a byte range is requested")
                    class GivenAByteRangeIsRequested {

                        @Test
                        @DisplayName("should return a partial content input stream and the partial content")
                        void shouldReturnAPartialContentInputStreamAndThePartialContent() throws Exception {

                            ((RangeableResource)genericResource).setRange("bytes=6-19");

                            var expectedBytes = "Hello Spring Content World!".getBytes();
                            Arrays.fill(expectedBytes, 0, 6, (byte) 0);
                            Arrays.fill(expectedBytes, 20, expectedBytes.length, (byte) 0);

                            try(InputStream actual = genericResource.getInputStream()) {
                                var actualBytes = actual.readAllBytes();
                                assertArrayEquals(expectedBytes, actualBytes);
                            }
                        }
                    }

                    @Nested
                    @DisplayName("given that resource is then deleted")
                    class GivenThatResourceIsThenDeleted {

                        @BeforeEach
                        void setUp() throws Exception {
                            try {
                                ((DeletableResource) genericResource).delete();
                            } catch (Exception ex) {
                                e = ex;
                            }
                        }

                        @Test
                        @DisplayName("should not exist")
                        void shouldNotExist() throws Exception {
                            assertThat(e, is(nullValue()));
                        }
                    }
                }
            }
        }

        @Nested
        @DisplayName("AssociativeStore")
        class Associativestore {

            @Nested
            @DisplayName("given a new entity")
            class GivenANewEntity {

                @BeforeEach
                void setUp() throws Exception {
                    entity = new TestEntity();
                    entity = repo.save(entity);
                }

                @Test
                @DisplayName("should not have an associated resource")
                void shouldNotHaveAnAssociatedResource() throws Exception {
                    assertThat(entity.getContentId(), is(nullValue()));
                    assertThat(store.getResource(entity), is(nullValue()));
                }

                @Nested
                @DisplayName("given a resource")
                class GivenAResource {

                    @BeforeEach
                    void setUp() throws Exception {
                        genericResource = store.getResource(resourceLocation);
                    }

                    @Nested
                    @DisplayName("when the resource is associated")
                    class WhenTheResourceIsAssociated {

                        @BeforeEach
                        void setUp() throws Exception {
                            store.associate(entity, resourceLocation);
                            store.associate(entity, PropertyPath.from("rendition"), resourceLocation);
                        }

                        @Test
                        @DisplayName("should be recorded as such on the entity's @ContentId")
                        void shouldBeRecordedAsSuchOnTheEntitySContentid() throws Exception {
                            assertThat(entity.getContentId(), is(resourceLocation));
                            assertThat(entity.getRenditionId(), is(resourceLocation));
                        }

                        @Nested
                        @DisplayName("when the resource is unassociated")
                        class WhenTheResourceIsUnassociated {

                            @BeforeEach
                            void setUp() throws Exception {
                                store.unassociate(entity);
                                store.unassociate(entity, PropertyPath.from("rendition"));
                            }

                            @Test
                            @DisplayName("should reset the entity's @ContentId")
                            void shouldResetTheEntitySContentid() throws Exception {
                                assertThat(entity.getContentId(), is(nullValue()));
                                assertThat(entity.getRenditionId(), is(nullValue()));
                            }
                        }

                        @Nested
                        @DisplayName("when a invalid property path is used to associate a resource")
                        class WhenAInvalidPropertyPathIsUsedToAssociateAResource {

                            @Test
                            @DisplayName("should throw an error")
                            void shouldThrowAnError() throws Exception {
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
                        class WhenAInvalidPropertyPathIsUsedToLoadAResource {

                            @Test
                            @DisplayName("should throw an error")
                            void shouldThrowAnError() throws Exception {
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
                        class WhenAInvalidPropertyPathIsUsedToUnassociateAResource {

                            @Test
                            @DisplayName("should throw an error")
                            void shouldThrowAnError() throws Exception {
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
        class Contentstore {

            @BeforeEach
            void setUp() throws Exception {
                entity = new TestEntity();
                entity = repo.save(entity);

                store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
            }

            @Test
            @DisplayName("should be able to store new content")
            void shouldBeAbleToStoreNewContent() throws Exception {
                try (InputStream content = store.getContent(entity)) {
                    assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                } catch (IOException ioe) {}

                try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                    assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));
                } catch (IOException ioe) {}
            }

            @Test
            @DisplayName("should have content metadata")
            void shouldHaveContentMetadata() throws Exception {
                assertThat(entity.getContentId(), is(CoreMatchers.notNullValue()));
                assertThat(entity.getContentId().trim().length(), greaterThan(0));
                assertEquals(Long.valueOf(27L), entity.getContentLen());

                assertThat(entity.getRenditionId(), is(CoreMatchers.notNullValue()));
                assertThat(entity.getRenditionId().trim().length(), greaterThan(0));
                assertEquals(40L, entity.getRenditionLen());
            }

            @Nested
            @DisplayName("when content is updated")
            class WhenContentIsUpdated {

                @BeforeEach
                void setUp() throws Exception {
                    store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
                    entity = repo.save(entity);
                }

                @Test
                @DisplayName("should have the updated content")
                void shouldHaveTheUpdatedContent() throws Exception {
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
                void setUp() throws Exception {
                    store.setContent(entity, new ByteArrayInputStream("Hello Spring World!".getBytes()));
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
                    entity = repo.save(entity);
                }

                @Test
                @DisplayName("should store only the new content")
                void shouldStoreOnlyTheNewContent() throws Exception {
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
                void shouldHaveTheUpdatedContent() throws Exception {
                    String contentId = entity.getContentId();
                    client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(contentId).build());

                    store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().disposition(SetContentParams.ContentDisposition.CreateNew).build());
                    entity = repo.save(entity);

                    boolean matches = false;
                    try (InputStream content = store.getContent(entity)) {
                        matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
                        assertThat(matches, is(true));
                    }

                    assertThat(entity.getContentId(), is(not(contentId)));
                    client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(entity.getContentId()).build());
                }
            }

            @Nested
            @DisplayName("when content is unset")
            class WhenContentIsUnset {

                @BeforeEach
                void setUp() throws Exception {
                    resourceLocation = entity.getContentId().toString();
                    entity = store.unsetContent(entity);
                    entity = store.unsetContent(entity, PropertyPath.from("rendition"));
                    entity = repo.save(entity);
                }

                @Test
                @DisplayName("should have no content")
                void shouldHaveNoContent() throws Exception {
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(content, is(Matchers.nullValue()));
                    }

                    assertThat(entity.getContentId(), is(Matchers.nullValue()));
                    assertThat(entity.getContentLen(), is(nullValue()));

                    try {
                        client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(resourceLocation).build());
                        fail("expected content to be removed but is still exists");
                    } catch (NoSuchKeyException nske) {
                    }

                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(content, is(Matchers.nullValue()));
                    }

                    assertThat(entity.getRenditionId(), is(Matchers.nullValue()));
                    assertThat(entity.getRenditionLen(), is(0L));
                }
            }

            @Nested
            @DisplayName("when content is unset but kept")
            class WhenContentIsUnsetButKept {

                @BeforeEach
                void setUp() throws Exception {
                    resourceLocation = entity.getContentId().toString();
                    entity = store.unsetContent(entity, PropertyPath.from("content"), UnsetContentParams.builder().disposition(UnsetContentParams.Disposition.Keep).build());
                    entity = repo.save(entity);
                }

                @Test
                @DisplayName("should have no content")
                void shouldHaveNoContent() throws Exception {
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(content, is(Matchers.nullValue()));
                    }

                    assertThat(entity.getContentId(), is(Matchers.nullValue()));
                    assertThat(entity.getContentLen(), is(nullValue()));

                    client.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(resourceLocation).build());
                }
            }

            @Nested
            @DisplayName("when an invalid property path is used to setContent")
            class WhenAnInvalidPropertyPathIsUsedToSetcontent {

                @Test
                @DisplayName("should throw an error")
                void shouldThrowAnError() throws Exception {
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
            class WhenAnInvalidPropertyPathIsUsedToGetcontent {

                @Test
                @DisplayName("should throw an error")
                void shouldThrowAnError() throws Exception {
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
            class WhenAnInvalidPropertyPathIsUsedToUnsetcontent {

                @Test
                @DisplayName("should throw an error")
                void shouldThrowAnError() throws Exception {
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
                void shouldNotResetTheIdField() throws Exception {
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
            class EmbeddedContent {

                @Nested
                @DisplayName("given a entity with a null embedded content object")
                class GivenAEntityWithANullEmbeddedContentObject {

                    @Test
                    @DisplayName("should return null when content is fetched")
                    void shouldReturnNullWhenContentIsFetched() throws Exception {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        assertThat(embeddedStore.getContent(entity, PropertyPath.from("content")), is(nullValue()));
                    }

                    @Test
                    @DisplayName("should be successful when content is set")
                    void shouldBeSuccessfulWhenContentIsSet() throws Exception {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        embeddedStore.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                        try (InputStream is = embeddedStore.getContent(entity, PropertyPath.from("content"))) {
                            assertThat(IOUtils.contentEquals(is, new ByteArrayInputStream("Hello Spring Content World!".getBytes())), is(true));
                        }
                    }

                    @Test
                    @DisplayName("should return null when content is unset")
                    void shouldReturnNullWhenContentIsUnset() throws Exception {
                        EntityWithEmbeddedContent entity = embeddedRepo.save(new EntityWithEmbeddedContent());
                        EntityWithEmbeddedContent expected = new EntityWithEmbeddedContent(entity.getId(), entity.getContent());
                        assertThat(embeddedStore.unsetContent(entity, PropertyPath.from("content")), is(expected));
                    }
                }
            }
        }
    }

    @SpringBootApplication()
    @EnableJpaRepositories(considerNestedRepositories = true)
    @EnableS3Stores
    static class Application {
        public static void main(String[] args) {
            SpringApplication.run(Application.class, args);
        }

        @Configuration
        public static class Config {

            @Bean
            public S3Client amazonS3() throws URISyntaxException {
                return LocalStack.getAmazonS3Client();
            }
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

        @MimeType
        private String contentType;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

        @MimeType
        private String renditionType;

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
