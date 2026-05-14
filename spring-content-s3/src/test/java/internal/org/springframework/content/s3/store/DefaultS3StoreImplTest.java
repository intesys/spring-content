package internal.org.springframework.content.s3.store;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.UUID;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.config.ContentPropertyInfo;
import org.springframework.content.commons.io.RangeableResource;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.content.commons.repository.StoreAccessException;
import org.springframework.content.commons.utils.PlacementService;
import org.springframework.content.commons.utils.PlacementServiceImpl;
import org.springframework.content.s3.Bucket;
import org.springframework.content.s3.S3ObjectId;
import org.springframework.content.s3.config.MultiTenantS3ClientProvider;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;

import internal.org.springframework.content.s3.config.S3StoreConfiguration;
import internal.org.springframework.content.s3.io.S3StoreResource;
import internal.org.springframework.content.s3.io.SimpleStorageProtocolResolver;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

public class DefaultS3StoreImplTest {

	private DefaultS3StoreImpl<ContentProperty, String> s3StoreImpl;
	private DefaultS3StoreImpl<ContentProperty, S3ObjectId> s3ObjectIdBasedStore;
	private DefaultS3StoreImpl<ContentProperty, CustomContentId> customS3ContentIdBasedStore;

	private GenericApplicationContext context = new GenericApplicationContext();
	private ResourceLoader loader;
	private PlacementService placementService;
	private S3Client client, client2;

	private MultiTenantS3ClientProvider clientProvider;

	private String defaultBucket;

	private CustomContentId customId;
	private ContentProperty entity;

	private String id;
	private WritableResource resource;
	private Resource r, nonExistentResource;
	private InputStream content;
	private OutputStream output;
	private File parent;
	private InputStream result;
	private Exception e;

	@Nested
	@DisplayName("DefaultS3StoreImpl")
	class DefaultS3StoreImplScenarios {

		@BeforeEach
		void setUp() throws Exception {
			resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
			loader = mock(ResourceLoader.class);
			placementService = mock(PlacementService.class);
			client = mock(S3Client.class);
			defaultBucket = null;

			context.registerBean("s3Client", S3Client.class, () -> client, new BeanDefinitionCustomizer[0]);
			context.refresh();
		}

		@Nested
		@DisplayName("Store")
		class Store {

			@Nested
			@DisplayName("#getResource")
			class GetResource {

				@Nested
				@DisplayName("given the store's ID is an S3ObjectId type")
				class GivenTheStoreSIdIsAnS3ObjectidType {

					@BeforeEach
					void setUp() throws Exception {
						placementService = new PlacementServiceImpl();
						S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
						placementService.addConverter(new Converter<String, String>() {
							@Override
							public String convert(String source) {
								return "/some/object/id";
							}
						});

						SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
						s3Protocol.afterPropertiesSet();
						loader = new DefaultResourceLoader();
						((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

						s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);

						try {
							r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-defaultBucket", "some-object-id"));
						} catch (Exception ex) {
							e = ex;
						}
					}

					@Test
					@DisplayName("should return the resource")
					void shouldReturnTheResource() throws Exception {
						assertThat(e, is(nullValue()));
						assertThat(r, is(instanceOf(S3StoreResource.class)));
						assertThat(((S3StoreResource) r).getClient(), is(client));
						assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-defaultBucket", "some/object/id")));
					}
				}

				@Nested
				@DisplayName("given the store's ID is a custom ID type")
				class GivenTheStoreSIdIsACustomIdType {

					@BeforeEach
					void setUp() throws Exception {
						customS3ContentIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);
					}

					@Nested
					@DisplayName("given a default bucket is set")
					class GivenADefaultBucketIsSet {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-customer";
						}

						@Nested
						@DisplayName("given the resolver is created with the static constructor function")
						class GivenTheResolverIsCreatedWithTheStaticConstructorFunction {

