package com.example.catalog.product;

import java.math.BigDecimal;
import org.springframework.data.jpa.domain.Specification;

/** Builds only the predicates for filters that were supplied by the client. */
public final class ProductSpecifications {
    private ProductSpecifications() { }

    public static Specification<Product> withFilters(String category, BigDecimal minPrice, BigDecimal maxPrice) {
        Specification<Product> specification = (root, query, builder) -> builder.conjunction();

        if (category != null && !category.isBlank()) {
            String normalizedCategory = category.trim().toLowerCase(java.util.Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("category")), normalizedCategory));
        }
        if (minPrice != null) {
            specification = specification.and((root, query, builder) ->
                    builder.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            specification = specification.and((root, query, builder) ->
                    builder.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        return specification;
    }
}
