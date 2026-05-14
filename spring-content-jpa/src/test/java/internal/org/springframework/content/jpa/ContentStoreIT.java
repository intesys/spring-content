package internal.org.springframework.content.jpa;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static internal.org.springframework.content.jpa.StoreIT.getContextName;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.function.Supplier;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.springframework.content.commons.store.ContentStore;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.store.SetContentParams;
import org.springframework.content.commons.store.UnsetContentParams;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import internal.org.springframework.content.jpa.StoreIT.H2Config;
import internal.org.springframework.content.jpa.StoreIT.TestConfig;
import internal.org.springframework.content.jpa.testsupport.models.Claim;
import internal.org.springframework.content.jpa.testsupport.models.ClaimForm;
import internal.org.springframework.content.jpa.testsupport.repositories.ClaimRepository;
import internal.org.springframework.content.jpa.testsupport.stores.ClaimStore;

import jakarta.persistence.*;


public class ContentStoreIT {

	private AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

	private PlatformTransactionManager ptm;

	protected ClaimRepository claimRepo;
	protected ClaimStore claimFormStore;

	private EmbeddedRepository embeddedRepo;
	private EmbeddedStore embeddedStore;

	protected Claim claim;
    protected Object id;

	@Nested
	@DisplayName("ContentStore")
	class Contentstore {

		@BeforeEach
		void setUp() throws Exception {
			context = new AnnotationConfigApplicationContext();
			context.register(TestConfig.class);
			context.register(H2Config.class);
			context.refresh();

			ptm = context.getBean(PlatformTransactionManager.class);
			claimRepo = context.getBean(ClaimRepository.class);
			claimFormStore = context.getBean(ClaimStore.class);

			embeddedRepo = context.getBean(EmbeddedRepository.class);
			embeddedStore = context.getBean(EmbeddedStore.class);

			if (ptm == null) {
				ptm = mock(PlatformTransactionManager.class);
			}
		}

		@AfterEach
		void tearDown() throws Exception {
			deleteAllClaimFormsContent();
			deleteAllClaims();
		}

		@Nested
		@DisplayName("given an Entity with content")
		class GivenAnEntityWithContent {

			@BeforeEach
			void setUp() throws Exception {
				claim = new Claim();
				claim.setFirstName("John");
				claim.setLastName("Smith");
				claim.setClaimForm(new ClaimForm());
				claim = claimRepo.save(claim);

				claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring Content World!".getBytes()));
                claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()));
			}

			@Test
			@DisplayName("should be able to store new content")
			void shouldBeAbleToStoreNewContent() throws Exception {
			    doInTransaction(ptm, () -> {
					try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
						assertThat(IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring Content World!".getBytes()), content), is(true));
					} catch (IOException ioe) {}
					return null;
				});

                doInTransaction(ptm, () -> {
                    try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                        assertThat(IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring Content World!</html>".getBytes()), content), is(true));
                    } catch (IOException ioe) {}
                    return null;
                });
			}

			@Test
			@DisplayName("should have content metadata")
			void shouldHaveContentMetadata() throws Exception {
			    assertThat(claim.getClaimForm().getContentId(), is(notNullValue()));
				assertThat(claim.getClaimForm().getContentId().trim().length(), greaterThan(0));
				assertThat(claim.getClaimForm().getContentLength(), is(Long.valueOf(27L)));

                assertThat(claim.getClaimForm().getRenditionId(), is(notNullValue()));
                assertThat(claim.getClaimForm().getRenditionId().trim().length(), greaterThan(0));
                assertThat(claim.getClaimForm().getRenditionLen(), is(40L));
			}

			@Nested
			@DisplayName("when content is updated")
			class WhenContentIsUpdated {

