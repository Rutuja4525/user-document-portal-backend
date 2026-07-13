package com.userdocumentportal.service;

public interface DocumentProcessingService {
    /**
     * Simulates the future Python macro processing service on a document.
     * Includes thorough logging of downloading original document, processing simulation,
     * uploading the result to S3, and updating the database.
     *
     * @param documentId the ID of the document to process
     */
    void processDocument(Long documentId);
}
