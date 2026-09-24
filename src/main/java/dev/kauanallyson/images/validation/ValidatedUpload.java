package dev.kauanallyson.images.validation;

// hash is the client-declared hash; content.verify() checks it once the content has been streamed
public record ValidatedUpload(VerifiedContent content, long size, String hash, String mimeType, String originalFileName) {
}
