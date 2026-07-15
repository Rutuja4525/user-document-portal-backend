package com.userdocumentportal.controller;

import com.userdocumentportal.dto.DocumentDto;
import com.userdocumentportal.service.DocumentService;
import com.userdocumentportal.security.services.UserDetailsImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // Get all documents for the authenticated company
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentDto>> getAllDocuments() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        logger.info("Received request to list all documents for user/company ID: {}", userDetails.getId());
        List<DocumentDto> docs = documentService.getDocumentsByUser(userDetails.getId());
        logger.info("Successfully retrieved {} documents for user ID: {}", docs.size(), userDetails.getId());
        return ResponseEntity.ok(docs);
    }

    // Upload document for the authenticated company
    @PostMapping("/documents")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("category") String category) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        String originalFilename = file.getOriginalFilename();
        logger.info("Received request to upload file: '{}' with category: '{}' for user/company ID: {}", 
                originalFilename, category, userDetails.getId());
        
        documentService.uploadDocument(userDetails.getId(), file, category);
        
        logger.info("Successfully completed upload API request for file: '{}' for user ID: {}", originalFilename, userDetails.getId());
        return ResponseEntity.ok().body("Document uploaded successfully!");
    }

    // Download original document content
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
