package dev.kauanallyson.image.exceptions;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public final class ImageNotFoundException extends BusinessException {
    public ImageNotFoundException(UUID uuid) {
        super(HttpStatus.NOT_FOUND, "Image not found: " + uuid);
    }
}
