package org.ndviet.tests.api.model;

import java.math.BigDecimal;

public record CreateProductRequest(
    String sku,
    String name,
    String description,
    BigDecimal price,
    int inventoryCount,
    boolean active) {
}
