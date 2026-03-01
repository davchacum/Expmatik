package com.expmatik.backend.auth.DTOs;

public record AuthResult(
        String accessToken,
        String refreshToken,
        String role
) {
}



