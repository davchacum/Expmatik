package com.expmatik.backend.user.DTOs;

import com.expmatik.backend.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateUser(
        @NotNull @Email String email,
        @NotNull @Size(min = 4) String password,
        @NotNull Role role,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName
) {
}



