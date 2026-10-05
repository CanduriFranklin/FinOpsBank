package com.finopsbank.exceptions;

/**
 * Exception thrown when an account has insufficient funds for an operation.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
