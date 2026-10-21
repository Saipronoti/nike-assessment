package com.example.catalog.product;

public class InvalidProductRequestException extends RuntimeException {
    private final String field;

    public InvalidProductRequestException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
