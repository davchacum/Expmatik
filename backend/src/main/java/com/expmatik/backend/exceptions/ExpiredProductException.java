package com.expmatik.backend.exceptions;

public class ExpiredProductException extends ConflictException {
    public ExpiredProductException(String message) {
        super(message);
    }
}