package it.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.bytebuddy.utility.RandomString;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.*;
import org.springframework.content.fs.config.EnableFilesystemStores;
import org.springframework.content.fs.io.FileSystemResourceLoader;
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
import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.*;



public class FilesystemStoreIT {

	private FilesystemStoreIT.TEntity entity;
	private Resource genericResource;
	private Exception e;

	private AnnotationConfigApplicationContext context;

	private TestEntityRepository repo;
	private TestEntityStore store;

	private EmbeddedRepository embeddedRepo;
	private EmbeddedStore embeddedStore;

	private String resourceLocation;

	@Nested
    @DisplayName("DefaultFilesystemStoreImpl")
    class Defaultfilesystemstoreimpl {

			@BeforeEach
        void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(FilesystemStoreIT.TestConfig.class);
				context.refresh();

				repo = context.getBean(TestEntityRepository.class);
				store = context.getBean(TestEntityStore.class);

				RandomString random  = new RandomString(5);
				resourceLocation = random.nextString();

				embeddedRepo = context.getBean(EmbeddedRepository.class);
				embeddedStore = context.getBean(EmbeddedStore.class);
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
						entity = new FilesystemStoreIT.TEntity();
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
									// relies on REST-layer to serve byte range
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
					entity = new FilesystemStoreIT.TEntity();
					entity = repo.save(entity);

					store.setContent(entity, new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
					store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
				}

