package dev.kauanallyson.images.ports;

import java.net.URI;

public interface StoragePort {
    URI objectUri(String key);
    void uploadFile(byte[] fileData, String key, String contentType);
    void deleteFile(String key);
    URI presignedGetUrl(String key, String downloadFileName);
}