							@BeforeEach
							void setUp() throws Exception {
								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
								placementService.addConverter(new Converter<CustomContentId, S3ObjectId>() {
									@Override
									public S3ObjectId convert(CustomContentId entity) {
										return new S3ObjectId(entity.getCustomer(), entity.getObjectId());
									}
								});

								SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
								s3Protocol.afterPropertiesSet();
								loader = new DefaultResourceLoader();
								((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

								customS3ContentIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, null);
							}

							@Nested
							@DisplayName("given an ID")
							class GivenAnId {

								@BeforeEach
								void setUp() throws Exception {
									customId = new CustomContentId("some-customer", "some-object-id");
									try {
										r = customS3ContentIdBasedStore.getResource(customId);
									} catch (Exception ex) {
										e = ex;
									}
								}

								@Test
								@DisplayName("should fetch the resource")
								void shouldFetchTheResource() throws Exception {
									assertThat(e, is(nullValue()));
									assertThat(r, is(instanceOf(S3StoreResource.class)));
									assertThat(((S3StoreResource) r).getClient(), is(client));
									assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-customer", "some-object-id")));
								}
							}
						}
					}

					@Nested
					@DisplayName("given a default bucket is not set")
					class GivenADefaultBucketIsNotSet {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = null;
						}

						@Nested
						@DisplayName("given a resolver that does not validate")
						class GivenAResolverThatDoesNotValidate {

							@BeforeEach
							void setUp() throws Exception {
								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
							}

							@Nested
							@DisplayName("when called with an ID that doesn't specify a bucket either")
							class WhenCalledWithAnIdThatDoesnTSpecifyABucketEither {

								@BeforeEach
								void setUp() throws Exception {
									customId = new CustomContentId(null, "some-object-id");
									try {
										r = customS3ContentIdBasedStore.getResource(customId);
									} catch (Exception ex) {
										e = ex;
									}
								}

								@Test
								@DisplayName("should throw an error")
								void shouldThrowAnError() throws Exception {
									assertThat(e, is(instanceOf(ConversionFailedException.class)));
								}
							}
						}
					}
				}

				@Nested
				@DisplayName("given a multi tenant configuration")
				class GivenAMultiTenantConfiguration {

					@BeforeEach
					void setUp() throws Exception {
						client2 = mock(S3Client.class);
						clientProvider = new MultiTenantS3ClientProvider() {
							@Override
							public S3Client getS3Client() {
								return client2;
							}
						};

						placementService = new PlacementServiceImpl();
						S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
						s3ObjectIdBasedStore = new DefaultS3StoreImpl<>(context, loader, null, placementService, client, clientProvider);

						try {
							r = s3ObjectIdBasedStore.getResource(new S3ObjectId("some-bucket", "some-object-id"));
						} catch (Exception ex) {
							e = ex;
						}
					}

					@Test
					@DisplayName("should fetch the resource using the correct client")
					void shouldFetchTheResourceUsingTheCorrectClient() throws Exception {
						assertThat(e, is(nullValue()));
						assertThat(r, is(instanceOf(S3StoreResource.class)));
						assertThat(((S3StoreResource) r).getClient(), is(client2));
						assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-bucket", "some-object-id")));
					}
				}
			}
		}

		@Nested
		@DisplayName("AssociativeStore")
		class AssociativeStore {

			@BeforeEach
			void setUp() throws Exception {
				s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
			}

			@Nested
			@DisplayName("#getResource")
			class GetResource {

				@BeforeEach
				void setUp() throws Exception {
				}

				@Nested
				@DisplayName("given the default associative store id resolver")
				class GivenTheDefaultAssociativeStoreIdResolver {

					@Nested
					@DisplayName("given a default bucket")
					class GivenADefaultBucket {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-defaultBucket";
						}

						@Nested
						@DisplayName("when called with an entity that doesn't have an @Bucket value")
						class WhenCalledWithAnEntityThatDoesnTHaveAnBucketValue {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
								placementService.addConverter(new Converter<S3ObjectId, String>() {
									@Override
									public String convert(S3ObjectId source) {
										return "/" + source.getKey().replaceAll("-", "/");
									}
								});

								SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
								s3Protocol.afterPropertiesSet();
								loader = new DefaultResourceLoader();
								((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should fetch the resource")
							void shouldFetchTheResource() throws Exception {
								assertThat(e, is(nullValue()));
								assertThat(r, is(instanceOf(S3StoreResource.class)));
								assertThat(((S3StoreResource) r).getClient(), is(client));
								assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "default-defaultBucket", "12345/67890")));
							}
						}

						@Nested
						@DisplayName("when called with an entity that has an @Bucket value")
						class WhenCalledWithAnEntityThatHasAnBucketValue {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntityWithBucketAnnotation("some-other-bucket");
								entity.setContentId("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

								SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
								s3Protocol.afterPropertiesSet();
								loader = new DefaultResourceLoader();
								((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should fetch the correct resource")
							void shouldFetchTheCorrectResource() throws Exception {
								assertThat(e, is(nullValue()));
								assertThat(r, is(instanceOf(S3StoreResource.class)));
								assertThat(((S3StoreResource) r).getClient(), is(client));
								assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-other-bucket", "12345-67890")));
							}
						}

						@Nested
						@DisplayName("when called with an entity that has no associated resource")
						class WhenCalledWithAnEntityThatHasNoAssociatedResource {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity();
							}

							@Test
							@DisplayName("should return null")
							void shouldReturnNull() throws Exception {
								assertThat(r, is(nullValue()));
								assertThat(e, is(nullValue()));
							}
						}
					}
				}

				@Nested
				@DisplayName("given a custom id resolver")
				class GivenACustomIdResolver {

					@Nested
					@DisplayName("given a default bucket")
					class GivenADefaultBucket {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-defaultBucket";
						}

						@Nested
						@DisplayName("when called with an entity")
						class WhenCalledWithAnEntity {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
								placementService.addConverter(new Converter<TestEntity, S3ObjectId>() {
									@Override
									public S3ObjectId convert(TestEntity source) {
										return new S3ObjectId("custom-bucket", "custom-object-id");
									}
								});

								SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
								s3Protocol.afterPropertiesSet();
								loader = new DefaultResourceLoader();
								((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should fetch the resource")
							void shouldFetchTheResource() throws Exception {
								assertThat(e, is(nullValue()));
								assertThat(r, is(instanceOf(S3StoreResource.class)));
								assertThat(((S3StoreResource) r).getClient(), is(client));
								assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "custom-bucket", "custom-object-id")));
							}
						}
					}
				}

				@Nested
				@DisplayName("given a custom id resolver that cannot resolve the bucket")
				class GivenACustomIdResolverThatCannotResolveTheBucket {

					@Nested
					@DisplayName("given the default bucket is not set")
					class GivenTheDefaultBucketIsNotSet {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = null;
						}

