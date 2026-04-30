package com.expmatik.backend.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class PredictionNotAvailableException extends RuntimeException {
    public PredictionNotAvailableException(String message) {
        super(message);
    }
}
