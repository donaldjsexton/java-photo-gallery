package com.example.photogallery.service;

import java.io.IOException;
import java.io.InputStream;

public interface PhotoStorageService {
    String storeFile(byte[] bytes, String storedFileName, String contentType)
        throws IOException;

    InputStream openStream(String storedFileName) throws IOException;

    long getFileSize(String storedFileName) throws IOException;

    boolean deleteFile(String storedFileName) throws IOException;

    void deleteEmptyTenantDirectory(String tenantSlug) throws IOException;
}