				@Test
        @DisplayName("should be able to store new content")
        void shouldBeAbleToStoreNewContent() throws Exception {
					// content
					try (InputStream content = store.getContent(entity)) {
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
					} catch (IOException ioe) {
					}

					//rendition
					try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));
					} catch (IOException ioe) {
					}
				}

				@Test
        @DisplayName("should have content metadata")
        void shouldHaveContentMetadata() throws Exception {
					// content
					assertThat(entity.getContentId(), is(notNullValue()));
					assertThat(entity.getContentId().trim().length(), greaterThan(0));
					assertEquals(Long.valueOf(27L), entity.getContentLen());

					//rendition
					assertThat(entity.getRenditionId(), is(notNullValue()));
					assertThat(entity.getRenditionId().trim().length(), greaterThan(0));
					assertEquals(40L, entity.getRenditionLen());
				}

				@Nested
    @DisplayName("when content is updated")
    class WhenContentIsUpdated {
					@Test
        @DisplayName("should have the updated content")
        void shouldHaveTheUpdatedContent() throws Exception {
						FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
						String contentId = entity.getContentId();
						assertThat(new File(loader.getFilesystemRoot(), contentId).exists(), is(true));
						String renditionId = entity.getRenditionId();
						assertThat(new File(loader.getFilesystemRoot(), renditionId).exists(), is(true));
						store.setContent(entity, new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
						store.setContent(entity, PropertyPath.from("rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
						entity = repo.save(entity);

						//content
						boolean matches = false;
						try (InputStream content = store.getContent(entity)) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
							assertThat(matches, is(true));
						}

						//rendition
						matches = false;
						try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
							assertThat(matches, is(true));
						}

						assertThat(entity.getContentId(), is(contentId));
						assertThat(entity.getRenditionId(), is(renditionId));

						assertThat(new File(loader.getFilesystemRoot(), entity.getContentId()).exists(), is(true));
						assertThat(new File(loader.getFilesystemRoot(), entity.getRenditionId()).exists(), is(true));

						int i=0;
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
						//content
						boolean matches = false;
						try (InputStream content = store.getContent(entity)) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
							assertThat(matches, is(true));
						}

						//rendition
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
						FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
						String contentId = entity.getContentId();
						assertThat(new File(loader.getFilesystemRoot(), contentId).exists(), is(true));

						store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().disposition(SetContentParams.ContentDisposition.CreateNew).build());
						entity = repo.save(entity);

						boolean matches = false;
						try (InputStream content = store.getContent(entity)) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
							assertThat(matches, is(true));
						}

						assertThat(new File(loader.getFilesystemRoot(), contentId).exists(), is(true));

						assertThat(entity.getContentId(), is(not(contentId)));

						assertThat(new File(loader.getFilesystemRoot(), entity.getContentId()).exists(), is(true));
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
						//content
						try (InputStream content = store.getContent(entity)) {
							assertThat(content, is(Matchers.nullValue()));
						}

						assertThat(entity.getContentId(), is(Matchers.nullValue()));
						assertThat(entity.getContentLen(), is(nullValue()));

						//rendition
						try (InputStream content = store.getContent(entity, PropertyPath.from("rendition"))) {
							assertThat(content, is(Matchers.nullValue()));
						}

						assertThat(entity.getRenditionId(), is(Matchers.nullValue()));
						assertThat(entity.getRenditionLen(), is(0L));

						FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
						assertThat(new File(loader.getFilesystemRoot(), resourceLocation).exists(), is(false));
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
						//content
						try (InputStream content = store.getContent(entity)) {
							assertThat(content, is(Matchers.nullValue()));
						}

						assertThat(entity.getContentId(), is(Matchers.nullValue()));
						assertThat(entity.getContentLen(), is(nullValue()));

						FileSystemResourceLoader loader = context.getBean(FileSystemResourceLoader.class);
						assertThat(new File(loader.getFilesystemRoot(), resourceLocation).exists(), is(true));
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
						assertThat(sharedIdContentIdEntity.getContentLen(), is(nullValue()));
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
							int i = 0;
						}
					}
				}
			}
		}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories = true)
	@EnableFilesystemStores
	@Import(InfrastructureConfig.class)
	public static class TestConfig {
		//
	}

	@Configuration
	public static class InfrastructureConfig {

	    @Bean
	    File filesystemRoot() {
	        try {
	            return Files.createTempDirectory("").toFile();
	        } catch (IOException ioe) {}
	        return null;
	    }

	    @Bean
	    FileSystemResourceLoader fileSystemResourceLoader() {
	        return new FileSystemResourceLoader(filesystemRoot().getAbsolutePath());
	    }

	    @Bean
	    public DataSource dataSource() {
	        EmbeddedDatabaseBuilder builder = new EmbeddedDatabaseBuilder();
	        return builder.setType(EmbeddedDatabaseType.HSQL).build();
	    }

	    @Bean
	    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {

	        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
	        vendorAdapter.setDatabase(Database.HSQL);
	        vendorAdapter.setGenerateDdl(true);

	        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
	        factory.setJpaVendorAdapter(vendorAdapter);
	        factory.setPackagesToScan("it.store");
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

	public interface ContentProperty {
	    String getContentId();

		void setContentId(String contentId);

		Long getContentLen();

		void setContentLen(Long contentLen);
	}

	@Entity
	@Table(name="tentity")
	public static class TEntity implements ContentProperty {

	    @Id
        private String id = UUID.randomUUID().toString();

		@ContentId
		private String contentId;

		@ContentLength
		private Long contentLen;

        @ContentId
        private String renditionId;

        @ContentLength
        private long renditionLen;

		public TEntity() {
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
        public Long getContentLen() {
			return contentLen;
		}

		@Override
        public void setContentLen(Long contentLen) {
			this.contentLen = contentLen;
		}

        public String getRenditionId() {
            return this.renditionId;
        }

        public void setRenditionId(String renditionId) {
            this.renditionId = renditionId;
        }

        public long getRenditionLen() {
            return renditionLen;
        }

        public void setRenditionLen(long renditionLen) {
            this.renditionLen = renditionLen;
        }
	}

	public interface TestEntityRepository extends JpaRepository<TEntity, String> {}
	public interface TestEntityStore extends ContentStore<TEntity, String> {}

	@Entity
    @Table(name="shared_id_entity")
	public static class SharedIdContentIdEntity implements ContentProperty {

		@Id
		@ContentId
		private String contentId = UUID.randomUUID().toString();

		@ContentLength
		private Long contentLen;

		public SharedIdContentIdEntity() {
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
        public Long getContentLen() {
			return contentLen;
		}

		@Override
        public void setContentLen(Long contentLen) {
			this.contentLen = contentLen;
		}
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
