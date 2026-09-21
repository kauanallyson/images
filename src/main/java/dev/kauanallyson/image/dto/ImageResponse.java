package dev.kauanallyson.image.dto;

import java.sql.Timestamp;

public record ImageResponse(
        String hash,
        String fileName,
        String contentType,
        Timestamp createdAt,
        Timestamp updatedAt
) {
}
