package dev.kauanallyson.image.ports;

import java.net.URI;

public interface StoragePort {
    URI uploadFile(byte[] fileData, String key, String contentType);
    void deleteFile(String key);
}