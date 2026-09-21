package dev.kauanallyson.image.exceptions;

import org.springframework.http.HttpStatus;

public final class ImageNotFoundException extends BusinessException {
    public ImageNotFoundException(String hash) {
        super(HttpStatus.NOT_FOUND, "Image not found: " + hash);
    }
}
