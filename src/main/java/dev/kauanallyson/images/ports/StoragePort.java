package dev.kauanallyson.images.ports;

import java.net.URI;

public interface StoragePort {
    URI uploadFile(byte[] fileData, String key, String contentType);
    void deleteFile(String key);
    URI presignedGetUrl(String key, String downloadFileName);
}
