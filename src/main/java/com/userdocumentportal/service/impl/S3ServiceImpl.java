package com.userdocumentportal.service.impl;

import com.userdocumentportal.exception.FileNotFoundException;
import com.userdocumentportal.exception.StorageException;
import com.userdocumentportal.service.S3Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.UUID;

@Service
public class S3ServiceImpl implements S3Service {

    private static final Logger logger = LoggerFactory.getLogger(S3ServiceImpl.class);

    @Autowired
    private S3Client s3Client;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Override
    public String uploadFile(String folder, String originalFilename, byte[] fileData) {
        String key = generateS3Key(folder, originalFilename);
        logger.info("Initiating upload to S3 bucket: '{}' with key: '{}'", bucketName, key);
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(getContentType(originalFilename))
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));
            logger.info("Successfully uploaded file: '{}' to S3 key: '{}' in bucket: '{}'", originalFilename, key, bucketName);
            return key;
        } catch (S3Exception e) {
            logger.error("AWS S3 upload failed for key: '{}' in bucket: '{}'. AWS Error: {}", key, bucketName, e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to upload file to S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public byte[] downloadFile(String key) {
        logger.info("Initiating download from S3 bucket: '{}' with key: '{}'", bucketName, key);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            byte[] fileData = s3Client.getObjectAsBytes(getObjectRequest).asByteArray();
            logger.info("Successfully downloaded file from S3 key: '{}' in bucket: '{}', downloaded size: {} bytes", key, bucketName, fileData.length);
            return fileData;
        } catch (NoSuchKeyException e) {
            logger.warn("AWS S3 download failed: Key '{}' does not exist in bucket: '{}'", key, bucketName);
            throw new FileNotFoundException("File not found in S3 storage with key: " + key, e);
        } catch (S3Exception e) {
            logger.error("AWS S3 download failed for key: '{}' in bucket: '{}'. AWS Error: {}", key, bucketName, e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to download file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public void deleteFile(String key) {
        logger.info("Initiating deletion from S3 bucket: '{}' with key: '{}'", bucketName, key);
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteObjectRequest);
            logger.info("Successfully deleted file from S3 key: '{}' in bucket: '{}'", key, bucketName);
        } catch (S3Exception e) {
            logger.error("AWS S3 deletion failed for key: '{}' in bucket: '{}'. AWS Error: {}", key, bucketName, e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Failed to delete file from S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public String generateS3Key(String folder, String originalFilename) {
        String cleanName = cleanFilename(originalFilename);
        String uuid = UUID.randomUUID().toString();
        String generatedKey = folder + "/" + uuid + "_" + cleanName;
        logger.debug("Generated unique S3 key: '{}' for original file: '{}' in folder: '{}'", generatedKey, originalFilename, folder);
        return generatedKey;
    }

    private String cleanFilename(String filename) {
        if (filename == null) {
            return "file";
        }
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    private String getContentType(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }
        String lower = filename.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        } else if (lower.endsWith(".doc")) {
            return "application/msword";
        } else if (lower.endsWith(".docx")) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }
}
