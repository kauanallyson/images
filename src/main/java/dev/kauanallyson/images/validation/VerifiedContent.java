package dev.kauanallyson.images.validation;

import dev.kauanallyson.images.exceptions.FileIntegrityException;
import dev.kauanallyson.images.exceptions.FileReadException;
import org.springframework.core.io.InputStreamSource;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

// every opened stream hashes the content as it is read, so storage can reopen it to retry an upload
// and verify() checks the declared hash against whichever attempt was opened last
public final class VerifiedContent implements InputStreamSource, AutoCloseable {
    private final InputStreamSource source;
    private final String expectedHash;
    private final long expectedSize;
    private HashingStream current;

    VerifiedContent(InputStreamSource source, long expectedSize, String expectedHash) {
        this.source = source;
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

    // closes the previous attempt's stream, which this class owns
    @Override
    public InputStream getInputStream() throws IOException {
        close();
        current = new HashingStream(source.getInputStream());
        return current;
    }

    // call once the consumer has read the content; a partial read fails like a mismatch
    public void verify() {
        if (current == null || current.bytesRead != expectedSize
                || !expectedHash.equals(HexFormat.of().formatHex(current.digest.digest()))) {
            throw new FileIntegrityException();
        }
    }

    // for content no consumer will read, e.g. an upload of an already stored hash
    public void readAndVerify() {
        try {
            getInputStream().transferTo(OutputStream.nullOutputStream());
        } catch (IOException e) {
            throw new FileReadException(e);
        }
        verify();
    }

    @Override
    public void close() {
        if (current != null) {
            try {
                current.close();
            } catch (IOException ignored) {
                // nothing is lost by failing to close an input stream
            }
        }
    }

    private static final class HashingStream extends FilterInputStream {
        private final MessageDigest digest = sha256();
        private long bytesRead;

        private HashingStream(InputStream in) {
            super(in);
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

        // skipped bytes would never be hashed; a retry reopens the content instead of resetting it
        @Override
        public long skip(long n) {
            throw new UnsupportedOperationException("Verified content cannot be skipped");
        }

        @Override
        public boolean markSupported() {
            return false;
        }
    }
}
