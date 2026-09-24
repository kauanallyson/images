package dev.kauanallyson.images.validation;

import java.io.InputStream;

// hash is the client-declared hash; the content is only verified against it once fully read
public record ValidatedUpload(InputStream content, long size, String hash, String mimeType, String originalFileName) {
}
