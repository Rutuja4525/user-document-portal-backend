package com.userdocumentportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDto {
    private Long id;
    private String name;
    private String category;
    private String size;
    private LocalDate date;
    private String status;
    private Long userId;
    private String s3Key;
    private String processedS3Key;
    private String contentType;

    // Constructor matching original DocumentDto for backwards compatibility
    public DocumentDto(Long id, String name, String category, String size, LocalDate date, String status, Long userId) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.size = size;
        this.date = date;
        this.status = status;
        this.userId = userId;
    }
}
