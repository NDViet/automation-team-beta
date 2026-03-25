package org.ndviet.tests.ordering;

import static com.platform.testframework.annotation.TestMetadata.Severity.BLOCKER;
import static com.platform.testframework.annotation.TestMetadata.Severity.CRITICAL;

import com.platform.testframework.annotation.TestMetadata;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.ndviet.tests.api.client.CatalogClient;
import org.ndviet.tests.api.client.GraphQlClient;
import org.ndviet.tests.api.client.OrderingClient;
import org.ndviet.tests.api.model.CreateCustomerRequest;
import org.ndviet.tests.api.model.CreateOrderItemRequest;
import org.ndviet.tests.api.model.CreateOrderRequest;
import org.ndviet.tests.api.model.CreateProductRequest;
import org.ndviet.tests.base.BaseApiTest;
import org.testng.annotations.Test;

@TestMetadata(owner = "automation-team-beta", feature = "Ordering")
public class OrderingWorkflowTest extends BaseApiTest {

  private final CatalogClient catalogClient = new CatalogClient();
  private final OrderingClient orderingClient = new OrderingClient();
  private final GraphQlClient graphQlClient = new GraphQlClient();

  @Test(description = "TC005: Create customer and place order through REST", groups = {"smoke", "ordering"})
  @TestMetadata(severity = BLOCKER, story = "Order Placement")
  public void createCustomerAndPlaceOrder() {
    String email = "qa-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    catalogClient.createProduct(new CreateProductRequest(
        sku,
        "Orderable Product",
        "Product reserved for ordering workflow validation",
        new BigDecimal("15.50"),
        6,
        true));

    log.step("Create customer");
    Response customerResponse = orderingClient.createCustomer(
        new CreateCustomerRequest(email, "API Customer", "GOLD"));
    softly(soft -> {
      soft.assertThat(customerResponse.statusCode()).as("Customer create status").isEqualTo(200);
      soft.assertThat(customerResponse.jsonPath().getString("email")).as("Customer email").isEqualTo(email);
    });
    env("ordering.customer.email", email);
    log.endStep();

    log.step("Place order");
    Response orderResponse = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Created during API ordering workflow",
        List.of(new CreateOrderItemRequest(sku, 2))));
    String orderNumber = orderResponse.jsonPath().getString("orderNumber");
    softly(soft -> {
      soft.assertThat(orderResponse.statusCode()).as("Order create status").isEqualTo(200);
      soft.assertThat(orderResponse.jsonPath().getString("status")).as("Order status").isEqualTo("CONFIRMED");
      soft.assertThat(orderNumber).as("Order number").isNotBlank();
    });
    env("ordering.order.number", orderNumber);
    log.endStep();

    log.step("Verify inventory was reserved");
    Response productResponse = catalogClient.getProductBySku(sku);
    Integer inventoryAfterOrder = productResponse.jsonPath().getObject("inventoryCount", Integer.class);
    softly(soft -> {
      soft.assertThat(productResponse.statusCode())
          .as("Product lookup status | body=%s", productResponse.asString())
          .isEqualTo(200);
      soft.assertThat(inventoryAfterOrder)
          .as("Inventory after order | body=%s", productResponse.asString())
          .isEqualTo(4);
    });
    log.endStep();
  }

  @Test(description = "TC006: Query placed order via GraphQL", groups = {"ordering", "graphql"})
  @TestMetadata(severity = CRITICAL, story = "Order Query")
  public void queryPlacedOrderViaGraphQl() {
    String email = "graphql-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    String sku = "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

    catalogClient.createProduct(new CreateProductRequest(
        sku,
        "GraphQL Order Product",
        "Product for GraphQL order verification",
        new BigDecimal("21.00"),
        9,
        true));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "GraphQL Customer", "STANDARD"));
    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "GraphQL verification order",
        List.of(new CreateOrderItemRequest(sku, 3))));
    String orderNumber = createOrder.jsonPath().getString("orderNumber");

    log.step("Query the created order via GraphQL");
    Response graphQlResponse = graphQlClient.queryOrderByNumber(orderNumber);
    softly(soft -> {
      soft.assertThat(graphQlResponse.statusCode()).as("GraphQL status").isEqualTo(200);
      soft.assertThat(graphQlResponse.jsonPath().getString("data.orderByNumber.orderNumber")).as("Order number").isEqualTo(orderNumber);
      soft.assertThat(graphQlResponse.jsonPath().getString("data.orderByNumber.customer.email")).as("Customer email").isEqualTo(email);
      soft.assertThat(graphQlResponse.jsonPath().getInt("data.orderByNumber.items[0].quantity")).as("Ordered quantity").isEqualTo(3);
    });
    log.endStep();
  }
}
