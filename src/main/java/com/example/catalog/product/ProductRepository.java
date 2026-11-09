package com.example.catalog.product;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    default Page<Product> search(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return findAll(ProductSpecifications.withFilters(category, minPrice, maxPrice), pageable);
    }
}
