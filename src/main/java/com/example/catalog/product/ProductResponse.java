package com.example.catalog.product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(Long id, String name, String description, String category,
                              BigDecimal price, Integer stockQuantity,
                              Instant createdAt, Instant updatedAt) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getCategory(), product.getPrice(), product.getStockQuantity(),
                product.getCreatedAt(), product.getUpdatedAt());
    }
}
