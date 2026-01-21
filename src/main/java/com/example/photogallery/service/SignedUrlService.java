package com.example.photogallery.service;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@ConditionalOnBean(S3Presigner.class)
@ConditionalOnProperty(name = "B2_BUCKET_NAME")
public class SignedUrlService {

    private final S3Presigner presigner;
    private final String bucketName;

    public SignedUrlService(
        S3Presigner presigner,
        @Value("${B2_BUCKET_NAME}") String bucketName
    ) {
        this.presigner = presigner;
        this.bucketName = bucketName;
    }

    public String signGetObjectUrl(String objectKey, Duration ttl) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl is required");
        }

        GetObjectRequest getObjectRequest = GetObjectRequest
            .builder()
            .bucket(bucketName)
            .key(objectKey)
            .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest
            .builder()
            .signatureDuration(ttl)
            .getObjectRequest(getObjectRequest)
            .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(
            presignRequest
        );
        return presigned.url().toString();
    }
}
