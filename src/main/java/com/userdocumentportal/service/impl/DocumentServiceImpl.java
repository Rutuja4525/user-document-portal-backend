package com.userdocumentportal.service.impl;

import com.userdocumentportal.dto.DocumentDto;
import com.userdocumentportal.entity.Document;
import com.userdocumentportal.entity.User;
import com.userdocumentportal.exception.FileNotFoundException;
import com.userdocumentportal.exception.StorageException;
import com.userdocumentportal.repository.DocumentRepository;
import com.userdocumentportal.repository.UserRepository;
import com.userdocumentportal.service.DocumentService;
import com.userdocumentportal.service.S3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentServiceImpl.class);

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private S3Service s3Service;


    @Override
    public List<DocumentDto> getDocumentsByUser(Long userId) {
        logger.debug("Fetching documents from database for user ID: {}", userId);
        List<Document> docs = documentRepository.findByUserId(userId);
        return docs.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public List<DocumentDto> getAllDocuments() {
        logger.debug("Fetching all documents from database globally");
        List<Document> docs = documentRepository.findAll();
        return docs.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public void uploadDocument(Long userId, MultipartFile file, String category) {
        logger.info("Starting upload document workflow for user ID: {}, category: {}", userId, category);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Upload failed: User not found with ID: {}", userId);
                    return new FileNotFoundException("User not found with id: " + userId);
                });

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            logger.warn("Upload failed: Invalid original file name.");
            throw new IllegalArgumentException("Uploaded file name is invalid.");
        }

        String lowerName = originalFilename.toLowerCase();
        logger.info("Validating file type and size for file: '{}', contentType: '{}', size: {} bytes", 
                originalFilename, file.getContentType(), file.getSize());

        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".doc") && !lowerName.endsWith(".docx")) {
            logger.warn("Upload failed: File '{}' has unsupported extension.", originalFilename);
            throw new IllegalArgumentException("Only Word documents (.doc, .docx) and PDF files (.pdf) are allowed.");
        }

        // Format size
        long sizeInBytes = file.getSize();
        String sizeStr;
        if (sizeInBytes >= 1024 * 1024) {
            sizeStr = String.format("%.1f MB", (double) sizeInBytes / (1024 * 1024));
        } else {
            sizeStr = String.format("%d KB", sizeInBytes / 1024);
        }

        byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (IOException e) {
            logger.error("Upload failed: Error reading uploaded bytes for file: '{}'", originalFilename, e);
            throw new StorageException("Failed to read uploaded file contents", e);
        }

        // Upload to S3 (original folder)
        logger.info("Forwarding file: '{}' of size: {} to S3 upload service", originalFilename, sizeStr);
        String s3Key = s3Service.uploadFile("original", originalFilename, fileData);
        logger.info("File '{}' successfully uploaded to S3 with key: '{}'", originalFilename, s3Key);

        Document doc = Document.builder()
                .name(originalFilename)
                .category(category)
                .size(sizeStr)
                .uploadDate(LocalDate.now())
                .status("Uploaded")
                .contentType(file.getContentType())
                .s3Key(s3Key)
                .user(user)
                .build();

        Document savedDoc = documentRepository.save(doc);
        logger.info("Document metadata successfully saved to database. Document ID: {}, assigned key: '{}'", 
                savedDoc.getId(), s3Key);

    }

    @Override
    public byte[] downloadDocument(Long id) {
        logger.info("Starting download workflow for document ID: {}", id);
        
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Download failed: Document metadata not found in database for ID: {}", id);
                    return new FileNotFoundException("Document not found with id: " + id);
                });

        logger.info("Document ID: {} matches S3 key: '{}'. Downloading file content from S3.", id, doc.getS3Key());
        byte[] fileData = s3Service.downloadFile(doc.getS3Key());
        logger.info("File download successful for S3 key: '{}', downloaded size: {} bytes", doc.getS3Key(), fileData.length);
        
        return fileData;
    }


    @Override
    public void deleteDocument(Long id) {
        logger.info("Starting deletion workflow for document ID: {}", id);
        
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Delete failed: Document metadata not found in database for ID: {}", id);
                    return new FileNotFoundException("Document not found with id: " + id);
                });

        // Delete original file from S3
        logger.info("Deleting original file from S3 with key: '{}'", doc.getS3Key());
        s3Service.deleteFile(doc.getS3Key());
        logger.info("S3 deletion successful for original key: '{}'", doc.getS3Key());


        documentRepository.delete(doc);
        logger.info("Document ID: {} metadata successfully deleted from MySQL database.", id);
    }

    @Override
    public DocumentDto getDocumentById(Long id) {
        logger.debug("Fetching document metadata by ID: {}", id);
        Document doc = documentRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Metadata lookup failed for Document ID: {}", id);
                    return new FileNotFoundException("Document not found with id: " + id);
                });
        return convertToDto(doc);
    }

    private DocumentDto convertToDto(Document doc) {
        return DocumentDto.builder()
                .id(doc.getId())
                .name(doc.getName())
                .category(doc.getCategory())
                .size(doc.getSize())
                .date(doc.getUploadDate())
                .status(doc.getStatus())
                .userId(doc.getUser() != null ? doc.getUser().getId() : null)
                .s3Key(doc.getS3Key())
                .processedS3Key(doc.getProcessedS3Key())
                .contentType(doc.getContentType())
                .build();
    }
}
