package com.expmatik.backend.auth.DTOs;

public record ValidateTokenResponse(boolean authenticated, String message) {
}



