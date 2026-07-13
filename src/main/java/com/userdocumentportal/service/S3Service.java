package com.userdocumentportal.service;

public interface S3Service {
    /**
     * Uploads file data to the specified S3 folder.
     * Generates a unique key automatically.
     *
     * @param folder           the target S3 folder prefix (e.g. "original" or "processed")
     * @param originalFilename the original name of the uploaded file
     * @param fileData         the byte content of the file
     * @return the generated unique S3 key
     */
    String uploadFile(String folder, String originalFilename, byte[] fileData);

    /**
     * Downloads file content from AWS S3 using the S3 object key.
     *
     * @param key the S3 object key
     * @return the file content as a byte array
     */
    byte[] downloadFile(String key);

    /**
     * Deletes a file from S3 using the S3 object key.
     *
     * @param key the S3 object key
     */
    void deleteFile(String key);

    /**
     * Generates a unique, URL-safe S3 object key.
     *
     * @param folder           the target S3 folder prefix
     * @param originalFilename the original file name
     * @return the generated unique key
     */
    String generateS3Key(String folder, String originalFilename);
}
