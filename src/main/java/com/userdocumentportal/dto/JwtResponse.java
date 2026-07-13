package com.userdocumentportal.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String companyName;
    private String fullName;
    private String role;

    public JwtResponse(String token, Long id, String email, String companyName, String fullName, String role) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.companyName = companyName;
        this.fullName = fullName;
        this.role = role;
    }
}
