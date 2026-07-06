package it.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.UUID;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.annotations.MimeType;
import org.springframework.content.commons.annotations.OriginalFileName;
import org.springframework.content.commons.io.DeletableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.ContentStore;
import org.springframework.content.commons.repository.StoreAccessException;
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
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;




import internal.org.springframework.content.fs.store.DefaultFilesystemStoreImpl;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.bytebuddy.utility.RandomString;

import javax.sql.DataSource;



public class FilesystemStorePropertyPathAccessorsIT {

    private DefaultFilesystemStoreImpl<Object, String> mongoContentRepoImpl;
	private FilesystemStorePropertyPathAccessorsIT.TEntity entity;
	private Resource genericResource;

	private InputStream content;
	private InputStream result;
	private Exception e;

	private AnnotationConfigApplicationContext context;

	private TestEntityRepository repo;
	private TestEntityStore store;

	private String resourceLocation;

	@Nested
    @DisplayName("DefaultFilesystemStoreImpl PropertyPath Accessors")
    class DefaultfilesystemstoreimplPropertypathAccessors {

			@BeforeEach
        void setUp() throws Exception {
				context = new AnnotationConfigApplicationContext();
				context.register(FilesystemStorePropertyPathAccessorsIT.TestConfig.class);
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
						entity = new FilesystemStorePropertyPathAccessorsIT.TEntity();
						entity = repo.save(entity);
					}

					@Test
        @DisplayName("should not have an associated resource")
        void shouldNotHaveAnAssociatedResource() throws Exception {
						assertThat(entity.getContent().getId(), is(nullValue()));
						assertThat(store.getResource(entity, PropertyPath.from("content")), is(nullValue()));
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
								store.associate(entity, PropertyPath.from("content"), resourceLocation);
							}

							@Test
        @DisplayName("should be recorded as such on the entity's @ContentId")
        void shouldBeRecordedAsSuchOnTheEntitySContentid() throws Exception {
								assertThat(entity.getContent().getId(), is(resourceLocation));
							}

							@Nested
    @DisplayName("when the resource is unassociated")
    class WhenTheResourceIsUnassociated {

								@BeforeEach
        void setUp() throws Exception {
                                    store.unassociate(entity, PropertyPath.from("content"));
								}

								@Test
        @DisplayName("should reset the entity's @ContentId")
        void shouldResetTheEntitySContentid() throws Exception {
									assertThat(entity.getContent().getId(), is(nullValue()));
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
					entity = new FilesystemStorePropertyPathAccessorsIT.TEntity();
					entity = repo.save(entity);

					store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
				}

				@Test
        @DisplayName("should be able to store new content")
        void shouldBeAbleToStoreNewContent() throws Exception {
				    // content
					try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
					} catch (IOException ioe) {}
				}

				@Test
        @DisplayName("should have content metadata")
        void shouldHaveContentMetadata() throws Exception {
				    // content
					assertThat(entity.getContent().getId(), is(notNullValue()));
					assertThat(entity.getContent().getId().trim().length(), greaterThan(0));
					assertEquals(27L, entity.getContent().getLength());
				}

				@Nested
    @DisplayName("when content is updated")
    class WhenContentIsUpdated {
					@BeforeEach
        void setUp() throws Exception {
						store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
						entity = repo.save(entity);
					}

					@Test
        @DisplayName("should have the updated content")
        void shouldHaveTheUpdatedContent() throws Exception {
					    //content
						boolean matches = false;
						try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
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
						store.setContent(entity, PropertyPath.from("content"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
						entity = repo.save(entity);
					}
					@Test
        @DisplayName("should store only the new content")
        void shouldStoreOnlyTheNewContent() throws Exception {
					    //content
						boolean matches = false;
						try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
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
						resourceLocation = entity.getContent().getId().toString();
						entity = store.unsetContent(entity, PropertyPath.from("content"));
						entity = repo.save(entity);
					}

					@Test
        @DisplayName("should have no content")
        void shouldHaveNoContent() throws Exception {
					    //content
						try (InputStream content = store.getContent(entity, PropertyPath.from("content"))) {
							assertThat(content, is(Matchers.nullValue()));
						}

						assertThat(entity.getContent().getId(), is(Matchers.nullValue()));
						assertEquals(0, entity.getContent().getLength());
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
			}
		}

	@Configuration
	@EnableJpaRepositories(considerNestedRepositories = true)
	@EntityScan(basePackageClasses = TEntity.class)
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
	    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {

	        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
	        vendorAdapter.setDatabase(Database.HSQL);
	        vendorAdapter.setGenerateDdl(true);

	        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
	        factory.setJpaVendorAdapter(vendorAdapter);
	        factory.setPackagesToScan(TEntity.class.getPackage().getName());
	        factory.setDataSource(dataSource);

	        return factory;
	    }

	    @Bean
	    public PlatformTransactionManager transactionManager(DataSource dataSource) {

	        JpaTransactionManager txManager = new JpaTransactionManager();
	        txManager.setEntityManagerFactory(entityManagerFactory(dataSource).getObject());
	        return txManager;
	    }
	}

	@Entity
	@NoArgsConstructor
	@Getter
	@Setter
	@Table(name = "tentity_content")
	public static class TEntity {

	    @Id
	    @GeneratedValue(strategy = GenerationType.AUTO)
	    private UUID id;

	    private String number;

	    @Embedded
	    @AttributeOverride(name="id", column = @Column(name = "content__id"))
	    private EmbeddedContent content = new EmbeddedContent();
	}

	@Embeddable
	@NoArgsConstructor
	@Getter
	@Setter
	public static class EmbeddedContent {
	    @ContentId
	    private String id;

	    @ContentLength
	    private long length;

	    @MimeType
	    private String mimetype;

	    @OriginalFileName
	    private String filename;
	}

	public interface TestEntityRepository extends JpaRepository<TEntity, UUID> {}
	public interface TestEntityStore extends ContentStore<TEntity, String> {}
}
