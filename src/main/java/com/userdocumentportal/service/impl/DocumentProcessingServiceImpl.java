package com.userdocumentportal.service.impl;

import com.userdocumentportal.entity.Document;
import com.userdocumentportal.repository.DocumentRepository;
import com.userdocumentportal.service.DocumentProcessingService;
import com.userdocumentportal.service.S3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class DocumentProcessingServiceImpl implements DocumentProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingServiceImpl.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private S3Service s3Service;

    @Value("${document.processing.python-command:python}")
    private String pythonCommand;

    @Override
    @Async("documentProcessingExecutor")
    public void processDocument(Long documentId) {
        logger.info("Starting async document processing for document ID: {}", documentId);

        Optional<Document> optDoc = documentRepository.findById(documentId);
        if (optDoc.isEmpty()) {
            logger.warn("Document ID: {} not found, skipping processing", documentId);
            return;
        }

        Document doc = optDoc.get();
        String fileName = doc.getName().toLowerCase();

        try {
            // Update status to PROCESSING
            doc.setProcessingStatus("PROCESSING");
            documentRepository.save(doc);
            logger.info("Document ID: {} status updated to PROCESSING", documentId);

            if (fileName.endsWith(".pdf")) {
                processPdf(doc);
            } else if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
                processDocx(doc);
            } else {
                logger.warn("Document ID: {} has unsupported extension '{}', marking as FAILED", documentId, fileName);
                doc.setProcessingStatus("FAILED");
                documentRepository.save(doc);
            }

        } catch (Exception e) {
            logger.error("Document processing failed for document ID: {}. Error: {}", documentId, e.getMessage(), e);
            doc.setProcessingStatus("FAILED");
            documentRepository.save(doc);
        }
    }

    /**
     * For PDF files: download from original S3 key and re-upload to processed/ folder as-is.
     */
    private void processPdf(Document doc) {
        logger.info("Processing PDF document ID: {} — copying as-is to processed folder", doc.getId());

        byte[] fileData = s3Service.downloadFile(doc.getS3Key());
        String processedKey = s3Service.uploadFile("processed", doc.getName(), fileData);

        doc.setProcessedS3Key(processedKey);
        doc.setProcessingStatus("COMPLETED");
        documentRepository.save(doc);

        logger.info("PDF document ID: {} successfully copied to processed S3 key: '{}'", doc.getId(), processedKey);
    }

    /**
     * For .docx files: download original, run the ysign macro Python script, upload processed output.
     */
    private void processDocx(Document doc) throws Exception {
        logger.info("Processing DOCX document ID: {} — running ysign macro", doc.getId());

        // Create temp directory for processing
        Path tempDir = Files.createTempDirectory("doc-processing-");
        Path inputFile = tempDir.resolve("input.docx");
        Path outputFile = tempDir.resolve("output.docx");
        Path scriptFile = tempDir.resolve("ysign_macro_m1.py");

        try {
            // Download original file from S3
            byte[] originalData = s3Service.downloadFile(doc.getS3Key());
            Files.write(inputFile, originalData);
            logger.info("Document ID: {} downloaded to temp file: '{}'", doc.getId(), inputFile);

            // Extract the bundled Python script
            ClassPathResource scriptResource = new ClassPathResource("scripts/ysign_macro_m1.py");
            try (InputStream is = scriptResource.getInputStream()) {
                Files.copy(is, scriptFile, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Extracted ysign macro script to: '{}'", scriptFile);

            // Execute the Python script
            ProcessBuilder pb = new ProcessBuilder(
                    pythonCommand,
                    scriptFile.toAbsolutePath().toString(),
                    inputFile.toAbsolutePath().toString(),
                    outputFile.toAbsolutePath().toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Capture stdout/stderr
            StringBuilder processOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    processOutput.append(line).append("\n");
                }
            }

            boolean completed = process.waitFor(120, TimeUnit.SECONDS);
            int exitCode = completed ? process.exitValue() : -1;

            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Python script timed out after 120 seconds");
            }

            logger.info("Python script exited with code: {} for document ID: {}. Output: {}",
                    exitCode, doc.getId(), processOutput.toString().trim());

            if (exitCode != 0) {
                throw new RuntimeException("Python script failed with exit code " + exitCode + ": " + processOutput);
            }

            // Verify output file exists
            if (!Files.exists(outputFile)) {
                throw new RuntimeException("Python script did not produce output file");
            }

            // Upload processed file to S3
            byte[] processedData = Files.readAllBytes(outputFile);
            String processedKey = s3Service.uploadFile("processed", doc.getName(), processedData);

            doc.setProcessedS3Key(processedKey);
            doc.setProcessingStatus("COMPLETED");
            documentRepository.save(doc);

            logger.info("Document ID: {} successfully processed and uploaded to S3 key: '{}'. Processed file size: {} bytes",
                    doc.getId(), processedKey, processedData.length);

        } finally {
            // Clean up temp files
            try {
                Files.deleteIfExists(outputFile);
                Files.deleteIfExists(inputFile);
                Files.deleteIfExists(scriptFile);
                Files.deleteIfExists(tempDir);
                logger.debug("Temp directory cleaned up: '{}'", tempDir);
            } catch (IOException e) {
                logger.warn("Failed to clean up temp directory: '{}'. Error: {}", tempDir, e.getMessage());
            }
        }
    }
}
