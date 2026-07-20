package com.userdocumentportal.service;

import com.userdocumentportal.dto.DocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    List<DocumentDto> getDocumentsByUser(Long userId);
    List<DocumentDto> getAllDocuments();
    void uploadDocument(Long userId, MultipartFile file, String category);
    byte[] downloadDocument(Long id);
    byte[] downloadProcessedDocument(Long id);
    void deleteDocument(Long id, String type);
    DocumentDto getDocumentById(Long id);
}
