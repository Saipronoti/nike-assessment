package com.example.catalog.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

/** Fields omitted from a PATCH request are left unchanged. Explicit nulls are rejected by the controller. */
public record ProductPatchRequest(
        @Size(max = 160) String name,
        @Size(max = 4000) String description,
        @Size(max = 80) String category,
        @DecimalMin(value = "0.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
        @PositiveOrZero Integer stockQuantity
) { }
