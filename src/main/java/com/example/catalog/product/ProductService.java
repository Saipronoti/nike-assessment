package com.example.catalog.product;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class ProductService {
    private final ProductRepository repository;

    public ProductService(ProductRepository repository) {
        this.repository = repository;
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name().trim(), request.description().trim(),
                request.category().trim(), request.price(), request.stockQuantity());
        return ProductResponse.from(repository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long id) {
        return ProductResponse.from(find(id));
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return repository.search(category == null || category.isBlank() ? null : category.trim(),
                minPrice, maxPrice, pageable).map(ProductResponse::from);
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = find(id);
        product.update(request.name().trim(), request.description().trim(), request.category().trim(),
                request.price(), request.stockQuantity());
        return ProductResponse.from(repository.save(product));
    }

    public void delete(Long id) {
        repository.delete(find(id));
    }

    private Product find(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found: " + id));
    }
}
