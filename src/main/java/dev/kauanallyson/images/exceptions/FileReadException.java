package dev.kauanallyson.images.exceptions;

import org.springframework.http.HttpStatus;

public final class FileReadException extends BusinessException {
    public FileReadException(Throwable cause) {
        super(HttpStatus.BAD_REQUEST, "Failed to read uploaded file", cause);
    }
}
