package com.example.catalog.product;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.catalog.error.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ProductControllerTest {
    private ProductService service;
    private MockMvc mvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        service = org.mockito.Mockito.mock(ProductService.class,
                org.mockito.Mockito.withSettings().mockMaker("mock-maker-subclass"));
        objectMapper = new ObjectMapper();
        mvc = MockMvcBuilders.standaloneSetup(new ProductController(service, objectMapper,
                        Validation.buildDefaultValidatorFactory().getValidator()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createReturnsCreated() throws Exception {
        when(service.create(any(ProductRequest.class))).thenReturn(response());

        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Runner","description":"Road shoe","category":"Shoes","price":99.99,"stockQuantity":3}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Runner"));
    }

    @Test
    void createRejectsInvalidBodyWithFieldErrors() throws Exception {
        mvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":" ","description":"Road shoe","category":"Shoes","price":-2,"stockQuantity":3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void getReturnsProduct() throws Exception {
        when(service.get(1L)).thenReturn(response());
        mvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void listBuildsFilteredSortedPage() throws Exception {
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "price").and(Sort.by("id")));
        when(service.list(eq("Shoes"), eq(new BigDecimal("10")), eq(new BigDecimal("150")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(response()), pageable, 1));

        mvc.perform(get("/api/v1/products?category=Shoes&minPrice=10&maxPrice=150&size=10&sort=price,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Runner"));
        verify(service).list(eq("Shoes"), eq(new BigDecimal("10")), eq(new BigDecimal("150")), eq(pageable));
    }

    @Test
    void listRejectsInvalidPageSize() throws Exception {
        mvc.perform(get("/api/v1/products?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.path").value("/api/v1/products"));
    }

    @Test
    void listRejectsUnsupportedSort() throws Exception {
        mvc.perform(get("/api/v1/products?sort=unknown,asc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateReturnsUpdatedProduct() throws Exception {
        when(service.update(eq(1L), any(ProductRequest.class))).thenReturn(response());
        mvc.perform(put("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Runner","description":"Road shoe","category":"Shoes","price":99.99,"stockQuantity":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Runner"));
    }

    @Test
    void patchAllowsOnlySuppliedField() throws Exception {
        when(service.patch(eq(1L), any(ProductPatchRequest.class), any()))
                .thenReturn(response());
        mvc.perform(patch("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"price\": 99.99}"))
                .andExpect(status().isOk());
    }

    @Test
    void patchRejectsNullUnknownAndEmptyBodies() throws Exception {
        for (String body : List.of("{\"price\":null}", "{\"unknown\":1}", "{}")) {
            mvc.perform(patch("/api/v1/products/1").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors").exists());
        }
    }

    @Test
    void deleteReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/v1/products/1"))
                .andExpect(status().isNoContent());
    }

    private ProductResponse response() {
        return new ProductResponse(1L, "Runner", "Road shoe", "Shoes", new BigDecimal("99.99"),
                3, null, null);
    }
}
