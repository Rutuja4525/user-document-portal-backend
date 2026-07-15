package com.userdocumentportal.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateCompanyRequest {
    @NotBlank
    private String companyName;
}
