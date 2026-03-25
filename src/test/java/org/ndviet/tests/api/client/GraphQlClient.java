package org.ndviet.tests.api.client;

import io.restassured.response.Response;
import java.util.LinkedHashMap;
import java.util.Map;
import org.ndviet.library.RestAssured;
import org.ndviet.tests.api.model.UpdateProductRequest;

public class GraphQlClient {

  public Response queryHealth() {
    return RestAssured.graphQl("query { health }");
  }

  public Response queryProductBySku(String sku) {
    return RestAssured.graphQl(
        "query($sku:String!){ productBySku(sku:$sku){ sku name inventoryCount active } }",
        Map.of("sku", sku));
  }

  public Response queryProducts(Boolean activeOnly, String query) {
    return RestAssured.graphQl(
        "query($activeOnly:Boolean,$query:String){ products(activeOnly:$activeOnly, query:$query){ sku name inventoryCount active } }",
        variables("activeOnly", activeOnly, "query", query));
  }

  public Response queryLowStockProducts(int threshold, String query) {
    return RestAssured.graphQl(
        "query($threshold:Int!,$query:String){ lowStockProducts(threshold:$threshold, query:$query){ sku inventoryCount active } }",
        variables("threshold", threshold, "query", query));
  }

  public Response mutateUpdateProduct(String sku, UpdateProductRequest request) {
    return RestAssured.graphQl(
        "mutation($sku:String!,$input:UpdateProductInput!){ updateProduct(sku:$sku, input:$input){ sku name description price active } }",
        variables("sku", sku, "input", request));
  }

  public Response mutateSetProductActivation(String sku, boolean active) {
    return RestAssured.graphQl(
        "mutation($sku:String!,$active:Boolean!){ setProductActivation(sku:$sku, active:$active){ sku active } }",
        variables("sku", sku, "active", active));
  }

  public Response queryCustomerByEmail(String email) {
    return RestAssured.graphQl(
        "query($email:String!){ customerByEmail(email:$email){ email fullName tier } }",
        Map.of("email", email));
  }

  public Response queryCustomers(String tier, String query) {
    return RestAssured.graphQl(
        "query($tier:CustomerTier,$query:String){ customers(tier:$tier, query:$query){ email fullName tier } }",
        variables("tier", tier, "query", query));
  }

  public Response queryOrderByNumber(String orderNumber) {
    return RestAssured.graphQl(
        "query($orderNumber:String!){ orderByNumber(orderNumber:$orderNumber){ orderNumber status totalAmount items { productSku quantity } customer { email } } }",
        Map.of("orderNumber", orderNumber));
  }

  public Response queryOrders(String status, String customerEmail) {
    return RestAssured.graphQl(
        "query($status:OrderStatus,$customerEmail:String){ orders(status:$status, customerEmail:$customerEmail){ orderNumber status customer { email } items { productSku quantity } } }",
        variables("status", status, "customerEmail", customerEmail));
  }

  public Response mutateUpdateCustomerTier(String email, String tier) {
    return RestAssured.graphQl(
        "mutation($email:String!,$tier:CustomerTier!){ updateCustomerTier(email:$email, tier:$tier){ email tier } }",
        variables("email", email, "tier", tier));
  }

  public Response mutateCancelOrder(String orderNumber, String reason) {
    return RestAssured.graphQl(
        "mutation($orderNumber:String!,$reason:String){ cancelOrder(orderNumber:$orderNumber, reason:$reason){ orderNumber status notes items { productSku quantity } } }",
        variables("orderNumber", orderNumber, "reason", reason));
  }

  private Map<String, Object> variables(Object... values) {
    Map<String, Object> variables = new LinkedHashMap<>();
    for (int index = 0; index < values.length; index += 2) {
      variables.put(String.valueOf(values[index]), values[index + 1]);
    }
    return variables;
  }
}
