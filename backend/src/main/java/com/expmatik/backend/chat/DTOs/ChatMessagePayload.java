package com.expmatik.backend.chat.DTOs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessagePayload(

    @NotBlank
    @Size(max = 1000)
    String content

) {

}
