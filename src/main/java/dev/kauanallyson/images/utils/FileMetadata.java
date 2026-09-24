package dev.kauanallyson.images.utils;

import org.apache.tika.Tika;

import java.io.IOException;
import java.io.InputStream;

public final class FileMetadata {
    // apache tika for general document metadata
    private static final Tika TIKA = new Tika();

    private FileMetadata() {
    }

    // sniffs only the leading bytes; the stream must support mark so it is reset for the caller
    public static String mimeType(InputStream stream) throws IOException {
        if (!stream.markSupported()) {
            throw new IllegalArgumentException("Stream must support mark/reset for type detection");
        }
        return TIKA.detect(stream);
    }
}
