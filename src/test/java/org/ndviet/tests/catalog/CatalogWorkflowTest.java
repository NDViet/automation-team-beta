package org.ndviet.tests.catalog;

import com.platform.testframework.annotation.AffectedBy;
import static com.platform.testframework.annotation.TestMetadata.Severity.BLOCKER;
import static com.platform.testframework.annotation.TestMetadata.Severity.NORMAL;

import com.platform.testframework.annotation.TestMetadata;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.UUID;
import org.ndviet.tests.api.client.CatalogClient;
import org.ndviet.tests.api.client.GraphQlClient;
import org.ndviet.tests.api.model.CreateProductRequest;
import org.ndviet.tests.base.BaseApiTest;
import org.testng.annotations.Test;

@TestMetadata(owner = "automation-team-beta", feature = "Catalog")
@AffectedBy({
    "org.ndviet.testing.catalog.CatalogService",
    "org.ndviet.testing.catalog.ProductController"
})
public class CatalogWorkflowTest extends BaseApiTest {

  private final CatalogClient catalogClient = new CatalogClient();
  private final GraphQlClient graphQlClient = new GraphQlClient();

  @Test(description = "TC003: Create product via REST and fetch it via GraphQL", groups = {"smoke", "catalog", "graphql"})
  @TestMetadata(severity = BLOCKER, story = "Product Creation")
  @AffectedBy({
      "org.ndviet.testing.catalog.CatalogService",
      "org.ndviet.testing.catalog.ProductController",
      "org.ndviet.testing.catalog.ProductGraphqlController"
  })
  public void createProductAndFetchItViaGraphQl() {
    String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    CreateProductRequest request = new CreateProductRequest(
        sku,
        "API Automation Product",
        "Product created during catalog workflow validation",
        new BigDecimal("49.99"),
        12,
        true);

    log.step("Create a product via REST");
    Response createResponse = catalogClient.createProduct(request);
    softly(soft -> {
      soft.assertThat(createResponse.statusCode()).as("Create status").isEqualTo(200);
      soft.assertThat(createResponse.jsonPath().getString("sku")).as("Created SKU").isEqualTo(sku);
    });
    env("catalog.sku", sku);
    log.endStep();

    log.step("Query the same product via GraphQL");
    Response graphQlResponse = graphQlClient.queryProductBySku(sku);
    softly(soft -> {
      soft.assertThat(graphQlResponse.statusCode()).as("GraphQL status").isEqualTo(200);
      soft.assertThat(graphQlResponse.jsonPath().getString("data.productBySku.sku")).as("GraphQL SKU").isEqualTo(sku);
      soft.assertThat(graphQlResponse.jsonPath().getInt("data.productBySku.inventoryCount")).as("Inventory count").isEqualTo(12);
    });
    log.endStep();
  }

  @Test(description = "TC004: Adjust inventory and verify the updated product state", groups = {"catalog"})
  @TestMetadata(severity = NORMAL, story = "Inventory Management")
  public void adjustInventoryAndVerifyLatestState() {
    String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    catalogClient.createProduct(new CreateProductRequest(
        sku,
        "Inventory Product",
        "Inventory verification product",
        new BigDecimal("19.99"),
        5,
        true));

    log.step("Adjust inventory by +7");
    Response patchResponse = catalogClient.adjustInventory(sku, 7);
    Integer patchedInventory = patchResponse.jsonPath().getObject("inventoryCount", Integer.class);
    softly(soft -> {
      soft.assertThat(patchResponse.statusCode())
          .as("Patch status | body=%s", patchResponse.asString())
          .isEqualTo(200);
      soft.assertThat(patchedInventory)
          .as("Patched inventory | body=%s", patchResponse.asString())
          .isEqualTo(12);
    });
    log.endStep();

    log.step("Verify the product reflects the new inventory");
    Response getResponse = catalogClient.getProductBySku(sku);
    Integer currentInventory = getResponse.jsonPath().getObject("inventoryCount", Integer.class);
    softly(soft -> {
      soft.assertThat(getResponse.statusCode())
          .as("Get status | body=%s", getResponse.asString())
          .isEqualTo(200);
      soft.assertThat(currentInventory)
          .as("Current inventory | body=%s", getResponse.asString())
          .isEqualTo(12);
    });
    log.endStep();
  }
}
