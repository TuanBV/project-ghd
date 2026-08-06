package com.example.mcprice.exception;

/** Thrown when a domain/business rule blocks an operation (e.g. publish khi chua approve). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) {
        super(message);
    }
}
