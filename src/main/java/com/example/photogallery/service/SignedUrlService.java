package com.example.photogallery.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
@ConditionalOnProperty(name = "R2_BUCKET_NAME")
public class SignedUrlService {

    private final S3Presigner presigner;
    private final String bucketName;
    private final String cdnBaseUrl;

    public SignedUrlService(
        S3Presigner presigner,
        @Value("${R2_BUCKET_NAME}") String bucketName,
        @Value("${R2_CDN_BASE_URL:}") String cdnBaseUrl
    ) {
        this.presigner = presigner;
        this.bucketName = bucketName;
        this.cdnBaseUrl = cdnBaseUrl;
    }

    public String signGetObjectUrl(String objectKey, Duration ttl) {
        return signGetObjectUrl(objectKey, ttl, true);
    }

    public String signGetObjectUrl(
        String objectKey,
        Duration ttl,
        boolean useCdn
    ) {
        if (!StringUtils.hasText(objectKey)) {
            throw new IllegalArgumentException("objectKey is required");
        }
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("ttl is required");
        }
        if (useCdn && StringUtils.hasText(cdnBaseUrl)) {
            return buildCdnUrl(objectKey);
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

    private String buildCdnUrl(String objectKey) {
        String base = cdnBaseUrl.trim();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + encodeObjectKey(objectKey);
    }

    private String encodeObjectKey(String objectKey) {
        String[] segments = objectKey.split("/");
        StringBuilder encoded = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                encoded.append('/');
            }
            encoded.append(encodePathSegment(segments[i]));
        }
        return encoded.toString();
    }

    private String encodePathSegment(String segment) {
        return URLEncoder
            .encode(segment, StandardCharsets.UTF_8)
            .replace("+", "%20");
    }
}
