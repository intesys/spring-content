package internal.org.springframework.content.rest.it;

import internal.org.springframework.content.rest.support.TestEntity2;
import internal.org.springframework.content.rest.support.TestEntity2JpaStore;
import internal.org.springframework.content.rest.support.TestEntity2Repository;
import internal.org.springframework.content.rest.support.TestEntityChild;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import net.bytebuddy.utility.RandomString;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.content.commons.property.PropertyPath;
import org.springframework.web.context.WebApplicationContext;

import java.io.ByteArrayInputStream;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.when;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class AbstractRestIT {

    @Autowired
    private TestEntity2Repository claimRepo;

    @Autowired
    private TestEntity2JpaStore claimFormStore;

    @LocalServerPort
    int port;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private TestEntity2 existingClaim;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.webAppContextSetup(webApplicationContext);

        Iterable<TestEntity2> existingClaims = claimRepo.findAll();
        for (TestEntity2 existingClaim : existingClaims) {
            if (existingClaim.getChild() != null) {
                claimFormStore.unsetContent(existingClaim, PropertyPath.from("child"));
            }
        }

        for (TestEntity2 existingClaim : existingClaims) {
            claimRepo.delete(existingClaim);
        }

        existingClaim = new TestEntity2();
        claimRepo.save(existingClaim);
    }

    @Test
    @DisplayName("should be POSTable with new content with 201 Created")
    void shouldPostNewContentWith201() {
        when()
        .get("/files/" + existingClaim.getId() + "/child")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_NOT_FOUND);

        String newContent = "This is some new content";

        given()
        .contentType("text/plain")
        .body(newContent.getBytes())
        .when()
        .post("/files/" + existingClaim.getId() + "/child")
        .then()
        .statusCode(HttpStatus.SC_CREATED);

        given()
        .header("accept", "text/plain")
        .get("/files/" + existingClaim.getId() + "/child")
        .then()
        .statusCode(HttpStatus.SC_OK)
        .assertThat()
        .contentType(Matchers.startsWith("text/plain"))
        .body(Matchers.equalTo(newContent));
    }

    @Test
    @DisplayName("given that claim has existing content: should return the content with 200 OK")
    void givenExistingContentShouldReturnContentWith200() {
        existingClaim.setChild(new TestEntityChild());
        existingClaim.getChild().setMimeType("text/plain");
        claimFormStore.setContent(existingClaim, PropertyPath.from("child"), new ByteArrayInputStream("This is plain text content!".getBytes()));
        claimRepo.save(existingClaim);

        given()
        .header("accept", "text/plain")
        .get("/files/" + existingClaim.getId() + "/child")
        .then()
        .statusCode(HttpStatus.SC_OK)
        .assertThat()
        .contentType(Matchers.startsWith("text/plain"))
        .body(Matchers.equalTo("This is plain text content!"));
    }

    @Test
    @DisplayName("given that claim has existing content: should be POSTable with new content with 201 Created")
    void givenExistingContentShouldPostNewContent() {
        existingClaim.setChild(new TestEntityChild());
        existingClaim.getChild().setMimeType("text/plain");
        claimFormStore.setContent(existingClaim, PropertyPath.from("child"), new ByteArrayInputStream("This is plain text content!".getBytes()));
        claimRepo.save(existingClaim);

        String newContent = "This is new content";

        given()
        .contentType("text/plain")
        .body(newContent.getBytes())
        .when()
        .post("/files/" + existingClaim.getId() + "/child")
        .then()
        .statusCode(HttpStatus.SC_OK);

        given()
        .header("accept", "text/plain")
        .get("/files/" + existingClaim.getId() + "/child")
        .then()
        .statusCode(HttpStatus.SC_OK)
        .assertThat()
        .contentType(Matchers.startsWith("text/plain"))
        .body(Matchers.equalTo(newContent));
    }

    @Test
    @DisplayName("given that claim has existing content: should be DELETEable with 204 No Content")
    void givenExistingContentShouldDeleteWith204() {
        existingClaim.setChild(new TestEntityChild());
        existingClaim.getChild().setMimeType("text/plain");
        claimFormStore.setContent(existingClaim, PropertyPath.from("child"), new ByteArrayInputStream("This is plain text content!".getBytes()));
        claimRepo.save(existingClaim);

        given()
        .delete("/files/" + existingClaim.getId() + "/child")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_NO_CONTENT);

        when()
        .get("/files/" + existingClaim.getId() + "/child")
        .then()
        .assertThat()
        .statusCode(HttpStatus.SC_NOT_FOUND);
    }

    protected String getId() {
        RandomString random  = new RandomString(5);
        return "/store-tests/" + random.nextString();
    }

    public static String getContextName(Class<?> configClass) {
        return configClass.getSimpleName().replaceAll("Config", "");
    }
}
