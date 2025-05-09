package com.nurbb.libris.exception;

public class QuotasFullException extends RuntimeException {
    public QuotasFullException(String message) {
        super(message);
    }
}