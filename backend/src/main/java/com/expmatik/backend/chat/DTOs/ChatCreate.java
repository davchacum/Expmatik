package com.expmatik.backend.chat.DTOs;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChatCreate(

    @NotBlank
    @Size(max = 1000)
    String content,

    @NotNull
    UUID maintenanceId

) {

}
