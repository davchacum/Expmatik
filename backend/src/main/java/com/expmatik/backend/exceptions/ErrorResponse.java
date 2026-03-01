package com.expmatik.backend.exceptions;

import java.util.Date;

public record ErrorResponse(
        String message,
        int statusCode,
        Date date
) {
}



