package com.example.catalog.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ProductServiceTest {
    private ProductRepository repository;
    private int saveCount;
    private java.util.Map<Long, Product> products;
    private ProductService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(ProductRepository.class, Mockito.withSettings().mockMaker("mock-maker-subclass"));
        products = new java.util.HashMap<>();
        saveCount = 0;
        when(repository.save(any(Product.class))).thenAnswer(invocation -> {
            saveCount++;
            return invocation.getArgument(0);
        });
        when(repository.findById(any(Long.class))).thenAnswer(invocation ->
                Optional.ofNullable(products.get(invocation.getArgument(0))));
        org.mockito.Mockito.doAnswer(invocation -> {
            products.values().remove(invocation.getArgument(0));
            return null;
        }).when(repository).delete(any(Product.class));
        service = new ProductService(repository, Validation.buildDefaultValidatorFactory().getValidator());
        objectMapper = new ObjectMapper();
    }

    @Test
    void createTrimsTextAndSavesProduct() {
        ProductRequest request = new ProductRequest(" Runner ", " Lightweight shoe ", " Shoes ",
                new BigDecimal("99.00"), 4);

        ProductResponse response = service.create(request);

        assertThat(response.name()).isEqualTo("Runner");
        assertThat(response.description()).isEqualTo("Lightweight shoe");
        assertThat(response.category()).isEqualTo("Shoes");
        assertThat(saveCount).isEqualTo(1);
    }

    @Test
    void getReturnsProductWhenFound() {
        products.put(1L, product());
        assertThat(service.get(1L).name()).isEqualTo("Runner");
    }

    @Test
    void getReturnsNotFoundWhenMissing() {
        assertThatThrownBy(() -> service.get(7L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode").isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listNormalizesBlankCategoryAndMapsPage() {
        var pageable = PageRequest.of(0, 10, Sort.by("id"));
        when(repository.search(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(product()), pageable, 1));

        var result = service.list("  ", null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Runner");
        verify(repository).search(null, null, null, pageable);
    }

    @Test
    void updateReplacesFields() {
        products.put(1L, product());

        ProductResponse response = service.update(1L, new ProductRequest("Trail shoe", "New description",
                "Outdoor", new BigDecimal("120.00"), 12));

        assertThat(response.name()).isEqualTo("Trail shoe");
        assertThat(response.price()).isEqualByComparingTo("120.00");
        assertThat(response.stockQuantity()).isEqualTo(12);
    }

    @Test
    void patchChangesOnlyProvidedFields() throws Exception {
        products.put(1L, product());
        var body = objectMapper.readTree("{\"price\": 105.50}");

        ProductResponse response = service.patch(1L,
                objectMapper.treeToValue(body, ProductPatchRequest.class), body);

        assertThat(response.name()).isEqualTo("Runner");
        assertThat(response.category()).isEqualTo("Shoes");
        assertThat(response.price()).isEqualByComparingTo("105.50");
        assertThat(response.stockQuantity()).isEqualTo(8);
    }

    @Test
    void patchRejectsExplicitNullAndDoesNotSave() throws Exception {
        products.put(1L, product());
        var body = objectMapper.readTree("{\"price\": null}");

        assertThatThrownBy(() -> service.patch(1L,
                objectMapper.treeToValue(body, ProductPatchRequest.class), body))
                .isInstanceOf(InvalidProductRequestException.class)
                .hasMessageContaining("must not be null");
        assertThat(saveCount).isZero();
    }

    @Test
    void patchRejectsInvalidWholeProductAndDoesNotSave() throws Exception {
        products.put(1L, product());
        var body = objectMapper.readTree("{\"name\": \"   \"}");

        assertThatThrownBy(() -> service.patch(1L,
                objectMapper.treeToValue(body, ProductPatchRequest.class), body))
                .isInstanceOf(InvalidProductRequestException.class);
        assertThat(saveCount).isZero();
    }

    @Test
    void deleteRemovesExistingProduct() {
        products.put(1L, product());
        service.delete(1L);
        assertThat(products).isEmpty();
    }

    private Product product() {
        return new Product("Runner", "Lightweight shoe", "Shoes", new BigDecimal("99.00"), 8);
    }
}
