package org.ndviet.tests.api.model;

import java.math.BigDecimal;

public record UpdateProductRequest(
    String name,
    String description,
    BigDecimal price,
    boolean active) {
}
