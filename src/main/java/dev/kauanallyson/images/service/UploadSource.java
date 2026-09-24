package dev.kauanallyson.images.service;

import org.springframework.core.io.InputStreamSource;

// content must open a fresh stream on every call, so uploads can be retried
public record UploadSource(InputStreamSource content, long size, String fileName) {
}
