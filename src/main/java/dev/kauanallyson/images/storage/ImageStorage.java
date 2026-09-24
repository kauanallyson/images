package dev.kauanallyson.images.storage;

import java.net.URI;

public interface ImageStorage {
    URI objectUri(String key);
    void upload(byte[] fileData, String key, String contentType);
    void delete(String key);
    URI presignedGetUrl(String key, String downloadFileName);
}
