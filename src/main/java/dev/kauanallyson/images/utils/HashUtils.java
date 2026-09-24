package dev.kauanallyson.images.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashUtils {

    private HashUtils() {
    }

    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public static String sha256Hex(byte[] data) {
        return hex(sha256().digest(data));
    }

    public static String hex(MessageDigest digest) {
        return hex(digest.digest());
    }

    private static String hex(byte[] hashBytes) {
        return HexFormat.of().formatHex(hashBytes);
    }
}
