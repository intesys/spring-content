package internal.org.springframework.content.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.GetResourceParams;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.Resource;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.TestConfig;
import internal.org.springframework.content.jpa.testsupport.models.Document;
import internal.org.springframework.content.jpa.testsupport.repositories.DocumentRepository;
import internal.org.springframework.content.jpa.testsupport.stores.DocumentAssociativeStore;
import org.springframework.core.io.WritableResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;


public class AssociativeStoreIT {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

    private DocumentRepository repo;
    private DocumentAssociativeStore store;

	private PlatformTransactionManager txn;

	private Document document;
    private Resource resource;
    private String resourceId;

    private Exception e;

	@Nested
	@DisplayName("AssociativeStore")
	class Associativestore {

		@BeforeEach
		void setUp() throws Exception {
			context = new AnnotationConfigApplicationContext();
			context.register(TestConfig.class);
			context.register(H2Config.class);
			context.refresh();

			repo = context.getBean(DocumentRepository.class);
			store = context.getBean(DocumentAssociativeStore.class);
			txn = context.getBean(PlatformTransactionManager.class);
		}

		@Nested
		@DisplayName("given a new entity")
		class GivenANewEntity {

			@BeforeEach
			void setUp() throws Exception {
				document = new Document();
				document = repo.save(document);
			}

			@Test
			@DisplayName("should not have an associated resource")
			void shouldNotHaveAnAssociatedResource() throws Exception {
				assertThat(document.getContentId(), is(nullValue()));
				assertThat(store.getResource(document), is(nullValue()));
			}

			@Nested
			@DisplayName("given a resource")
			class GivenAResource {

				@BeforeEach
				void setUp() throws Exception {
					resourceId = UUID.randomUUID().toString();
					resource = store.getResource(resourceId);
				}

				@Nested
				@DisplayName("when the resource is associated")
				class WhenTheResourceIsAssociated {

					@BeforeEach
					void setUp() throws Exception {
						store.associate(document, resourceId);
						store.associate(document, PropertyPath.from("rendition"), resourceId);
					}

					@Test
					@DisplayName("should be recorded as such on the entity's @ContentId")
					void shouldBeRecordedAsSuchOnTheEntitySContentid() throws Exception {
						assertThat(document.getContentId(), is(resourceId));
						assertThat(document.getRenditionId(), is(resourceId));
					}

					@Nested
					@DisplayName("when the resource has content")
					class WhenTheResourceHasContent {

						@BeforeEach
						void setUp() throws Exception {
							TransactionStatus status = txn.getTransaction(new DefaultTransactionDefinition());

							Resource r = store.getResource(document, PropertyPath.from("content"), GetResourceParams.builder().build());
							try (OutputStream os = ((WritableResource)resource).getOutputStream()) {
								os.write("Hello Client-side World!".getBytes());
							}

							txn.commit(status);
						}

						@Test
						@DisplayName("should not honor byte ranges")
						void shouldNotHonorByteRanges() throws Exception {
							Resource r = store.getResource(document, PropertyPath.from("content"), GetResourceParams.builder().range("5-10").build());
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
							store.unassociate(document);
							store.unassociate(document, PropertyPath.from("rendition"));
						}

						@Test
						@DisplayName("should reset the entity's @ContentId")
						void shouldResetTheEntitySContentid() throws Exception {
							assertThat(document.getContentId(), is(nullValue()));
							assertThat(document.getRenditionId(), is(nullValue()));
						}
					}
				}

				@Nested
				@DisplayName("when a invalid property path is used to associate a resource")
				class WhenAInvalidPropertyPathIsUsedToAssociateAResource {

					@Test
					@DisplayName("should throw an error")
					void shouldThrowAnError() throws Exception {
						try {
							store.associate(document, PropertyPath.from("does.not.exist"), resourceId);
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
							store.getResource(document, PropertyPath.from("does.not.exist"));
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
							store.unassociate(document, PropertyPath.from("does.not.exist"));
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
