package internal.org.springframework.content.mongo.it;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.model.GridFSFile;
import internal.org.springframework.content.mongo.store.DefaultMongoStoreImpl;
import lombok.Getter;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.hamcrest.Matchers;

import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.mongo.config.EnableMongoStores;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;


public class DepractedMongoStoreIT {
	private DefaultMongoStoreImpl<Object, String> mongoContentRepoImpl;
	private GridFsTemplate gridFsTemplate;
	private GridFSFile gridFSFile;
	private ObjectId gridFSId;
	private TestEntity entity;
	private GridFsResource resource;
	private Resource genericResource;
	private PlacementService placer;

	private InputStream content;
	private InputStream result;
	private Exception e;

	private AnnotationConfigApplicationContext context;

	private TestEntityRepository repo;
	private TestEntityStore store;

	private String resourceLocation;

	@Nested
	@DisplayName("DefaultMongoStoreImpl")
	class Defaultmongostoreimpl {

		@BeforeEach
		void setUp() throws Exception {
			context = new AnnotationConfigApplicationContext();
			context.register(TestConfig.class);
			context.refresh();

			repo = context.getBean(TestEntityRepository.class);
			store = context.getBean(TestEntityStore.class);

			RandomString random  = new RandomString(5);
			resourceLocation = random.nextString();
		}

		@AfterEach
		void tearDown() throws Exception {
			context.close();
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
        @DisplayName("when the resource has content")
        class WhenTheResourceHasContent {
								@BeforeEach
        void setUp() throws Exception {
									try (OutputStream os = ((WritableResource)genericResource).getOutputStream()) {
										os.write("Hello Client-side World!".getBytes());
									}
								}

								@Test
        @DisplayName("should not honor byte ranges")
        void shouldNotHonorByteRanges() throws Exception {
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
                    store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                }

                @Test
        @DisplayName("should be able to store new content")
        void shouldBeAbleToStoreNewContent() throws Exception {
                    try (InputStream content = store.getContent(entity)) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                    } catch (IOException ioe) {}

                    try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
                    } catch (IOException ioe) {}
                }

                @Test
        @DisplayName("should have content metadata")
        void shouldHaveContentMetadata() throws Exception {
                    assertThat(entity.getContentId(), is(notNullValue()));
                    assertThat(entity.getContentId().trim().length(), greaterThan(0));
                    assertThat(entity.getContentLen(), is(27L));

                    assertThat(entity.getRenditionId(), is(notNullValue()));
                    assertThat(entity.getRenditionId().trim().length(), greaterThan(0));
                    assertThat(entity.getRenditionLen(), is(27L));
                }

                @Nested
        @DisplayName("when content is updated")
        class WhenContentIsUpdated {
                    @BeforeEach
        void setUp() throws Exception {
                        store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
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
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
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
                        store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
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
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
                            assertThat(matches, is(true));
                        }
                    }
                }

                @Nested
        @DisplayName("when content is deleted")
        class WhenContentIsDeleted {
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
                        assertThat(entity.getContentLen(), is(0L));

                        try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
                            assertThat(content, is(Matchers.nullValue()));
                        }

                        assertThat(entity.getContentId(), is(Matchers.nullValue()));
                        assertThat(entity.getContentLen(), is(0L));
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
        @DisplayName("when content is deleted and the id field is shared with javax id")
        class WhenContentIsDeletedAndTheIdFieldIsSharedWithJavaxId {

					@Test
        @DisplayName("should not reset the id field")
        void shouldNotResetTheIdField() throws Exception {
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
        @DisplayName("when content is deleted and the id field is shared with spring id")
        class WhenContentIsDeletedAndTheIdFieldIsSharedWithSpringId {

					@Test
        @DisplayName("should not reset the id field")
        void shouldNotResetTheIdField() throws Exception {
						SharedSpringIdRepository SharedSpringIdRepository = context.getBean(SharedSpringIdRepository.class);
						SharedSpringIdStore SharedSpringIdStore = context.getBean(SharedSpringIdStore.class);

						SharedSpringIdContentIdEntity SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(new SharedSpringIdContentIdEntity());

						SharedSpringIdContentIdEntity = SharedSpringIdStore.setContent(SharedSpringIdContentIdEntity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
						SharedSpringIdContentIdEntity = SharedSpringIdRepository.save(SharedSpringIdContentIdEntity);
						String id = SharedSpringIdContentIdEntity.getContentId();
						SharedSpringIdContentIdEntity = SharedSpringIdStore.unsetContent(SharedSpringIdContentIdEntity);
						assertThat(SharedSpringIdContentIdEntity.getContentId(), is(id));
						assertThat(SharedSpringIdContentIdEntity.getContentLen(), is(0L));
					}
				}
		}
	}

	@Configuration
	@EnableMongoRepositories(considerNestedRepositories = true)
	@EnableMongoStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
		//
	}

	@Configuration
	public static class InfrastructureConfig extends AbstractMongoClientConfiguration {
		@Override
		protected String getDatabaseName() {
			return MongoTestContainer.getTestDbName();
		}

		@Override
        @Bean
		public MongoClient mongoClient() {
			return MongoTestContainer.getMongoClient();
		}

		@Bean
		public GridFsTemplate gridFsTemplate(MappingMongoConverter mongoConverter) {
			return new GridFsTemplate(mongoDbFactory(), mongoConverter);
		}

		@Override
        @Bean
		public MongoDatabaseFactory mongoDbFactory() {
			return new SimpleMongoClientDatabaseFactory(mongoClient(), getDatabaseName());
		}
	}

	public interface ContentProperty {
		String getContentId();

		void setContentId(String contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	@Getter
	@Setter
	public static class TestEntity implements ContentProperty {

		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = new String(contentId);
		}
	}

	public interface TestEntityRepository extends MongoRepository<TestEntity, String> {}
	public interface TestEntityStore extends ContentStore<TestEntity, String> {}

	public static class SharedIdContentIdEntity implements ContentProperty {

		@jakarta.persistence.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
        public String getContentId() {
			return this.contentId;
		}

		@Override
        public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
        public long getContentLen() {
			return contentLen;
		}

		@Override
        public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public interface SharedIdRepository extends MongoRepository<SharedIdContentIdEntity, String> {}
	public interface SharedIdStore extends ContentStore<SharedIdContentIdEntity, String> {}

	public static class SharedSpringIdContentIdEntity implements ContentProperty {

		@org.springframework.data.annotation.Id
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public SharedSpringIdContentIdEntity() {
			this.contentId = null;
		}

		@Override
        public String getContentId() {
			return this.contentId;
		}

		@Override
        public void setContentId(String contentId) {
			this.contentId = contentId;
		}

		@Override
        public long getContentLen() {
			return contentLen;
		}

		@Override
        public void setContentLen(long contentLen) {
			this.contentLen = contentLen;
		}
	}

	public interface SharedSpringIdRepository extends MongoRepository<SharedSpringIdContentIdEntity, String> {}
	public interface SharedSpringIdStore extends ContentStore<SharedSpringIdContentIdEntity, String> {}
}