				@BeforeEach
				void setUp() throws Exception {
					claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()));
                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()));
					claim = claimRepo.save(claim);
				}

				@Test
				@DisplayName("should have the updated content")
				void shouldHaveTheUpdatedContent() throws Exception {
				    doInTransaction(ptm, () -> {
						boolean matches = false;
						try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
							assertThat(matches, is(true));
						} catch (IOException e) {
						}
						return null;
					});

                    doInTransaction(ptm, () -> {
                        boolean matches = false;
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Updated Spring Content World!</html>".getBytes()), content);
                            assertThat(matches, is(true));
                        } catch (IOException e) {
                        }
                        return null;
                    });
				}
			}

			@Nested
			@DisplayName("when content is updated with shorter content")
			class WhenContentIsUpdatedWithShorterContent {

				@BeforeEach
				void setUp() throws Exception {
					claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Spring World!".getBytes()));
                    claimFormStore.setContent(claim, PropertyPath.from("claimForm/rendition"), new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()));
					claim = claimRepo.save(claim);
				}

				@Test
				@DisplayName("should store only the new content")
				void shouldStoreOnlyTheNewContent() throws Exception {
				    doInTransaction(ptm, () -> {
						boolean matches = false;
						try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
							matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Spring World!".getBytes()), content);
							assertThat(matches, is(true));
						} catch (IOException e) {
						}
						return null;
					});

                    doInTransaction(ptm, () -> {
                        boolean matches = false;
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            matches = IOUtils.contentEquals(new ByteArrayInputStream("<html>Hello Spring World!</html>".getBytes()), content);
                            assertThat(matches, is(true));
                        } catch (IOException e) {
                        }
                        return null;
                    });
				}
			}

			@Nested
			@DisplayName("when content is updated and not overwritten")
			class WhenContentIsUpdatedAndNotOverwritten {

				@Test
				@DisplayName("should have the updated content")
				void shouldHaveTheUpdatedContent() throws Exception {
					String contentId = claim.getClaimForm().getContentId();

					claimFormStore.setContent(claim, PropertyPath.from("claimForm/content"), new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), SetContentParams.builder().disposition(SetContentParams.ContentDisposition.CreateNew).build());
					claim = claimRepo.save(claim);

					boolean matches = false;
					try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
						matches = IOUtils.contentEquals(new ByteArrayInputStream("Hello Updated Spring Content World!".getBytes()), content);
						assertThat(matches, is(true));
					}

					assertThat(claim.getClaimForm().getContentId(), is(not(contentId)));
				}
			}

			@Nested
			@DisplayName("when content is deleted")
			class WhenContentIsDeleted {

			    @BeforeEach
				void setUp() throws Exception {
			        id = claim.getClaimForm().getContentId();
					claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"));
                    claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/rendition"));
					claim = claimRepo.save(claim);
				}

			    @AfterEach
				void tearDown() throws Exception {
			        claimRepo.delete(claim);
			    }

				@Test
				@DisplayName("should have no content")
				void shouldHaveNoContent() throws Exception {
			        doInTransaction(ptm, () -> {
			        	try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
					        assertThat(content, is(nullValue()));
			        	} catch (IOException e) {
						}
						return null;
			        });

                    assertThat(claim.getClaimForm().getContentId(), is(nullValue()));
					assertThat(claim.getClaimForm().getContentLength(), is(nullValue()));

                    doInTransaction(ptm, () -> {
                        try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/rendition"))) {
                            assertThat(content, is(nullValue()));
                        } catch (IOException e) {
                        }
                        return null;
                    });

					assertThat(claim.getClaimForm().getRenditionId(), is(nullValue()));
					assertThat(claim.getClaimForm().getRenditionLen(), is(0L));
				}
			}

			@Nested
			@DisplayName("when content is deleted with keep")
			class WhenContentIsDeletedWithKeep {

				@BeforeEach
				void setUp() throws Exception {
					id = claim.getClaimForm().getContentId();
					claimFormStore.unsetContent(claim, PropertyPath.from("claimForm/content"), UnsetContentParams.builder().disposition(UnsetContentParams.Disposition.Keep).build());
					claim = claimRepo.save(claim);
				}

				@AfterEach
				void tearDown() throws Exception {
					claimRepo.delete(claim);
				}

				@Test
				@DisplayName("should have no content")
				void shouldHaveNoContent() throws Exception {
					doInTransaction(ptm, () -> {
						try (InputStream content = claimFormStore.getContent(claim, PropertyPath.from("claimForm/content"))) {
							assertThat(content, is(nullValue()));
						} catch (IOException e) {
						}
						return null;
					});

					assertThat(claim.getClaimForm().getContentId(), is(nullValue()));
					assertThat(claim.getClaimForm().getContentLength(), is(nullValue()));
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

	public static <T> T doInTransaction(PlatformTransactionManager ptm, Supplier<T> block) {
		TransactionStatus status = ptm.getTransaction(new DefaultTransactionDefinition());

		try {
			T result = block.get();
			ptm.commit(status);
			return result;
		} catch (Exception e) {
			ptm.rollback(status);
		}

		return null;
	}

	protected boolean hasContent(Claim claim, PropertyPath path) {

		if (claim == null) {
			return false;
		}

		boolean exists = doInTransaction(ptm, () -> {
			try (InputStream content = claimFormStore.getContent(claim, path)) {
				if (content != null) {
					return true;
				}
			} catch (Exception e) {
			}
			return false;
		});

		return exists;
	}

	protected void deleteAllClaims() {
		claimRepo.deleteAll();
	}

	protected void deleteAllClaimFormsContent() {
		Iterable<Claim> existingClaims = claimRepo.findAll();
		for (Claim existingClaim : existingClaims) {
			if (existingClaim.getClaimForm() != null && (hasContent(existingClaim, PropertyPath.from("claimForm/content")) || hasContent(existingClaim, PropertyPath.from("claimForm/rendition")))) {
				String contentId = existingClaim.getClaimForm().getContentId();
                String renditionId = existingClaim.getClaimForm().getRenditionId();
				claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/content"));
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("claimForm/rendition"));
				if (existingClaim.getClaimForm() != null) {
					assertThat(existingClaim.getClaimForm().getContentId(), is(nullValue()));
					assertThat(existingClaim.getClaimForm().getContentLength(), is(nullValue()));
                    assertThat(existingClaim.getClaimForm().getRenditionId(), is(nullValue()));
                    assertThat(existingClaim.getClaimForm().getRenditionLen(), is(Long.valueOf(0)));

					InputStream content = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/content")));
                    InputStream renditionContent = doInTransaction(ptm, () -> claimFormStore.getContent(existingClaim, PropertyPath.from("claimForm/rendition")));
					try {
						assertThat(content, is(nullValue()));
                        assertThat(renditionContent, is(nullValue()));
					}
					finally {
						IOUtils.closeQuietly(content);
					}
				}
			}
		}
	}

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
