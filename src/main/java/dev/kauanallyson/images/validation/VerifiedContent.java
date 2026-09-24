package dev.kauanallyson.images.validation;

import dev.kauanallyson.images.exceptions.FileIntegrityException;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// hashes the content as it is read, so it can be checked against the declared hash once a consumer has streamed it
public final class VerifiedContent extends FilterInputStream {
    private final String expectedHash;
    private final long expectedSize;
    private final MessageDigest digest = sha256();
    private long bytesRead;

    VerifiedContent(InputStream content, long expectedSize, String expectedHash) {
        super(content);
        this.expectedSize = expectedSize;
        this.expectedHash = expectedHash;
    }

    public static String sha256Hex(byte[] data) {
        return HexFormat.of().formatHex(sha256().digest(data));
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @Override
    public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            digest.update((byte) b);
            bytesRead++;
        }
        return b;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int n = super.read(buffer, offset, length);
        if (n > 0) {
            digest.update(buffer, offset, n);
            bytesRead += n;
        }
        return n;
    }

    // skipped bytes would never be hashed, so skipping is not allowed
    @Override
    public long skip(long n) {
        throw new UnsupportedOperationException("Verified content cannot be skipped");
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    // call once the consumer has read the content; a partial read fails like a mismatch
    public void verify() {
        if (bytesRead != expectedSize || !expectedHash.equals(HexFormat.of().formatHex(digest.digest()))) {
            throw new FileIntegrityException();
        }
    }
}
