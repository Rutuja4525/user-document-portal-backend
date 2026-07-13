package com.userdocumentportal.service;

import com.userdocumentportal.dto.DocumentDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DocumentService {
    /**
     * Retrieves all documents associated with a property.
     *
     * @param propertyId the property ID
     * @return list of Document DTOs
     */
    List<DocumentDto> getDocumentsByProperty(Long propertyId);

    /**
     * Retrieves all documents in the system.
     *
     * @return list of Document DTOs
     */
    List<DocumentDto> getAllDocuments();

    /**
     * Handles document upload: validates input, uploads file to S3, and saves metadata to MySQL.
     *
     * @param propertyId the property ID to associate the document with
     * @param file       the uploaded file
     * @param category   the category of the document
     */
    void uploadDocument(Long propertyId, MultipartFile file, String category);

    /**
     * Downloads document content from S3 based on database record id.
     *
     * @param id the document database ID
     * @return file data as byte array
     */
    byte[] downloadDocument(Long id);

    /**
     * Deletes a document from the database and deletes its file from S3.
     *
     * @param id the document database ID
     */
    void deleteDocument(Long id);

    /**
     * Retrieves a single document by its ID.
     *
     * @param id the document database ID
     * @return the document DTO
     */
    DocumentDto getDocumentById(Long id);
}
