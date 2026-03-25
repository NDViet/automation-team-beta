package org.ndviet.tests.api.client;

import io.restassured.response.Response;
import org.ndviet.library.RestAssured;
import org.ndviet.tests.api.model.ProductActivationRequest;
import org.ndviet.tests.api.model.AdjustInventoryRequest;
import org.ndviet.tests.api.model.CreateProductRequest;
import org.ndviet.tests.api.model.UpdateProductRequest;

public class CatalogClient {

  public Response listProducts() {
    return RestAssured.get("/api/v1/products");
  }

  public Response listProducts(Boolean activeOnly, String query) {
    var request = RestAssured.given();
    if (activeOnly != null) {
      request.queryParam("activeOnly", activeOnly);
    }
    if (query != null && !query.isBlank()) {
      request.queryParam("query", query);
    }
    return request.when().get("/api/v1/products");
  }

  public Response listLowStockProducts(int threshold, String query) {
    var request = RestAssured.given().queryParam("threshold", threshold);
    if (query != null && !query.isBlank()) {
      request.queryParam("query", query);
    }
    return request.when().get("/api/v1/products/low-stock");
  }

  public Response getProductBySku(String sku) {
    return RestAssured.get("/api/v1/products/" + sku);
  }

  public Response createProduct(CreateProductRequest request) {
    return RestAssured.post("/api/v1/products", request);
  }

  public Response updateProduct(String sku, UpdateProductRequest request) {
    return RestAssured.put("/api/v1/products/" + sku, request);
  }

  public Response adjustInventory(String sku, int delta) {
    return RestAssured.patch("/api/v1/products/" + sku + "/inventory", new AdjustInventoryRequest(delta));
  }

  public Response setProductActivation(String sku, boolean active) {
    return RestAssured.patch("/api/v1/products/" + sku + "/activation", new ProductActivationRequest(active));
  }
}
