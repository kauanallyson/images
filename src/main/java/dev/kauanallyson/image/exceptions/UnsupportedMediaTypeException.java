package dev.kauanallyson.image.exceptions;

import org.springframework.http.HttpStatus;

import java.util.Collection;

public final class UnsupportedMediaTypeException extends BusinessException {
    public UnsupportedMediaTypeException(String detectedType, Collection<String> allowedTypes) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported media type '" + detectedType + "'. Allowed types: " + String.join(", ", allowedTypes));
    }
}
