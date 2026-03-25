package org.ndviet.tests.e2e;

import static com.platform.testframework.annotation.TestMetadata.Severity.BLOCKER;
import static com.platform.testframework.annotation.TestMetadata.Severity.CRITICAL;

import com.platform.testframework.annotation.TestMetadata;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.util.List;
import org.ndviet.tests.api.client.CatalogClient;
import org.ndviet.tests.api.client.GraphQlClient;
import org.ndviet.tests.api.client.OrderingClient;
import org.ndviet.tests.api.model.CreateCustomerRequest;
import org.ndviet.tests.api.model.CreateOrderItemRequest;
import org.ndviet.tests.api.model.CreateOrderRequest;
import org.ndviet.tests.api.model.CreateProductRequest;
import org.ndviet.tests.base.BaseApiTest;
import org.testng.annotations.Test;

@TestMetadata(owner = "automation-team-beta", feature = "API End-to-End Workflows")
public class ApiEndToEndWorkflowTest extends BaseApiTest {

  private final CatalogClient catalogClient = new CatalogClient();
  private final OrderingClient orderingClient = new OrderingClient();
  private final GraphQlClient graphQlClient = new GraphQlClient();

  @Test(description = "TC035: Full product-to-order workflow is visible across REST and GraphQL", groups = {"e2e", "smoke"})
  @TestMetadata(severity = BLOCKER, story = "Cross-API Order Flow")
  public void shouldCreateProductCustomerAndPlaceOrderThenVerifyViaGraphQl() {
    String sku = uniqueSku();
    String email = uniqueEmail("e2e-flow");
    catalogClient.createProduct(productRequest(sku, "E2E Flow Product", 9, true, "16.00"));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "E2E Flow Customer", "STANDARD"));

    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "E2E flow order",
        List.of(new CreateOrderItemRequest(sku, 3))));
    String orderNumber = createOrder.jsonPath().getString("orderNumber");

    Response orderQuery = graphQlClient.queryOrderByNumber(orderNumber);
    Response productLookup = catalogClient.getProductBySku(sku);
    Integer inventoryCount = productLookup.jsonPath().getObject("inventoryCount", Integer.class);

    softly(soft -> {
      soft.assertThat(createOrder.statusCode()).as("Order create status | body=%s", createOrder.asString()).isEqualTo(200);
      soft.assertThat(orderQuery.jsonPath().getString("data.orderByNumber.customer.email")).as("GraphQL customer email")
          .isEqualTo(email);
      soft.assertThat(orderQuery.jsonPath().getInt("data.orderByNumber.items[0].quantity")).as("Ordered quantity")
          .isEqualTo(3);
      soft.assertThat(inventoryCount).as("Remaining inventory | body=%s", productLookup.asString()).isEqualTo(6);
    });
  }

  @Test(description = "TC036: Multi-item order updates totals and inventories consistently", groups = {"e2e"})
  @TestMetadata(severity = CRITICAL, story = "Multi-Item Ordering")
  public void shouldPlaceMultiItemOrderAndVerifyTotalsAndInventories() {
    String email = uniqueEmail("multi");
    String skuOne = uniqueSku();
    String skuTwo = uniqueSku();
    catalogClient.createProduct(productRequest(skuOne, "Multi Item One", 10, true, "5.50"));
    catalogClient.createProduct(productRequest(skuTwo, "Multi Item Two", 8, true, "7.25"));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Multi Item Customer", "GOLD"));

    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Multi item order",
        List.of(
            new CreateOrderItemRequest(skuOne, 2),
            new CreateOrderItemRequest(skuTwo, 3))));

    Response productOne = catalogClient.getProductBySku(skuOne);
    Response productTwo = catalogClient.getProductBySku(skuTwo);

    softly(soft -> {
      soft.assertThat(createOrder.statusCode()).as("Order status | body=%s", createOrder.asString()).isEqualTo(200);
      soft.assertThat(createOrder.jsonPath().getFloat("totalAmount")).as("Total amount").isEqualTo(32.75f);
      soft.assertThat(productOne.jsonPath().getObject("inventoryCount", Integer.class)).as("SKU one inventory").isEqualTo(8);
      soft.assertThat(productTwo.jsonPath().getObject("inventoryCount", Integer.class)).as("SKU two inventory").isEqualTo(5);
    });
  }

  @Test(description = "TC037: Cancelled order restores inventory across REST and GraphQL views", groups = {"e2e"})
  @TestMetadata(severity = CRITICAL, story = "Order Cancellation")
  public void shouldCancelOrderAndRestoreInventoryAcrossRestAndGraphQl() {
    String email = uniqueEmail("restore");
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "Restock Product", 4, true, "12.00"));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Restock Customer", "STANDARD"));
    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Restock order",
        List.of(new CreateOrderItemRequest(sku, 2))));

    String orderNumber = createOrder.jsonPath().getString("orderNumber");
    Response cancelResponse = orderingClient.cancelOrder(orderNumber, "inventory restoration");
    Response graphQlOrder = graphQlClient.queryOrderByNumber(orderNumber);
    Response productResponse = catalogClient.getProductBySku(sku);

    softly(soft -> {
      soft.assertThat(cancelResponse.statusCode()).as("Cancel status | body=%s", cancelResponse.asString()).isEqualTo(200);
      soft.assertThat(graphQlOrder.jsonPath().getString("data.orderByNumber.status")).as("GraphQL status").isEqualTo("CANCELLED");
      soft.assertThat(productResponse.jsonPath().getObject("inventoryCount", Integer.class)).as("Restored inventory").isEqualTo(4);
    });
  }

  @Test(description = "TC038: Inactive products stay out of active listings while remaining directly addressable", groups = {"e2e"})
  @TestMetadata(severity = CRITICAL, story = "Catalog Visibility")
  public void shouldKeepInactiveProductOutOfActiveListingsWhileAccessibleBySku() {
    String sku = uniqueSku();
    String token = "inactive-" + sku.substring(4);
    catalogClient.createProduct(productRequest(sku, token + " Product", 3, true, "8.50"));
    catalogClient.setProductActivation(sku, false);

    Response activeList = catalogClient.listProducts(true, token);
    Response lookup = catalogClient.getProductBySku(sku);
    List<String> skus = activeList.jsonPath().getList("sku", String.class);

    softly(soft -> {
      soft.assertThat(activeList.statusCode()).as("Active list status | body=%s", activeList.asString()).isEqualTo(200);
      soft.assertThat(skus).as("Active listing SKUs").doesNotContain(sku);
      soft.assertThat(lookup.jsonPath().getBoolean("active")).as("Direct lookup active flag").isFalse();
    });
  }

  @Test(description = "TC039: Customer tier changes remain visible through later order flow", groups = {"e2e"})
  @TestMetadata(severity = CRITICAL, story = "Customer Lifecycle")
  public void shouldUpgradeCustomerTierAndUseItInSubsequentOrderLifecycle() {
    String email = uniqueEmail("tier-e2e");
    String sku = uniqueSku();
    orderingClient.createCustomer(new CreateCustomerRequest(email, "Tier Lifecycle Customer", "STANDARD"));
    orderingClient.updateCustomerTier(email, "ENTERPRISE");
    catalogClient.createProduct(productRequest(sku, "Tier Lifecycle Product", 5, true, "20.00"));

    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "Tier lifecycle order",
        List.of(new CreateOrderItemRequest(sku, 1))));
    Response customerQuery = graphQlClient.queryCustomerByEmail(email);

    softly(soft -> {
      soft.assertThat(createOrder.statusCode()).as("Order status | body=%s", createOrder.asString()).isEqualTo(200);
      soft.assertThat(customerQuery.jsonPath().getString("data.customerByEmail.tier")).as("GraphQL tier")
          .isEqualTo("ENTERPRISE");
      soft.assertThat(createOrder.jsonPath().getString("customer.tier")).as("Order customer tier").isEqualTo("ENTERPRISE");
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
        name + " created for end-to-end testing",
        new BigDecimal(price),
        inventoryCount,
        active);
  }
}
