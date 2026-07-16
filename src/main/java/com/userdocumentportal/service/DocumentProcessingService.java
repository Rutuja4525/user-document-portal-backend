package com.userdocumentportal.service;

public interface DocumentProcessingService {
    /**
     * Asynchronously processes a document after upload.
     * For .docx files: runs the ysign macro Python script to replace placeholders with FORMTEXT fields.
     * For .pdf files: copies the file as-is to the processed S3 folder.
     *
     * @param documentId the ID of the document to process
     */
    void processDocument(Long documentId);
}
