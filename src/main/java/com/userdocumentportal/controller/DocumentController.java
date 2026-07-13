package com.userdocumentportal.controller;

import com.userdocumentportal.dto.DocumentDto;
import com.userdocumentportal.service.DocumentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api")
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    @Autowired
    private DocumentService documentService;

    // Get all documents for a property
    @GetMapping("/properties/{propertyId}/documents")
    public ResponseEntity<List<DocumentDto>> getDocumentsByProperty(@PathVariable Long propertyId) {
        logger.info("Received request to get all documents for property ID: {}", propertyId);
        List<DocumentDto> docs = documentService.getDocumentsByProperty(propertyId);
        logger.info("Successfully retrieved {} documents for property ID: {}", docs.size(), propertyId);
        return ResponseEntity.ok(docs);
    }

    // Get all documents globally
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentDto>> getAllDocuments() {
        logger.info("Received request to list all documents globally");
        List<DocumentDto> docs = documentService.getAllDocuments();
        logger.info("Successfully retrieved {} documents globally", docs.size());
        return ResponseEntity.ok(docs);
    }

    // Upload document for a property
    @PostMapping("/properties/{propertyId}/documents")
    public ResponseEntity<?> uploadDocument(
            @PathVariable Long propertyId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {
        String originalFilename = file.getOriginalFilename();
        logger.info("Received request to upload file: '{}' with category: '{}' for property ID: {}", 
                originalFilename, category, propertyId);
        
        documentService.uploadDocument(propertyId, file, category);
        
        logger.info("Successfully completed upload API request for file: '{}' for property ID: {}", originalFilename, propertyId);
        return ResponseEntity.ok().body("Document uploaded successfully!");
    }

    // Download document content
    @GetMapping("/documents/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long id) {
        logger.info("Received download request for document ID: {}", id);
        
        DocumentDto doc = documentService.getDocumentById(id);
        byte[] fileData = documentService.downloadDocument(id);

        logger.info("Successfully completed download API request for document ID: {}, file name: '{}'", id, doc.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getName() + "\"")
                .body(fileData);
    }

    // Delete a document
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<?> deleteDocument(@PathVariable Long id) {
        logger.info("Received delete request for document ID: {}", id);
        documentService.deleteDocument(id);
        logger.info("Successfully deleted document ID: {} from system", id);
        return ResponseEntity.ok().body("Document deleted successfully!");
    }
}
