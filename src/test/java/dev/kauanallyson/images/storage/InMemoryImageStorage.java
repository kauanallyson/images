package dev.kauanallyson.images.storage;

import dev.kauanallyson.images.exceptions.StorageException;
import org.springframework.core.io.InputStreamSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryImageStorage implements ImageStorage {
    public final Map<String, byte[]> objects = new ConcurrentHashMap<>();
    public Runnable beforeUpload = () -> {
    };
    public boolean failUploads;
    public boolean failDeletes;
    // reads part of the content before failing once, like a dropped connection the S3 client retries
    public boolean failFirstAttemptMidway;

    @Override
    public void upload(InputStreamSource content, long size, String key, String contentType) {
        beforeUpload.run();
        if (failUploads) {
            throw new StorageException("upload failed", null);
        }
        // like the S3 client, opens a new stream per attempt and reads exactly the declared length
        try {
            if (failFirstAttemptMidway) {
                content.getInputStream().readNBytes((int) size / 2);
            }
            objects.put(key, content.getInputStream().readNBytes((int) size));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(String key) {
        if (failDeletes) {
            throw new StorageException("delete failed", null);
        }
        objects.remove(key);
    }

    @Override
    public URI presignedGetUrl(String key, String downloadFileName) {
        return URI.create("https://storage.test/" + key + "?name=" + downloadFileName);
    }
}
