package com.expmatik.backend.auth.DTOs;

public record AuthResponse(
        String token,
        String role
) {
}



