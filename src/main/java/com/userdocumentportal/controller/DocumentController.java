package com.userdocumentportal.controller;

import com.userdocumentportal.entity.Document;
import com.userdocumentportal.entity.Property;
import com.userdocumentportal.repository.DocumentRepository;
import com.userdocumentportal.repository.PropertyRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class DocumentController {

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    PropertyRepository propertyRepository;

    // Get all documents for a property
    @GetMapping("/properties/{propertyId}/documents")
    public ResponseEntity<List<DocumentDto>> getDocumentsByProperty(@PathVariable Long propertyId) {
        List<Document> docs = documentRepository.findByPropertyId(propertyId);
        List<DocumentDto> dtos = docs.stream().map(d -> new DocumentDto(
                d.getId(),
                d.getName(),
                d.getCategory(),
                d.getSize(),
                d.getUploadDate(),
                d.getStatus(),
                propertyId
        )).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Get all documents globally
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentDto>> getAllDocuments() {
        List<Document> docs = documentRepository.findAll();
        List<DocumentDto> dtos = docs.stream().map(d -> new DocumentDto(
                d.getId(),
                d.getName(),
                d.getCategory(),
                d.getSize(),
                d.getUploadDate(),
                d.getStatus(),
                d.getProperty() != null ? d.getProperty().getId() : null
        )).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // Upload document for a property
    @PostMapping("/properties/{propertyId}/documents")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long propertyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {
        try {
            Property property = propertyRepository.findById(propertyId)
                    .orElseThrow(() -> new RuntimeException("Property not found with id: " + propertyId));

            // Validate file name and extension (PDF or Word only)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) {
                return ResponseEntity.badRequest().body("Uploaded file name is invalid.");
            }
            String lowerName = originalFilename.toLowerCase();
            if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".doc") && !lowerName.endsWith(".docx")) {
                return ResponseEntity.badRequest().body("Only Word documents (.doc, .docx) and PDF files (.pdf) are allowed.");
            }

            // Format size
            long sizeInBytes = file.getSize();
            String sizeStr;
            if (sizeInBytes >= 1024 * 1024) {
                sizeStr = String.format("%.1f MB", (double) sizeInBytes / (1024 * 1024));
            } else {
                sizeStr = String.format("%d KB", sizeInBytes / 1024);
            }

            Document doc = Document.builder()
                    .name(originalFilename)
                    .category(category)
                    .size(sizeStr)
                    .uploadDate(LocalDate.now())
                    .status("Pending")
                    .data(file.getBytes())
                    .property(property)
                    .build();

            documentRepository.save(doc);
            return ResponseEntity.ok().body("Document uploaded successfully!");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to upload document: " + e.getMessage());
        }
    }

    // Download document content
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getName() + "\"")
                .body(doc.getData());
    }

    // Delete a document
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Document not found with id: " + id));
        documentRepository.delete(doc);
        return ResponseEntity.ok().body("Document deleted successfully!");
    }

    // DTO Helper
    public static class DocumentDto {
        public Long id;
        public String name;
        public String category;
        public String size;
        public LocalDate date;
        public String status;
        public Long propertyId;

        public DocumentDto(Long id, String name, String category, String size, LocalDate date, String status, Long propertyId) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.size = size;
            this.date = date;
            this.status = status;
            this.propertyId = propertyId;
        }
    }
}
