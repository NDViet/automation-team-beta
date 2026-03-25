package org.ndviet.tests.api.model;

import java.util.List;

public record CreateOrderRequest(
    String customerEmail,
    String notes,
    List<CreateOrderItemRequest> items) {
}
