package org.ndviet.tests.api.client;

import io.restassured.response.Response;
import org.ndviet.library.RestAssured;
import org.ndviet.tests.api.model.CancelOrderRequest;
import org.ndviet.tests.api.model.CreateCustomerRequest;
import org.ndviet.tests.api.model.CreateOrderRequest;
import org.ndviet.tests.api.model.CustomerTierUpdateRequest;

public class OrderingClient {

  public Response createCustomer(CreateCustomerRequest request) {
    return RestAssured.post("/api/v1/customers", request);
  }

  public Response listCustomers() {
    return RestAssured.get("/api/v1/customers");
  }

  public Response listCustomers(String tier, String query) {
    var request = RestAssured.given();
    if (tier != null && !tier.isBlank()) {
      request.queryParam("tier", tier);
    }
    if (query != null && !query.isBlank()) {
      request.queryParam("query", query);
    }
    return request.when().get("/api/v1/customers");
  }

  public Response getCustomerByEmail(String email) {
    return RestAssured.given().when().get("/api/v1/customers/{email}", email);
  }

  public Response updateCustomerTier(String email, String tier) {
    return RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(new CustomerTierUpdateRequest(tier))
        .when()
        .patch("/api/v1/customers/{email}/tier", email);
  }

  public Response createOrder(CreateOrderRequest request) {
    return RestAssured.post("/api/v1/orders", request);
  }

  public Response listOrders(String status, String customerEmail) {
    var request = RestAssured.given();
    if (status != null && !status.isBlank()) {
      request.queryParam("status", status);
    }
    if (customerEmail != null && !customerEmail.isBlank()) {
      request.queryParam("customerEmail", customerEmail);
    }
    return request.when().get("/api/v1/orders");
  }

  public Response getOrderByNumber(String orderNumber) {
    return RestAssured.get("/api/v1/orders/" + orderNumber);
  }

  public Response cancelOrder(String orderNumber, String reason) {
    return RestAssured.given()
        .contentType(io.restassured.http.ContentType.JSON)
        .body(new CancelOrderRequest(reason))
        .when()
        .patch("/api/v1/orders/{orderNumber}/cancel", orderNumber);
  }
}
