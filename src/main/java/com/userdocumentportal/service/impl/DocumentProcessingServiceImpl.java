package com.userdocumentportal.service.impl;

import com.userdocumentportal.entity.Document;
import com.userdocumentportal.exception.FileNotFoundException;
import com.userdocumentportal.repository.DocumentRepository;
import com.userdocumentportal.service.DocumentProcessingService;
import com.userdocumentportal.service.S3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingServiceImpl.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private S3Service s3Service;

    @Override
    public void processDocument(Long documentId) {
        logger.info("Python document processing started for document ID: {}", documentId);
        
        try {
            Document doc = documentRepository.findById(documentId)
                    .orElseThrow(() -> {
                        logger.error("Processing aborted: Document ID {} not found in database.", documentId);
                        return new FileNotFoundException("Document not found with id: " + documentId);
                    });

            logger.info("Step 1: Downloading original document from S3 with key: '{}' for Python processing", doc.getS3Key());
            byte[] originalData = s3Service.downloadFile(doc.getS3Key());
            logger.info("Original document downloaded successfully from S3 ({} bytes)", originalData.length);
            
            // Simulating VBA macro processing converted to Python
            logger.info("Step 2: Simulating Python macro processing on document: '{}'", doc.getName());
            byte[] processedData = simulatePythonProcessing(originalData);
            logger.info("Step 3: Processing completed successfully for document: '{}'", doc.getName());

            // Generate unique S3 key for processed folder
            String processedS3Key = s3Service.generateS3Key("processed", doc.getName());
            logger.info("Step 4: Uploading processed document to S3 with key: '{}'", processedS3Key);
            s3Service.uploadFile("processed", doc.getName(), processedData);
            logger.info("Processed document uploaded successfully to S3 with key: '{}'", processedS3Key);

            // Update database with processed key
            logger.info("Step 5: Updating MySQL database with processed document S3 key and status");
            doc.setProcessedS3Key(processedS3Key);
            doc.setStatus("Processed");
            documentRepository.save(doc);
            logger.info("Database updated successfully for document ID: {}. Status set to 'Processed', key: '{}'", 
                    documentId, processedS3Key);

        } catch (Exception e) {
            logger.error("Processing errors: Python document processing failed for document ID: {}. Error: {}", 
                    documentId, e.getMessage(), e);
            throw e;
        }
    }

    private byte[] simulatePythonProcessing(byte[] originalData) {
        // Simulates macro modification: simply returns the original byte array
        if (originalData == null) {
            return new byte[0];
        }
        return originalData;
    }
}
