package com.userdocumentportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {
    @NotBlank
    private String credential;

    private String role; // Optional: Used to register as OWNER or TENANT if the user is new
}
