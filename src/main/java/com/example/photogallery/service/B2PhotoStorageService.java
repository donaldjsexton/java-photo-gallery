package com.example.photogallery.service;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@ConditionalOnProperty(
    name = {
        "B2_ACCESS_KEY_ID",
        "B2_SECRET_ACCESS_KEY",
        "B2_BUCKET_NAME",
        "B2_REGION",
        "B2_S3_ENDPOINT"
    }
)
public class B2PhotoStorageService implements PhotoStorageService {

    private final S3Client s3Client;
    private final String bucketName;

    private static final String TENANT_SLUG_PATTERN = "^[a-z0-9][a-z0-9-]{0,63}$";

    public B2PhotoStorageService(
        S3Client s3Client,
        @Value("${B2_BUCKET_NAME}") String bucketName
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public String storeFile(
        byte[] bytes,
        String storedFileName,
        String contentType
    ) throws IOException {
        validateStoredKey(storedFileName);
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Empty file bytes");
        }

        PutObjectRequest.Builder request = PutObjectRequest
            .builder()
            .bucket(bucketName)
            .key(storedFileName);
        if (StringUtils.hasText(contentType)) {
            request.contentType(contentType.trim());
        }

        try {
            s3Client.putObject(request.build(), RequestBody.fromBytes(bytes));
        } catch (S3Exception e) {
            throw new IOException("Failed to store file in B2", e);
        }
        return storedFileName;
    }

    @Override
    public InputStream openStream(String storedFileName) throws IOException {
        validateStoredKey(storedFileName);
        GetObjectRequest request = GetObjectRequest
            .builder()
            .bucket(bucketName)
            .key(storedFileName)
            .build();
        try {
            ResponseInputStream<?> response = s3Client.getObject(request);
            return response != null ? response : new ByteArrayInputStream(new byte[0]);
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException("Stored file not found");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new FileNotFoundException("Stored file not found");
            }
            throw new IOException("Failed to read stored file", e);
        }
    }

    @Override
    public long getFileSize(String storedFileName) throws IOException {
        validateStoredKey(storedFileName);
        HeadObjectRequest request = HeadObjectRequest
            .builder()
            .bucket(bucketName)
            .key(storedFileName)
            .build();
        try {
            return s3Client.headObject(request).contentLength();
        } catch (NoSuchKeyException e) {
            throw new FileNotFoundException("Stored file not found");
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new FileNotFoundException("Stored file not found");
            }
            throw new IOException("Failed to read stored file metadata", e);
        }
    }

    @Override
    public boolean deleteFile(String storedFileName) throws IOException {
        validateStoredKey(storedFileName);
        try {
            s3Client.deleteObject(
                DeleteObjectRequest
                    .builder()
                    .bucket(bucketName)
                    .key(storedFileName)
                    .build()
            );
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return false;
            }
            throw new IOException("Failed to delete stored file", e);
        }
    }

    @Override
    public void deleteEmptyTenantDirectory(String tenantSlug) {
        // B2 object storage is flat; no-op.
    }

    private void validateStoredKey(String storedFileName) throws IOException {
        if (
            storedFileName == null ||
            storedFileName.isBlank() ||
            storedFileName.contains("..") ||
            storedFileName.contains("\\")
        ) {
            throw new IOException("Invalid file name");
        }

        String normalized = storedFileName.trim();
        int slashCount = 0;
        for (int i = 0; i < normalized.length(); i++) {
            if (normalized.charAt(i) == '/') slashCount++;
        }
        if (slashCount == 0) {
            if (normalized.contains("/")) {
                throw new IOException("Invalid file name");
            }
            return;
        }
        if (slashCount != 1) {
            throw new IOException("Invalid file name");
        }

        int idx = normalized.indexOf('/');
        String tenantSlug = normalized.substring(0, idx);
        String leafName = normalized.substring(idx + 1);

        if (!tenantSlug.matches(TENANT_SLUG_PATTERN)) {
            throw new IOException("Invalid tenant segment");
        }
        if (
            leafName.isBlank() ||
            leafName.contains("..") ||
            leafName.contains("/") ||
            leafName.contains("\\")
        ) {
            throw new IOException("Invalid file name");
        }
    }
}
