package org.ndviet.tests.api.model;

public record CreateOrderItemRequest(
    String sku,
    int quantity) {
}
