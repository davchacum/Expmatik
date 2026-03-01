package com.expmatik.backend.auth.DTOs;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthRequestLogin(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String deviceId
) {
}