						@Nested
						@DisplayName("when called with an entity")
						class WhenCalledWithAnEntity {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
								placementService.addConverter(new Converter<CustomContentId, S3ObjectId>() {
									@Override
									public S3ObjectId convert(CustomContentId entity) {
										return new S3ObjectId(null, entity.getObjectId());
									}
								});

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should throw an exception")
							void shouldThrowAnException() throws Exception {
								assertThat(e, is(instanceOf(ConversionFailedException.class)));
							}
						}
					}
				}
			}

			@Nested
			@DisplayName("#getResource with PropertyPath")
			class GetResourceWithPropertyPath {

				@BeforeEach
				void setUp() throws Exception {
				}

				// the following context is (and should be) exactly the same as for "#getResource" above
				@Nested
				@DisplayName("given the default associative store id resolver")
				class GivenTheDefaultAssociativeStoreIdResolver {

					@Nested
					@DisplayName("given a default bucket")
					class GivenADefaultBucket {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-defaultBucket";
						}

						@Nested
						@DisplayName("when called with an entity that doesn't have an @Bucket value")
						class WhenCalledWithAnEntityThatDoesnTHaveAnBucketValue {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
								placementService.addConverter(new Converter<S3ObjectId, String>() {
									@Override
									public String convert(S3ObjectId source) {
										return "/" + source.getKey().replaceAll("-", "/");
									}
								});

								SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
								s3Protocol.afterPropertiesSet();
								loader = new DefaultResourceLoader();
								((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should fetch the resource")
							void shouldFetchTheResource() throws Exception {
								assertThat(e, is(nullValue()));
								assertThat(r, is(instanceOf(S3StoreResource.class)));
								assertThat(((S3StoreResource) r).getClient(), is(client));
								assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "default-defaultBucket", "12345/67890")));
							}
						}

						@Nested
						@DisplayName("when called with an entity that has an @Bucket value")
						class WhenCalledWithAnEntityThatHasAnBucketValue {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntityWithBucketAnnotation("some-other-bucket");
								entity.setContentId("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

								SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
								s3Protocol.afterPropertiesSet();
								loader = new DefaultResourceLoader();
								((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should fetch the correct resource")
							void shouldFetchTheCorrectResource() throws Exception {
								assertThat(e, is(nullValue()));
								assertThat(r, is(instanceOf(S3StoreResource.class)));
								assertThat(((S3StoreResource) r).getClient(), is(client));
								assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "some-other-bucket", "12345-67890")));
							}
						}

						@Nested
						@DisplayName("when called with an entity that has no associated resource")
						class WhenCalledWithAnEntityThatHasNoAssociatedResource {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity();
								try {
									r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should return null")
							void shouldReturnNull() throws Exception {
								assertThat(r, is(nullValue()));
								assertThat(e, is(nullValue()));
							}
						}
					}
				}

				@Nested
				@DisplayName("given a custom id resolver")
				class GivenACustomIdResolver {

					@Nested
					@DisplayName("given a default bucket")
					class GivenADefaultBucket {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-defaultBucket";
						}

						@Nested
						@DisplayName("when called with an entity")
						class WhenCalledWithAnEntity {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

								// Converter that matches Entity and content Id types. Expected to be invoked.
								// Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId> instead of Converter<TestEntity, S3ObjectId> for #getResource with PropertyPath
								placementService.addConverter(new Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId>() {
									@Override
									public S3ObjectId convert(ContentPropertyInfo<TestEntity, String> source) {
										return new S3ObjectId("custom-bucket", "test-entity/custom-object-id-string-based");
									}
								});

								// Converter that does not match content Id type. Should not be invoked.
								placementService.addConverter(new Converter<ContentPropertyInfo<Object, UUID>, S3ObjectId>() {
									@Override
									public S3ObjectId convert(ContentPropertyInfo<Object, UUID> source) {
										return new S3ObjectId("custom-bucket", "object/custom-object-id-uuid-based");
									}
								});

								// Converter that does not match Entity type. Should not be invoked.
								placementService.addConverter(new Converter<ContentPropertyInfo<TestEntityWithBucketAnnotation, String>, S3ObjectId>() {
									@Override
									public S3ObjectId convert(ContentPropertyInfo<TestEntityWithBucketAnnotation, String> source) {
										return new S3ObjectId("custom-bucket", "test-entity-with-bucket/custom-object-id-string-based");
									}
								});

								SimpleStorageProtocolResolver s3Protocol = new SimpleStorageProtocolResolver(client);
								s3Protocol.afterPropertiesSet();
								loader = new DefaultResourceLoader();
								((DefaultResourceLoader) loader).addProtocolResolver(s3Protocol);

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should fetch the resource")
							void shouldFetchTheResource() throws Exception {
								assertThat(e, is(nullValue()));
								assertThat(r, is(instanceOf(S3StoreResource.class)));
								assertThat(((S3StoreResource) r).getClient(), is(client));
								assertThat(r.getDescription(), is(format("Amazon s3 resource [bucket='%s' and object='%s']", "custom-bucket", "test-entity/custom-object-id-string-based")));
							}
						}
					}
				}

				@Nested
				@DisplayName("given a custom id resolver that cannot resolve the bucket")
				class GivenACustomIdResolverThatCannotResolveTheBucket {

					@Nested
					@DisplayName("given the default bucket is not set")
					class GivenTheDefaultBucketIsNotSet {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = null;
						}

						@Nested
						@DisplayName("when called with an entity")
						class WhenCalledWithAnEntity {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity("12345-67890");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
								placementService.addConverter(new Converter<ContentPropertyInfo<TestEntity, String>, S3ObjectId>() {
									@Override
									public S3ObjectId convert(ContentPropertyInfo<TestEntity, String> source) {
										return new S3ObjectId(null, "custom-object-id");
									}
								});

								s3StoreImpl = new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null);
								try {
									r = s3StoreImpl.getResource(entity, PropertyPath.from("content"));
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should throw an exception")
							void shouldThrowAnException() throws Exception {
								assertThat(e, is(instanceOf(ConversionFailedException.class)));
							}
						}
					}
				}
			}

			@Nested
			@DisplayName("#associate")
			class Associate {

				@BeforeEach
				void setUp() throws Exception {
					id = "12345-67890";
					entity = new TestEntity();
					s3StoreImpl.associate(entity, id);
				}

				@Test
				@DisplayName("should set the entity's content ID attribute")
				void shouldSetTheEntitySContentIdAttribute() throws Exception {
					assertThat(entity.getContentId(), is("12345-67890"));
				}
			}

			@Nested
			@DisplayName("#unassociate")
			class Unassociate {

				@BeforeEach
				void setUp() throws Exception {
					entity = new TestEntity();
					entity.setContentId("12345-67890");
					s3StoreImpl.unassociate(entity);
				}

				@Test
				@DisplayName("should reset the entity's content ID attribute")
				void shouldResetTheEntitySContentIdAttribute() throws Exception {
					assertThat(entity.getContentId(), is(nullValue()));
				}
			}
		}

		@Nested
		@DisplayName("ContentStore")
		class ContentStore {

			@BeforeEach
			void setUp() throws Exception {
				s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null));
			}

			@Nested
			@DisplayName("#setContent")
			class SetContent {

				@BeforeEach
				void setUp() throws Exception {
					entity = new TestEntity();
					content = new ByteArrayInputStream("Hello content world!".getBytes());
				}

				@Nested
				@DisplayName("given the default associative store id resolver")
				class GivenTheDefaultAssociativeStoreIdResolver {

					@Nested
					@DisplayName("given a default bucket is set")
					class GivenADefaultBucketIsSet {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-defaultBucket";
						}

			@Nested
			@DisplayName("when the content already exists")
			class WhenTheContentAlreadyExists {

				@BeforeEach
				void setUp() throws Exception {
					entity.setContentId("abcd-efgh");

					placementService = new PlacementServiceImpl();
					S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);
					s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null));

					when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
					output = mock(OutputStream.class);
					when(resource.getOutputStream()).thenReturn(output);

					when(resource.contentLength()).thenReturn(20L);

					when(resource.exists()).thenReturn(true);

					try {
						s3StoreImpl.setContent(entity, content);
					} catch (Exception ex) {
						e = ex;
					}
				}

				@Test
				@DisplayName("should fetch the resource")
				void shouldFetchTheResource() throws Exception {
					verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
				}

				@Test
				@DisplayName("should change the content length")
				void shouldChangeTheContentLength() throws Exception {
					assertThat(entity.getContentLen(), is(20L));
				}

				@Test
				@DisplayName("should write to the resource's outputstream")
				void shouldWriteToTheResourceSOutputstream() throws Exception {
					verify(resource).getOutputStream();
					verify(output, times(1)).write(any(byte[].class), eq(0), eq(20));
				}

					@Nested
						@DisplayName("when the resource output stream throws an IOException")
						class WhenTheResourceOutputStreamThrowsAnIoexception {

							@BeforeEach
							void setUp() throws Exception {
								when(resource.getOutputStream()).thenThrow(new IOException("set-ioexception"));
								try {
									s3StoreImpl.setContent(entity, content);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should throw a StoreAccessException")
							void shouldThrowAStoreaccessexception() throws Exception {
								assertThat(e, is(instanceOf(StoreAccessException.class)));
								assertThat(e.getCause().getMessage(), is("set-ioexception"));
							}
						}
						}

						@Nested
						@DisplayName("when the content does not already exist")
						class WhenTheContentDoesNotAlreadyExist {

							@BeforeEach
							void setUp() throws Exception {
								assertThat(entity.getContentId(), is(nullValue()));

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

								s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null));

								when(loader.getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))).thenReturn(resource);
								output = mock(OutputStream.class);
								when(resource.getOutputStream()).thenReturn(output);

								when(resource.contentLength()).thenReturn(20L);

								File resourceFile = mock(File.class);
								parent = mock(File.class);

								when(resource.getFile()).thenReturn(resourceFile);
								when(resourceFile.getParentFile()).thenReturn(parent);

								try {
									s3StoreImpl.setContent(entity, content);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should make a new UUID")
							void shouldMakeANewUuid() throws Exception {
								assertThat(entity.getContentId(), is(not(nullValue())));
							}

							@Test
							@DisplayName("should create a new resource")
							void shouldCreateANewResource() throws Exception {
								verify(loader).getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"));
							}

							@Test
							@DisplayName("should write to the resource's outputstream")
							void shouldWriteToTheResourceSOutputstream() throws Exception {
								verify(resource).getOutputStream();
								verify(output, times(1)).write(any(byte[].class), eq(0), eq(20));
							}
						}

						@Nested
						@DisplayName("when s3 throws an S3Exception")
						class WhenS3ThrowsAnS3Exception {

							@BeforeEach
							void setUp() throws Exception {
								assertThat(entity.getContentId(), is(nullValue()));

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

								s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null));

								when(loader.getResource(matches("^s3://.*[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))).thenReturn(resource);
								output = mock(OutputStream.class);
								when(resource.getOutputStream()).thenReturn(output);

								doThrow(S3Exception.builder().message("no such upload").build()).when(output).close();

								try {
									s3StoreImpl.setContent(entity, content);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should do something")
							void shouldDoSomething() throws Exception {
								assertThat(e, is(instanceOf(S3Exception.class)));
							}
						}
					}
				}
			}

			@Nested
			@DisplayName("#setContent from Resource")
			class SetContentFromResource {

				@BeforeEach
				void setUp() throws Exception {
					entity = new TestEntity();
					content = new ByteArrayInputStream("Hello content world!".getBytes());
					r = new InputStreamResource(content);
				}

				@Test
				@DisplayName("should delegate")
				void shouldDelegate() throws Exception {
					s3StoreImpl.setContent(entity, r);
					verify(s3StoreImpl).setContent(eq(entity), eq(content));
				}

				@Nested
				@DisplayName("when the resource throws an IOException")
				class WhenTheResourceThrowsAnIoexception {

					@BeforeEach
					void setUp() throws Exception {
						r = mock(Resource.class);
						when(r.getInputStream()).thenThrow(new IOException("setContent badness"));
						try {
							s3StoreImpl.setContent(entity, r);
						} catch (Exception ex) {
							e = ex;
						}
					}

					@Test
					@DisplayName("should throw a StoreAccessException")
					void shouldThrowAStoreaccessexception() throws Exception {
						assertThat(e, is(instanceOf(StoreAccessException.class)));
						assertThat(e.getCause().getMessage(), containsString("setContent badness"));
					}
				}
			}

			@Nested
			@DisplayName("#getContent")
			class GetContent {

				@BeforeEach
				void setUp() throws Exception {
				}

				@Nested
				@DisplayName("given the default associative store id resolver")
				class GivenTheDefaultAssociativeStoreIdResolver {

					@Nested
					@DisplayName("given a default bucket is set")
					class GivenADefaultBucketIsSet {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-defaultBucket";
						}

						@Nested
						@DisplayName("when called with an entity")
						class WhenCalledWithAnEntity {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity();
								content = mock(InputStream.class);
								entity.setContentId("abcd-efgh");

								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

								when(loader.getResource(matches("^s3://default-defaultBucket/abcd-efgh"))).thenReturn(resource);
								when(resource.getInputStream()).thenReturn(content);

								s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null));
							}

							@Nested
							@DisplayName("and the resource already exists")
							class AndTheResourceAlreadyExists {

								@BeforeEach
								void setUp() throws Exception {
									when(resource.exists()).thenReturn(true);
									try {
										result = s3StoreImpl.getContent(entity);
									} catch (Exception ex) {
										e = ex;
									}
								}

								@Test
								@DisplayName("should fetch the resource")
								void shouldFetchTheResource() throws Exception {
									verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
								}

								@Test
								@DisplayName("should get content")
								void shouldGetContent() throws Exception {
									assertThat(result, is(content));
								}

								@Nested
								@DisplayName("when the resource input stream throws an IOException")
								class WhenTheResourceInputStreamThrowsAnIoexception {

									@BeforeEach
									void setUp() throws Exception {
										when(resource.getInputStream()).thenThrow(new IOException("get-ioexception"));
										try {
											result = s3StoreImpl.getContent(entity);
										} catch (Exception ex) {
											e = ex;
										}
									}

									@Test
									@DisplayName("should throw a StoreAccessException")
									void shouldThrowAStoreaccessexception() throws Exception {
										assertThat(e, is(instanceOf(StoreAccessException.class)));
										assertThat(e.getCause().getMessage(), is("get-ioexception"));
									}
								}
							}

							@Nested
							@DisplayName("and the resource doesn't exist")
							class AndTheResourceDoesnTExist {

								@BeforeEach
								void setUp() throws Exception {
									nonExistentResource = mock(WritableResource.class);
									when(resource.exists()).thenReturn(true);

									when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
									try {
										result = s3StoreImpl.getContent(entity);
									} catch (Exception ex) {
										e = ex;
									}
								}

								@Test
								@DisplayName("should fetch the resource")
								void shouldFetchTheResource() throws Exception {
									verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
								}

								@Test
								@DisplayName("should not find the content")
								void shouldNotFindTheContent() throws Exception {
									assertThat(result, is(nullValue()));
								}
							}

							@Nested
							@DisplayName("with an null @ContentId")
							class WithAnNullContentid {

								@BeforeEach
								void setUp() throws Exception {
									entity.setContentId(null);
									try {
										result = s3StoreImpl.getContent(entity);
									} catch (Exception ex) {
										e = ex;
									}
								}

								@Test
								@DisplayName("should return null")
								void shouldReturnNull() throws Exception {
									assertThat(result, is(nullValue()));
									assertThat(e, is(nullValue()));
								}
							}
						}
					}
				}
			}

			@Nested
			@DisplayName("#unsetContent")
			class UnsetContent {

				@Nested
				@DisplayName("given the default associative store id resolver")
				class GivenTheDefaultAssociativeStoreIdResolver {

					@Nested
					@DisplayName("given a default bucket is set")
					class GivenADefaultBucketIsSet {

						@BeforeEach
						void setUp() throws Exception {
							defaultBucket = "default-defaultBucket";
						}

						@Nested
						@DisplayName("when called with an entity")
						class WhenCalledWithAnEntity {

							@BeforeEach
							void setUp() throws Exception {
								entity = new TestEntity();
								entity.setContentId("abcd-efgh");
								entity.setContentLen(100L);
								resource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
							}

							@Nested
							@DisplayName("and the content exists")
							class AndTheContentExists {

								@BeforeEach
								void setUp() throws Exception {
									placementService = new PlacementServiceImpl();
									S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

									when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(resource);
									when(resource.exists()).thenReturn(true);

									s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null));
								}

								@Test
								@DisplayName("should fetch the resource")
								void shouldFetchTheResource() throws Exception {
									s3StoreImpl.unsetContent(entity);
									verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
								}

								@Nested
								@DisplayName("when the property has a dedicated ContentId field")
								class WhenThePropertyHasADedicatedContentidField {

									@BeforeEach
									void setUp() throws Exception {
										try {
											s3StoreImpl.unsetContent(entity);
										} catch (Exception ex) {
											e = ex;
										}
									}

									@Test
									@DisplayName("should reset the metadata")
									void shouldResetTheMetadata() throws Exception {
										assertThat(entity.getContentId(), is(nullValue()));
										assertThat(entity.getContentLen(), is(0L));
									}
								}

								@Nested
								@DisplayName("when the property's ContentId field also is the javax persistence Id field")
								class WhenThePropertySContentidFieldAlsoIsTheJavaxPersistenceIdField {

									@BeforeEach
									void setUp() throws Exception {
										entity = new SharedIdContentIdEntity();
										entity.setContentId("abcd-efgh");
										try {
											s3StoreImpl.unsetContent(entity);
										} catch (Exception ex) {
											e = ex;
										}
									}

									@Test
									@DisplayName("should not reset the content id metadata")
									void shouldNotResetTheContentIdMetadata() throws Exception {
										assertThat(entity.getContentId(), is("abcd-efgh"));
										assertThat(entity.getContentLen(), is(0L));
									}
								}

								@Nested
								@DisplayName("when the property's ContentId field also is the Spring Id field")
								class WhenThePropertySContentidFieldAlsoIsTheSpringIdField {

									@BeforeEach
									void setUp() throws Exception {
										entity = new SharedSpringIdContentIdEntity();
										entity.setContentId("abcd-efgh");
										try {
											s3StoreImpl.unsetContent(entity);
										} catch (Exception ex) {
											e = ex;
										}
									}

									@Test
									@DisplayName("should not reset the content id metadata")
									void shouldNotResetTheContentIdMetadata() throws Exception {
										assertThat(entity.getContentId(), is("abcd-efgh"));
										assertThat(entity.getContentLen(), is(0L));
									}
								}
							}

			@Nested
						@DisplayName("and the content doesn't exist")
						class AndTheContentDoesnTExist {

							@BeforeEach
							void setUp() throws Exception {
								placementService = new PlacementServiceImpl();
								S3StoreConfiguration.addDefaultS3ObjectIdConverters(placementService, defaultBucket);

								s3StoreImpl = spy(new DefaultS3StoreImpl<ContentProperty, String>(context, loader, null, placementService, client, null));

								nonExistentResource = mock(WritableResource.class, withSettings().extraInterfaces(RangeableResource.class));
								when(loader.getResource(endsWith("abcd-efgh"))).thenReturn(nonExistentResource);
								when(nonExistentResource.exists()).thenReturn(false);

								try {
									s3StoreImpl.unsetContent(entity);
								} catch (Exception ex) {
									e = ex;
								}
							}

							@Test
							@DisplayName("should fetch the resource")
							void shouldFetchTheResource() throws Exception {
								verify(loader).getResource(eq("s3://default-defaultBucket/abcd-efgh"));
							}

							@Test
							@DisplayName("should unset the content")
								void shouldUnsetTheContent() throws Exception {
									verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
									assertThat(entity.getContentId(), is(nullValue()));
									assertThat(entity.getContentLen(), is(0L));
								}
							}
						}
					}
				}
			}
		}
	}

	public interface ContentProperty {
		String getContentId();

		void setContentId(String contentId);

		long getContentLen();

		void setContentLen(long contentLen);
	}

	public static class TestEntity implements ContentProperty {
		@ContentId
		private String contentId;

		@ContentLength
		private long contentLen;

		public TestEntity() {
			this.contentId = null;
		}

		public TestEntity(String contentId) {
			this.contentId = new String(contentId);
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

	public class TestEntityWithBucketAnnotation extends TestEntity {
		@Bucket
		private String bucketId = null;

		public TestEntityWithBucketAnnotation(String bucketId) {
			this.bucketId = bucketId;
		}

		public String getBucketId() {
			return bucketId;
		}

		public void setBucketId(String bucketId) {
			this.bucketId = bucketId;
		}
	}

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

	public class CustomContentId implements Serializable {
		private String customer;
		private String objectId;

		public CustomContentId(String bucket, String objectId) {
			this.customer = bucket;
			this.objectId = objectId;
		}

		public String getCustomer() {
			return customer;
		}

		public void setCustomer(String customer) {
			this.customer = customer;
		}

		public String getObjectId() {
			return objectId;
		}

		public void setObjectId(String objectId) {
			this.objectId = objectId;
		}
	}
}
