package com.example.catalog.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> PATCH_FIELDS = Set.of("name", "description", "category", "price", "stockQuantity");
    private final ProductService service;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public ProductController(ProductService service, ObjectMapper objectMapper, Validator validator) {
        this.service = service;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping
    public Page<ProductResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id,asc") String sort) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be non-negative and size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (minPrice != null && minPrice.signum() < 0 || maxPrice != null && maxPrice.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Prices must be non-negative");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "minPrice must be less than or equal to maxPrice");
        }

        Sort sortSpec = parseSort(sort);
        Pageable pageable = PageRequest.of(page, size, sortSpec);
        return service.list(category, minPrice, maxPrice, pageable);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}")
    public ProductResponse patch(@PathVariable Long id, @RequestBody JsonNode body) {
        if (!body.isObject() || body.isEmpty()) {
            throw new InvalidProductRequestException("request", "must be a non-empty JSON object");
        }
        Set<String> unknownFields = new HashSet<>();
        body.fieldNames().forEachRemaining(field -> {
            if (!PATCH_FIELDS.contains(field)) unknownFields.add(field);
        });
        if (!unknownFields.isEmpty()) {
            throw new InvalidProductRequestException("request", "contains unknown fields: " + unknownFields);
        }
        body.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNull()) {
                throw new InvalidProductRequestException(entry.getKey(), "must not be null when supplied");
            }
        });
        try {
            ProductPatchRequest request = objectMapper.treeToValue(body, ProductPatchRequest.class);
            for (ConstraintViolation<ProductPatchRequest> violation : validator.validate(request)) {
                throw new InvalidProductRequestException(violation.getPropertyPath().toString(), violation.getMessage());
            }
            return service.patch(id, request, body);
        } catch (IllegalArgumentException exception) {
            throw new InvalidProductRequestException("request", "contains an invalid value or unknown field");
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new InvalidProductRequestException("request", "contains an invalid value");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    private Sort parseSort(String sort) {
        String[] parts = sort.split(",", -1);
        if (parts.length > 2 || !parts[0].matches("id|name|category|price|createdAt|updatedAt")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported sort field");
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 1 ? Sort.Direction.ASC : Sort.Direction.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort direction must be asc or desc");
        }
        return Sort.by(direction, parts[0]).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
