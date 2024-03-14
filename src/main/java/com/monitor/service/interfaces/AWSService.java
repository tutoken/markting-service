package com.monitor.service.interfaces;

public interface AWSService {

    /**
     * @param file
     * @param bucket
     * @param objectKey
     * @return
     */
    void uploadFile(String file, String bucket, String objectKey);

    boolean uploadFileInBytes(byte[] pdfBytes, String bucket, String objectKey);

    String generateDownloadUrl(String bucketName, String objectKey);
}
