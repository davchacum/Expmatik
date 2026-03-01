package com.expmatik.backend.auth.DTOs;

import com.expmatik.backend.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuthRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6) String password,
    @NotBlank String deviceId,
    @NotNull Role role,
    @NotBlank String firstName,
    @NotBlank String lastName) {
}
