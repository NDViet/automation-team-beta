package org.ndviet.tests.authentication;

import static com.platform.testframework.annotation.TestMetadata.Severity.BLOCKER;
import static com.platform.testframework.annotation.TestMetadata.Severity.CRITICAL;
import static com.platform.testframework.annotation.TestMetadata.Severity.NORMAL;

import com.platform.testframework.annotation.TestMetadata;
import io.restassured.response.Response;
import org.ndviet.library.RestAssured;
import org.ndviet.tests.api.client.GraphQlClient;
import org.ndviet.tests.api.client.CatalogClient;
import org.ndviet.tests.base.BaseApiTest;
import org.testng.annotations.Test;

@TestMetadata(owner = "automation-team-beta", feature = "Authentication")
public class AuthenticationApiTest extends BaseApiTest {

  private final CatalogClient catalogClient = new CatalogClient();
  private final GraphQlClient graphQlClient = new GraphQlClient();

  @Test(description = "TC001: Missing bearer token returns 401", groups = {"smoke", "authentication"})
  @TestMetadata(severity = BLOCKER, story = "Bearer Authentication")
  public void rejectMissingBearerToken() {
    log.step("Reopen API session without authentication");
    RestAssured.closeSession();
    RestAssured.openSessionWithoutAuthentication(BASE_URL);
    log.endStep();

    log.step("Call products endpoint without token");
    Response response = catalogClient.listProducts();
    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status code").isEqualTo(401);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("missing_bearer_token");
    });
    log.endStep();
  }

  @Test(description = "TC002: Invalid bearer token returns 401", groups = {"authentication"})
  @TestMetadata(severity = CRITICAL, story = "Bearer Authentication")
  public void rejectInvalidBearerToken() {
    log.step("Reopen API session with an invalid token");
    RestAssured.closeSession();
    RestAssured.openSessionWithBearerToken(BASE_URL, "invalid-token");
    log.endStep();

    log.step("Call products endpoint with invalid token");
    Response response = catalogClient.listProducts();
    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status code").isEqualTo(401);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("invalid_bearer_token");
    });
    log.endStep();
  }

  @Test(description = "TC003: Health endpoint remains public without bearer token", groups = {"authentication", "health"})
  @TestMetadata(severity = NORMAL, story = "Bearer Authentication")
  public void allowActuatorHealthWithoutToken() {
    log.step("Reopen API session without authentication");
    RestAssured.closeSession();
    RestAssured.openSessionWithoutAuthentication(BASE_URL);
    log.endStep();

    log.step("Call health endpoint without token");
    Response response = RestAssured.given().when().get("/actuator/health");
    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status code").isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("status")).as("Health status").isEqualTo("UP");
    });
    log.endStep();
  }

  @Test(description = "TC004: GraphQL endpoint rejects requests without bearer token", groups = {"authentication", "graphql"})
  @TestMetadata(severity = CRITICAL, story = "Bearer Authentication")
  public void rejectGraphQlQueryWithoutToken() {
    log.step("Reopen API session without authentication");
    RestAssured.closeSession();
    RestAssured.openSessionWithoutAuthentication(BASE_URL);
    log.endStep();

    log.step("Call GraphQL query without token");
    Response response = graphQlClient.queryHealth();
    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status code").isEqualTo(401);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("missing_bearer_token");
    });
    log.endStep();
  }

  @Test(description = "TC005: Valid bearer token allows access to protected endpoints", groups = {"authentication"})
  @TestMetadata(severity = NORMAL, story = "Bearer Authentication")
  public void allowValidBearerTokenForProtectedEndpoint() {
    log.step("Call products endpoint with the configured bearer token");
    Response response = catalogClient.listProducts();
    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status code").isEqualTo(200);
      soft.assertThat(response.asString()).as("Response body").isNotBlank();
    });
    log.endStep();
  }
}
