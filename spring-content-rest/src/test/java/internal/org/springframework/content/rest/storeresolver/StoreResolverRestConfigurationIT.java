package internal.org.springframework.content.rest.storeresolver;

import internal.org.springframework.content.rest.support.TestEntity2;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(classes = Application.class, webEnvironment=WebEnvironment.RANDOM_PORT)
public class StoreResolverRestConfigurationIT {

    @Autowired
    private Application.TEntityRepository repo;

    @Autowired
    private Application.TEntityJpaStore jpaStore;

    @Autowired
    private Application.TEntityFsStore fsStore;

    @LocalServerPort
    int port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Nested
    @DisplayName("JpaRest")
    class JpaRestTests {

        private Application.TEntity tEntity;

        @BeforeEach
        void setup() {
            RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
        }

        @Nested
        @DisplayName("given that claim has existing content")
        class GivenClaimHasExistingContent {

            @BeforeEach
            void init() {
                tEntity = new Application.TEntity();
                tEntity = repo.save(tEntity);
            }

            @Test
            @DisplayName("should return the content from the correct store")
	void shouldReturnContentFromCorrectStore() throws Exception {
                assertThat(jpaStore, is(not(nullValue())));
                assertThat(fsStore, is(not(nullValue())));
                assertThat(repo, is(not(nullValue())));

                String newContent = "This is some new content";

                given()
                        .contentType("text/plain")
                        .body(newContent.getBytes())
                        .when()
                        .post("/tEntities/" + tEntity.getId())
                        .then()
                        .statusCode(HttpStatus.SC_CREATED);

                tEntity = repo.findById(tEntity.getId()).get();

                try (InputStream is = fsStore.getContent(tEntity)) {
                    assertThat(IOUtils.toString(is), is(newContent));
                }

                try (InputStream is = jpaStore.getContent(tEntity)) {
                    assertThat(is, is(nullValue()));
                }
            }
        }
    }
}
