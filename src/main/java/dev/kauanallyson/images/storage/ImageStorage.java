package dev.kauanallyson.images.storage;

import java.io.InputStream;
import java.net.URI;

public interface ImageStorage {
    void upload(InputStream content, long size, String key, String contentType);

    void delete(String key);

    URI presignedGetUrl(String key, String downloadFileName);
}
