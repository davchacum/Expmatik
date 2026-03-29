package com.expmatik.backend.exceptions;

public class SlotBlockedException extends ConflictException {
    public SlotBlockedException(String message) {
        super(message);
    }
}