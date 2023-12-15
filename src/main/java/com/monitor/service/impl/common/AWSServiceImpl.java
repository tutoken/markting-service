package com.monitor.service.impl.common;

import com.monitor.service.interfaces.AWSService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.nio.file.Path;
import java.time.Duration;

@Service("awsS3Service")
@Slf4j
public class AWSServiceImpl implements AWSService {

    @Autowired
    private ProfileCredentialsProvider profileCredentialsProvider;

    @Value("${aws.region}")
    private String region;

    private static final Long PRE_SIGNED_VALID_PERIOD = 1L;

    @Override
    public void uploadFile(String file, String bucket, String objectKey) {
        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(profileCredentialsProvider)
                .build()) {
            String md5Pass = DigestUtils.md5DigestAsHex(objectKey.getBytes());
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(md5Pass)
//                    .contentType("text/plain")
                    .build();

            s3Client.putObject(putObjectRequest, Path.of(file));
        }
    }

    @Override
    public boolean uploadFileInBytes(byte[] pdfBytes, String bucketName, String objectKey) {
        PutObjectResponse response;
        try (S3Client s3Client = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(profileCredentialsProvider)
                .build()) {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
//                    .contentType("application/pdf")
                    .build();
            response = s3Client.putObject(putObjectRequest, RequestBody.fromBytes(pdfBytes));

            log.info("response is " + response);
            if (response == null) {
                return false;
            }
            String eTag = response.eTag();
            if (eTag == null) {
                return false;
            }

            return objectKey.equals(response.eTag().replaceAll("\"", ""));
        } catch (Exception exception) {
            log.error("Upload file failed. ", exception);
        }
        return false;
    }

    @Override
    public String generateDownloadUrl(String bucketName, String objectKey) {
        try (S3Presigner preSigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(profileCredentialsProvider)
                .build()) {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            GetObjectPresignRequest getObjectPresignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(PRE_SIGNED_VALID_PERIOD))
                    .getObjectRequest(getObjectRequest)
                    .build();
            PresignedGetObjectRequest presignedGetObjectRequest = preSigner.presignGetObject(getObjectPresignRequest);

            return presignedGetObjectRequest.url().toString();
        }
    }
}

