package dev.kauanallyson.image.dto;

import java.net.URI;

public record ImageUploadResponse(
        URI uri,
        String fileName
) {
}
