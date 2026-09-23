package dev.kauanallyson.images.utils;

import org.apache.tika.Tika;

public final class FileMetadata {
    // apache tika for general document metadata
    private static final Tika TIKA = new Tika();

    private FileMetadata() {
    }

    public static String mimeType(byte[] bytes) {
        return TIKA.detect(bytes);
    }
}
