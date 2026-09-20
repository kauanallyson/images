package dev.kauanallyson.image.ports;

import java.net.URI;

public interface StoragePort {
    URI uploadFile(byte[] fileData, String filename, String contentType);
}