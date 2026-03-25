package org.ndviet.tests.ordering;

import static com.platform.testframework.annotation.TestMetadata.Severity.BLOCKER;
import static com.platform.testframework.annotation.TestMetadata.Severity.CRITICAL;
import static com.platform.testframework.annotation.TestMetadata.Severity.NORMAL;

import com.platform.testframework.annotation.TestMetadata;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.ndviet.library.RestAssured;
import org.ndviet.tests.api.client.CatalogClient;
import org.ndviet.tests.api.client.OrderingClient;
import org.ndviet.tests.api.model.CreateCustomerRequest;
import org.ndviet.tests.api.model.CreateOrderItemRequest;
import org.ndviet.tests.api.model.CreateOrderRequest;
import org.ndviet.tests.api.model.CreateProductRequest;
import org.ndviet.tests.base.BaseApiTest;
import org.testng.annotations.Test;

@TestMetadata(owner = "automation-team-beta", feature = "Ordering REST Contract")
public class OrderingContractTest extends BaseApiTest {

  private final CatalogClient catalogClient = new CatalogClient();
  private final OrderingClient orderingClient = new OrderingClient();

  @Test(description = "TC016: Create customer returns expected contract", groups = {"ordering", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Customer Contract")
  public void createCustomerReturnsContract() {
    String email = uniqueEmail("contract");

    Response response = orderingClient.createCustomer(new CreateCustomerRequest(email, "Contract Customer", "STANDARD"));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("id")).as("ID").isNotBlank();
      soft.assertThat(response.jsonPath().getString("email")).as("Email").isEqualTo(email);
      soft.assertThat(response.jsonPath().getString("tier")).as("Tier").isEqualTo("STANDARD");
    });
  }

  @Test(description = "TC017: Customer listing supports tier and query filters", groups = {"ordering", "contract"})
  @TestMetadata(severity = NORMAL, story = "Customer Search")
  public void listCustomersSupportsTierAndQueryFilters() {
    String token = "filter-" + uniqueSku().substring(4).toLowerCase();
    String standardEmail = token + "-standard@example.com";
    String enterpriseEmail = token + "-enterprise@example.com";

    orderingClient.createCustomer(new CreateCustomerRequest(standardEmail, "Standard " + token, "STANDARD"));
    orderingClient.createCustomer(new CreateCustomerRequest(enterpriseEmail, "Enterprise " + token, "ENTERPRISE"));

    Response response = orderingClient.listCustomers("ENTERPRISE", token);
    List<String> emails = response.jsonPath().getList("email", String.class);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(emails).as("Filtered emails").contains(enterpriseEmail);
      soft.assertThat(emails).as("Filtered emails").doesNotContain(standardEmail);
    });
  }

  @Test(description = "TC018: Customer lookup by email returns expected record", groups = {"ordering", "contract"})
  @TestMetadata(severity = NORMAL, story = "Customer Lookup")
  public void getCustomerByEmailReturnsContract() {
    String email = uniqueEmail("lookup");
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Lookup Customer", "GOLD"));

    Response response = orderingClient.getCustomerByEmail(email);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("email")).as("Email").isEqualTo(email);
      soft.assertThat(response.jsonPath().getString("tier")).as("Tier").isEqualTo("GOLD");
    });
  }

  @Test(description = "TC019: Customer tier update returns updated contract", groups = {"ordering", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Customer Update")
  public void updateCustomerTierReturnsUpdatedCustomer() {
    String email = uniqueEmail("tier");
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Tier Customer", "STANDARD"));

    Response response = orderingClient.updateCustomerTier(email, "ENTERPRISE");

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("tier")).as("Tier").isEqualTo("ENTERPRISE");
      soft.assertThat(response.jsonPath().getString("email")).as("Email").isEqualTo(email);
    });
  }

  @Test(description = "TC020: Duplicate customer creation returns 409", groups = {"ordering", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Customer Creation")
  public void createCustomerRejectsDuplicate() {
    String email = uniqueEmail("duplicate");
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Duplicate Customer", "STANDARD"));

    Response response = orderingClient.createCustomer(new CreateCustomerRequest(email, "Duplicate Customer", "STANDARD"));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(409);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("customer_exists");
    });
  }

  @Test(description = "TC021: Invalid customer payload returns 400", groups = {"ordering", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Customer Validation")
  public void createCustomerRejectsInvalidEmail() {
    Map<String, Object> payload = Map.of(
        "email", "not-an-email",
        "fullName", "",
        "tier", "STANDARD");

    Response response = RestAssured.given()
        .contentType(ContentType.JSON)
        .body(payload)
        .when()
        .post("/api/v1/customers");

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(400);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("validation_error");
    });
  }

  @Test(description = "TC022: Order creation returns confirmed contract", groups = {"ordering", "contract"})
  @TestMetadata(severity = BLOCKER, story = "Order Contract")
  public void createOrderReturnsConfirmedContract() {
    String email = uniqueEmail("order");
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Order Contract Product", 5, true, "14.25"));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Order Contract Customer", "GOLD"));

    Response response = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Contract order",
        List.of(new CreateOrderItemRequest(sku, 2))));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("orderNumber")).as("Order number").isNotBlank();
      soft.assertThat(response.jsonPath().getString("status")).as("Status").isEqualTo("CONFIRMED");
      soft.assertThat(response.jsonPath().getFloat("totalAmount")).as("Total").isEqualTo(28.5f);
      soft.assertThat(response.jsonPath().getString("customer.email")).as("Customer email").isEqualTo(email);
    });
  }

  @Test(description = "TC023: Order listing supports status and customer filters", groups = {"ordering", "contract"})
  @TestMetadata(severity = NORMAL, story = "Order Search")
  public void listOrdersSupportsStatusAndCustomerEmailFilters() {
    String email = uniqueEmail("orders");
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Filtered Order Product", 8, true, "10.00"));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Filtered Order Customer", "STANDARD"));
    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Filtered order",
        List.of(new CreateOrderItemRequest(sku, 1))));

    String orderNumber = createOrder.jsonPath().getString("orderNumber");
    Response response = orderingClient.listOrders("CONFIRMED", email);
    List<String> orderNumbers = response.jsonPath().getList("orderNumber", String.class);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(orderNumbers).as("Filtered orders").contains(orderNumber);
    });
  }

  @Test(description = "TC024: Unknown customer order request returns 404", groups = {"ordering", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Order Validation")
  public void createOrderRejectsUnknownCustomer() {
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Unknown Customer Product", 6, true, "9.00"));

    Response response = orderingClient.createOrder(new CreateOrderRequest(
        uniqueEmail("missing"),
        "Missing customer order",
        List.of(new CreateOrderItemRequest(sku, 1))));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(404);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("customer_not_found");
    });
  }

  @Test(description = "TC025: Unknown product order request returns 404", groups = {"ordering", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Order Validation")
  public void createOrderRejectsUnknownSku() {
    String email = uniqueEmail("unknown-sku");
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Unknown Sku Customer", "STANDARD"));

    Response response = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Unknown sku order",
        List.of(new CreateOrderItemRequest(uniqueSku(), 1))));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(404);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("product_not_found");
    });
  }

  @Test(description = "TC026: Empty order items payload returns 400", groups = {"ordering", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Order Validation")
  public void createOrderRejectsEmptyItems() {
    String email = uniqueEmail("empty-items");
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Empty Items Customer", "STANDARD"));

    Response response = orderingClient.createOrder(new CreateOrderRequest(email, "No items", List.of()));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(400);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("validation_error");
    });
  }

  @Test(description = "TC027: Cancel order returns cancelled state and notes", groups = {"ordering", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Order Cancellation")
  public void cancelOrderReturnsCancelledAndAppendsReason() {
    String email = uniqueEmail("cancel");
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Cancel Contract Product", 6, true, "11.00"));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Cancel Contract Customer", "STANDARD"));
    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Original order note",
        List.of(new CreateOrderItemRequest(sku, 2))));

    Response response = orderingClient.cancelOrder(createOrder.jsonPath().getString("orderNumber"), "customer changed mind");

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("status")).as("Status").isEqualTo("CANCELLED");
      soft.assertThat(response.jsonPath().getString("notes")).as("Notes").contains("customer changed mind");
    });
  }

  @Test(description = "TC028: Cancelling an already cancelled order returns 409", groups = {"ordering", "contract", "negative"})
  @TestMetadata(severity = NORMAL, story = "Order Cancellation")
  public void cancelOrderRejectsSecondCancellation() {
    String email = uniqueEmail("cancel-twice");
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Cancel Twice Product", 6, true, "18.00"));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Cancel Twice Customer", "STANDARD"));
    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Cancel twice order",
        List.of(new CreateOrderItemRequest(sku, 1))));

    String orderNumber = createOrder.jsonPath().getString("orderNumber");
    orderingClient.cancelOrder(orderNumber, "first cancellation");
    Response response = orderingClient.cancelOrder(orderNumber, "second cancellation");

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(409);
      soft.assertThat(response.jsonPath().getString("errorCode")).as("Error code").isEqualTo("order_already_cancelled");
    });
  }

  private CreateProductRequest productRequest(
      String sku,
      String name,
      int inventoryCount,
      boolean active,
      String price) {
    return new CreateProductRequest(
        sku,
        name,
        name + " created for ordering contract testing",
        new BigDecimal(price),
        inventoryCount,
        active);
  }
}
