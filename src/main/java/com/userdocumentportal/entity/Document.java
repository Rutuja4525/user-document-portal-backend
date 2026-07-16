package com.userdocumentportal.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String category; // Lease, Identity, Payment Receipt, etc.
    private String size;
    private LocalDate uploadDate;
    private String status; // Approved, Pending, Rejected

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "s3_key", nullable = false)
    private String s3Key;

    @Column(name = "processed_s3_key")
    private String processedS3Key;

    @Column(name = "processing_status")
    private String processingStatus; // PENDING, PROCESSING, COMPLETED, FAILED, NOT_APPLICABLE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;
}
