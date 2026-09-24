package dev.kauanallyson.images.storage;

import org.springframework.core.io.InputStreamSource;

import java.net.URI;

public interface ImageStorage {
    // content may be opened more than once, e.g. to retry after a transient failure
    void upload(InputStreamSource content, long size, String key, String contentType);

    void delete(String key);

    URI presignedGetUrl(String key, String downloadFileName);
}
