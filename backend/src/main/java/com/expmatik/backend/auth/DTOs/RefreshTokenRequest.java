package com.expmatik.backend.auth.DTOs;

import jakarta.validation.constraints.NotNull;

public record RefreshTokenRequest(@NotNull String deviceId) {
}



