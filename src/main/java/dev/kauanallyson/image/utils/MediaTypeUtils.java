package dev.kauanallyson.image.utils;

import org.apache.tika.Tika;

public final class MediaTypeUtils {
    private static final Tika TIKA = new Tika();

    private MediaTypeUtils() {
    }

    public static String detectMimeType(byte[] bytes) {
        return TIKA.detect(bytes);
    }
}
