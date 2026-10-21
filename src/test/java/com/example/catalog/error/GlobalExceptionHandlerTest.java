package com.example.catalog.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.catalog.product.InvalidProductRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsProductInputErrorToBadRequest() {
        var response = handler.handleInvalidProduct(new InvalidProductRequestException("price", "must be positive"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().fieldErrors()).containsEntry("price", "must be positive");
    }

    @Test
    void mapsNotFoundStatusException() {
        var response = handler.handleStatus(new ResponseStatusException(HttpStatus.NOT_FOUND, "Product missing"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("Product missing");
    }

    @Test
    void mapsMalformedRequestToBadRequest() {
        var exception = new HttpMessageNotReadableException("bad json");
        var response = handler.handleMalformedRequest(exception, request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsOptimisticLockToConflict() {
        var response = handler.handleConflict(new OptimisticLockingFailureException("stale"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void hidesUnexpectedExceptionDetails() {
        var response = handler.handleUnexpected(new IllegalStateException("secret db detail"), request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).doesNotContain("secret db detail");
    }

    private jakarta.servlet.http.HttpServletRequest request() {
        org.springframework.mock.web.MockHttpServletRequest request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setRequestURI("/api/v1/products/1");
        return request;
    }
}
