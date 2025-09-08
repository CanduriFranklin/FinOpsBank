package com.finopsbank.exceptions;

/**
 * Exception thrown for invalid operations.
 */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}
