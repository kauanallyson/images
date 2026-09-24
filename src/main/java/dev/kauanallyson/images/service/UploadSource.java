package dev.kauanallyson.images.service;

import java.io.InputStream;

// the caller that opened the stream is responsible for closing it
public record UploadSource(InputStream content, long size, String fileName) {
}
