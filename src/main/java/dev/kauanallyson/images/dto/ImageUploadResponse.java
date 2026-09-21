package dev.kauanallyson.images.dto;

import java.net.URI;

public record ImageUploadResponse(
        URI uri,
        String fileName
) {
}
