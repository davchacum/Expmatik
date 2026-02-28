package com.expmatik.backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponse {
    
    private String message;
    private boolean success;
    private String email;
    
    public LogoutResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}
