package dev.kauanallyson.images.validation;

public record ValidatedUpload(byte[] data, String hash, String mimeType, String originalFileName) {
}
