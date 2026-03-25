package org.ndviet.tests.api.model;

public record CreateCustomerRequest(
    String email,
    String fullName,
    String tier) {
}
