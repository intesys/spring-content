package it.typesupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import it.typesupport.model.*;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.net.URI;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { FsTypeSupportConfig.class })
public class FsTypeSupportTest {

    @Autowired protected UUIDBasedContentEntityStore uuidStore;
    @Autowired protected URIBasedContentEntityStore uriStore;
    @Autowired protected LongBasedContentEntityStore longStore;
    @Autowired protected BigIntegerBasedContentEntityStore bigIntStore;

    Object entity;
    Object id;

    @Nested
    @DisplayName("java.util.UUID")
    class JavaUtilUUID {

        @AfterEach
        void cleanUp() {
            uuidStore.unsetContent((UUIDBasedContentEntity)entity);
            assertThat(((UUIDBasedContentEntity) entity).getContentId(), is(nullValue()));
        }

        @Nested
        @DisplayName("given a content entity")
        class GivenAContentEntity {

            @BeforeEach
            void setUp() {
                entity = new UUIDBasedContentEntity();
            }

            @Nested
            @DisplayName("given the Application sets the ID")
            class GivenTheApplicationSetsTheID {

                @BeforeEach
                void setUp() {
                    id = UUID.randomUUID();
                    ((UUIDBasedContentEntity)entity).setContentId((UUID)id);
                    uuidStore.setContent((UUIDBasedContentEntity)entity, new ByteArrayInputStream("uuid".getBytes()));
                }

                @Test
                @DisplayName("should store the content successfully")
                void shouldStoreTheContentSuccessfully() throws Exception {
                    assertThat(IOUtils.contentEquals(uuidStore.getContent((UUIDBasedContentEntity)entity), IOUtils.toInputStream("uuid")), is(true));
                }
            }

            @Nested
            @DisplayName("given Spring Content generates the ID")
            class GivenSpringContentGeneratesTheID {

                @BeforeEach
                void setUp() {
                    uuidStore.setContent((UUIDBasedContentEntity)entity, new ByteArrayInputStream("uuid".getBytes()));
                }

                @Test
                @DisplayName("should store the content successfully")
                void shouldStoreTheContentSuccessfully() throws Exception {
                    assertThat(IOUtils.contentEquals(uuidStore.getContent((UUIDBasedContentEntity)entity), IOUtils.toInputStream("uuid")), is(true));
                }
            }
        }
    }

    @Nested
    @DisplayName("java.net.URI")
    class JavaNetURI {

        @AfterEach
        void cleanUp() {
            uriStore.unsetContent((URIBasedContentEntity)entity);
            assertThat(((URIBasedContentEntity) entity).getContentId(), is(nullValue()));
        }

        @Nested
        @DisplayName("given a content entity")
        class GivenAContentEntity {

            @BeforeEach
            void setUp() {
                entity = new URIBasedContentEntity();
            }

            @Nested
            @DisplayName("given the Application sets the ID")
            class GivenTheApplicationSetsTheID {

                @BeforeEach
                void setUp() throws Exception {
                    id = new URI("/some/deep/location");
                    ((URIBasedContentEntity)entity).setContentId((URI)id);
                    uriStore.setContent((URIBasedContentEntity)entity, new ByteArrayInputStream("uri".getBytes()));
                }

                @Test
                @DisplayName("should store the content successfully")
                void shouldStoreTheContentSuccessfully() throws Exception {
                    assertThat(IOUtils.contentEquals(uriStore.getContent((URIBasedContentEntity)entity), IOUtils.toInputStream("uri")), is(true));
                }
            }
        }
    }

    @Nested
    @DisplayName("java.lang.Long")
    class JavaLangLong {

        @AfterEach
        void cleanUp() {
            longStore.unsetContent((LongBasedContentEntity)entity);
            assertThat(((LongBasedContentEntity) entity).getContentId(), is(nullValue()));
        }

        @Nested
        @DisplayName("given a content entity")
        class GivenAContentEntity {

            @BeforeEach
            void setUp() {
                entity = new LongBasedContentEntity();
            }

            @Nested
            @DisplayName("given the Application sets the ID")
            class GivenTheApplicationSetsTheID {

                @BeforeEach
                void setUp() {
                    id = Long.MAX_VALUE;
                    ((LongBasedContentEntity)entity).setContentId((Long)id);
                    longStore.setContent((LongBasedContentEntity)entity, new ByteArrayInputStream("long".getBytes()));
                }

                @Test
                @DisplayName("should store the content successfully")
                void shouldStoreTheContentSuccessfully() throws Exception {
                    assertThat(IOUtils.contentEquals(longStore.getContent((LongBasedContentEntity)entity), IOUtils.toInputStream("long")), is(true));
                }
            }
        }
    }

    @Nested
    @DisplayName("java.math.BigInteger")
    class JavaMathBigInteger {

        @AfterEach
        void cleanUp() {
            bigIntStore.unsetContent((BigIntegerBasedContentEntity)entity);
            assertThat(((BigIntegerBasedContentEntity) entity).getContentId(), is(nullValue()));
        }

        @Nested
        @DisplayName("given a content entity")
        class GivenAContentEntity {

            @BeforeEach
            void setUp() {
                entity = new BigIntegerBasedContentEntity();
            }

            @Nested
            @DisplayName("given the Application sets the ID")
            class GivenTheApplicationSetsTheID {

                @BeforeEach
                void setUp() {
                    id = BigInteger.valueOf(Long.MAX_VALUE);
                    ((BigIntegerBasedContentEntity)entity).setContentId((BigInteger)id);
                    bigIntStore.setContent((BigIntegerBasedContentEntity)entity, new ByteArrayInputStream("big-int".getBytes()));
                }

                @Test
                @DisplayName("should store the content successfully")
                void shouldStoreTheContentSuccessfully() throws Exception {
                    assertThat(IOUtils.contentEquals(bigIntStore.getContent((BigIntegerBasedContentEntity)entity), IOUtils.toInputStream("big-int")), is(true));
                }
            }
        }
    }
}
