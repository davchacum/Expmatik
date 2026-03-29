package com.expmatik.backend.exceptions;

public class OutOfStockException extends ConflictException {
    public OutOfStockException(String message) {
        super(message);
    }
}