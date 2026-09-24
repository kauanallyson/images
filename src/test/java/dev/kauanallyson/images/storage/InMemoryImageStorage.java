package dev.kauanallyson.images.storage;

import dev.kauanallyson.images.exceptions.StorageException;

import java.io.IOException;
import java.io.InputStream;
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

    @Override
    public void upload(InputStream content, long size, String key, String contentType) {
        beforeUpload.run();
        if (failUploads) {
            throw new StorageException("upload failed", null);
        }
        // like the S3 client, reads exactly the declared length
        try {
            objects.put(key, content.readNBytes((int) size));
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
