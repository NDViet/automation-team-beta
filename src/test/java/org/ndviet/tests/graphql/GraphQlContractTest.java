package org.ndviet.tests.graphql;

import static com.platform.testframework.annotation.TestMetadata.Severity.CRITICAL;
import static com.platform.testframework.annotation.TestMetadata.Severity.NORMAL;

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
import org.ndviet.tests.api.model.UpdateProductRequest;
import org.ndviet.tests.base.BaseApiTest;
import org.testng.annotations.Test;

@TestMetadata(owner = "automation-team-beta", feature = "GraphQL Contract")
public class GraphQlContractTest extends BaseApiTest {

  private final CatalogClient catalogClient = new CatalogClient();
  private final OrderingClient orderingClient = new OrderingClient();
  private final GraphQlClient graphQlClient = new GraphQlClient();

  @Test(description = "TC029: GraphQL health query returns UP", groups = {"graphql", "contract"})
  @TestMetadata(severity = NORMAL, story = "Platform Health")
  public void healthQueryReturnsUp() {
    Response response = graphQlClient.queryHealth();

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("data.health")).as("Health").isEqualTo("UP");
      soft.assertThat(response.jsonPath().getList("errors")).as("Errors").isNull();
    });
  }

  @Test(description = "TC030: Product GraphQL query supports filters", groups = {"graphql", "catalog", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Product Query")
  public void productQueriesSupportFilters() {
    String token = "gql-" + uniqueSku().substring(4);
    String activeSku = uniqueSku();
    String inactiveSku = uniqueSku();
    catalogClient.createProduct(productRequest(activeSku, token + " Active", 2, true));
    catalogClient.createProduct(productRequest(inactiveSku, token + " Inactive", 8, false));

    Response response = graphQlClient.queryProducts(true, token);
    List<String> skus = response.jsonPath().getList("data.products.sku", String.class);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(skus).as("GraphQL product SKUs").contains(activeSku);
      soft.assertThat(skus).as("GraphQL product SKUs").doesNotContain(inactiveSku);
    });
  }

  @Test(description = "TC031: Customer GraphQL query returns the expected customer", groups = {"graphql", "ordering", "contract"})
  @TestMetadata(severity = NORMAL, story = "Customer Query")
  public void customerByEmailQueryReturnsContract() {
    String email = uniqueEmail("graphql-customer");
    orderingClient.createCustomer(new CreateCustomerRequest(email, "GraphQL Customer", "ENTERPRISE"));

    Response response = graphQlClient.queryCustomerByEmail(email);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("data.customerByEmail.email")).as("Email").isEqualTo(email);
      soft.assertThat(response.jsonPath().getString("data.customerByEmail.tier")).as("Tier").isEqualTo("ENTERPRISE");
    });
  }

  @Test(description = "TC032: Orders GraphQL query supports status and customer filters", groups = {"graphql", "ordering", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Order Query")
  public void ordersQuerySupportsFilters() {
    String email = uniqueEmail("graphql-orders");
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "GraphQL Order Product", 5, true));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "GraphQL Order Customer", "STANDARD"));
    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "GraphQL filtered order",
        List.of(new CreateOrderItemRequest(sku, 1))));

    String orderNumber = createOrder.jsonPath().getString("orderNumber");
    Response response = graphQlClient.queryOrders("CONFIRMED", email);
    List<String> orderNumbers = response.jsonPath().getList("data.orders.orderNumber", String.class);

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(orderNumbers).as("GraphQL orders").contains(orderNumber);
    });
  }

  @Test(description = "TC033: Product GraphQL mutation updates mutable fields", groups = {"graphql", "catalog", "contract"})
  @TestMetadata(severity = CRITICAL, story = "Product Mutation")
  public void updateProductMutationReturnsUpdatedProduct() {
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "GraphQL Legacy Product", 4, true));

    Response response = graphQlClient.mutateUpdateProduct(
        sku,
        new UpdateProductRequest("GraphQL Updated Product", "Updated via GraphQL", new BigDecimal("44.90"), false));

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("data.updateProduct.name")).as("Name")
          .isEqualTo("GraphQL Updated Product");
      soft.assertThat(response.jsonPath().getBoolean("data.updateProduct.active")).as("Active").isFalse();
    });
  }

  @Test(description = "TC034: Cancel order GraphQL mutation returns cancelled status", groups = {"graphql", "ordering", "contract"})
  @TestMetadata(severity = NORMAL, story = "Order Mutation")
  public void cancelOrderMutationReturnsCancelledStatus() {
    String email = uniqueEmail("graphql-cancel");
    String sku = uniqueSku();
    catalogClient.createProduct(productRequest(sku, "GraphQL Cancel Product", 7, true));
    orderingClient.createCustomer(new CreateCustomerRequest(email, "GraphQL Cancel Customer", "GOLD"));
    Response createOrder = orderingClient.createOrder(new CreateOrderRequest(
        email,
        "GraphQL cancellation order",
        List.of(new CreateOrderItemRequest(sku, 2))));

    Response response = graphQlClient.mutateCancelOrder(
        createOrder.jsonPath().getString("orderNumber"),
        "graphql cancellation");

    softly(soft -> {
      soft.assertThat(response.statusCode()).as("Status | body=%s", response.asString()).isEqualTo(200);
      soft.assertThat(response.jsonPath().getString("data.cancelOrder.status")).as("Status").isEqualTo("CANCELLED");
      soft.assertThat(response.jsonPath().getString("data.cancelOrder.notes")).as("Notes").contains("graphql cancellation");
    });
  }

  private CreateProductRequest productRequest(String sku, String name, int inventoryCount, boolean active) {
    return new CreateProductRequest(
        sku,
        name,
        name + " created for GraphQL testing",
        new BigDecimal("13.50"),
        inventoryCount,
        active);
  }
}
