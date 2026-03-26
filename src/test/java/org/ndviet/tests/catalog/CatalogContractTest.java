package org.ndviet.tests.catalog;

import com.platform.testframework.annotation.AffectedBy;
import static com.platform.testframework.annotation.TestMetadata.Severity.CRITICAL;
import static com.platform.testframework.annotation.TestMetadata.Severity.NORMAL;

import com.platform.testframework.annotation.TestMetadata;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.ndviet.library.RestAssured;
import org.ndviet.tests.api.client.CatalogClient;
import org.ndviet.tests.api.model.CreateProductRequest;
import org.ndviet.tests.api.model.UpdateProductRequest;
import org.ndviet.tests.base.BaseApiTest;
import org.testng.annotations.Test;

@TestMetadata(owner = "automation-team-beta", feature = "Catalog REST Contract")
@AffectedBy({
    "org.ndviet.testing.catalog.CatalogService",
    "org.ndviet.testing.catalog.ProductController",
    "org.ndviet.testing.catalog.ProductRepository",
    "org.ndviet.testing.catalog.ProductResponse"
})
public class CatalogContractTest extends BaseApiTest {

  private final CatalogClient catalogClient = new CatalogClient();

  @Test(description = "TC006: Create product returns the expected contract", groups = {"catalog", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Product Contract")
  public void createProductReturnsCompleteContract() {
    String sku = uniqueSku();
    Response response = catalogClient.createProduct(productRequest(sku, "Contract Product", 8, true));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("id")).as("ID").isNotBlank();
      soft.assertThat(response.jsonPath().getString("sku")).as("SKU").isEqualTo(sku);
      soft.assertThat(response.jsonPath().getString("name")).as("Name").isEqualTo("Contract Product");
      soft.assertThat(response.jsonPath().getString("createdAt")).as("Created timestamp").isNotBlank();
      soft.assertThat(response.jsonPath().getString("updatedAt")).as("Updated timestamp").isNotBlank();
    });
  }

  @Test(description = "TC007: Product listing supports query filtering", groups = {"catalog", "contract"})
  @TestMetadata(severity = NORMAL, story = "Product Search")
  public void listProductsReturnsCreatedRecordWhenQueried() {
    String sku = uniqueSku();
    String query = "search-" + sku.substring(4);
    catalogClient.createProduct(productRequest(sku, query + " Headset", 4, true));

    Response response = catalogClient.listProducts(null, query);
    List<String> skus = response.jsonPath().getList("sku", String.class);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(skus).as("Returned SKUs").contains(sku);
    });
  }

  @Test(description = "TC008: Active product filter excludes inactive records", groups = {"catalog", "contract"})
  @TestMetadata(severity = NORMAL, story = "Product Filtering")
  public void listProductsActiveOnlyExcludesInactiveProducts() {
    String token = "active-" + uniqueSku().substring(4);
    String activeSku = uniqueSku();
    String inactiveSku = uniqueSku();

    catalogClient.createProduct(productRequest(activeSku, token + " Active", 6, true));
    catalogClient.createProduct(productRequest(inactiveSku, token + " Inactive", 6, false));

    Response response = catalogClient.listProducts(true, token);
    List<String> skus = response.jsonPath().getList("sku", String.class);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(skus).as("Active-only SKUs").contains(activeSku);
      soft.assertThat(skus).as("Active-only SKUs").doesNotContain(inactiveSku);
    });
  }

  @Test(description = "TC009: Missing product lookup returns 404", groups = {"catalog", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Product Lookup")
  public void getProductBySkuReturns404WhenMissing() {
    Response response = catalogClient.getProductBySku(uniqueSku());

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(404);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("product_not_found");
    });
  }

  @Test(description = "TC010: Duplicate SKU creation returns 409", groups = {"catalog", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Product Creation")
  public void createDuplicateProductReturns409() {
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Duplicate Product", 9, true));

    Response response = catalogClient.createProduct(productRequest(sku, "Duplicate Product", 9, true));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(409);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("product_exists");
    });
  }

  @Test(description = "TC011: Invalid product payload returns 400", groups = {"catalog", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Product Validation")
  public void createInvalidProductReturns400() {
    Map<String, Object> payload = Map.of(
        "sku", "",
        "name", "",
        "description", "",
        "price", 0,
        "inventoryCount", -1,
        "active", true);

    Response response = RestAssured.given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post("/api/v1/products");

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(400);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("validation_error");
    });
  }

  @Test(description = "TC012: Product update replaces mutable fields", groups = {"catalog", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Product Update")
  public void updateProductReplacesMutableFields() {
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Legacy Product", 7, true));

    Response updateResponse = catalogClient.updateProduct(
        sku,
        new UpdateProductRequest("Updated Product", "Updated contract description", new BigDecimal("77.45"), false));

    softly(soft -> {
      soft.assertThat(updateResponse.statusCode()).as("Status | body=%s", updateResponse.asString()).isEqualTo(200);
      soft.assertThat(updateResponse.jsonPath().getString("name")).as("Name").isEqualTo("Updated Product");
      soft.assertThat(updateResponse.jsonPath().getString("description")).as("Description")
          .isEqualTo("Updated contract description");
      soft.assertThat(updateResponse.jsonPath().getFloat("price")).as("Price").isEqualTo(77.45f);
      soft.assertThat(updateResponse.jsonPath().getBoolean("active")).as("Active flag").isFalse();
    });
  }

  @Test(description = "TC013: Product activation endpoint toggles state", groups = {"catalog", "contract"})
  @TestMetadata(severity = NORMAL, story = "Product Activation")
  public void setProductActivationTogglesState() {
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Activation Product", 3, true));

    Response deactivateResponse = catalogClient.setProductActivation(sku, false);
    Response activateResponse = catalogClient.setProductActivation(sku, true);

    softly(soft -> {
      soft.assertThat(deactivateResponse.statusCode()).as("Deactivate status").isEqualTo(200);
      soft.assertThat(deactivateResponse.jsonPath().getBoolean("active")).as("Active after deactivate").isFalse();
      soft.assertThat(activateResponse.statusCode()).as("Activate status").isEqualTo(200);
      soft.assertThat(activateResponse.jsonPath().getBoolean("active")).as("Active after activate").isTrue();
    });
  }

  @Test(description = "TC014: Inventory cannot be adjusted below zero", groups = {"catalog", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Inventory")
  public void adjustInventoryRejectsBelowZero() {
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Low Inventory Product", 1, true));

    Response response = catalogClient.adjustInventory(sku, -2);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(400);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("inventory_below_zero");
    });
  }

  @Test(description = "TC015: Low stock endpoint filters by threshold and query", groups = {"catalog", "contract"})
  @TestMetadata(severity = NORMAL, story = "Low Stock")
  public void lowStockEndpointFiltersByThresholdAndQuery() {
    String token = "low-" + uniqueSku().substring(4);
    String lowSku = uniqueSku();
    String healthySku = uniqueSku();

    catalogClient.createProduct(productRequest(lowSku, token + " Low", 2, true));
    catalogClient.createProduct(productRequest(healthySku, token + " Healthy", 12, true));

    Response response = catalogClient.listLowStockProducts(3, token);
    List<String> skus = response.jsonPath().getList("sku", String.class);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(skus).as("Low stock SKUs").contains(lowSku);
      soft.assertThat(skus).as("Low stock SKUs").doesNotContain(healthySku);
    });
  }

  private CreateProductRequest productRequest(String sku, String name, int inventoryCount, boolean active) {
    return new CreateProductRequest(
        sku,
        name,
        name + " created for catalog contract testing",
        new BigDecimal("19.95"),
        inventoryCount,
        active);
  }
}
