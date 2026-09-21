package dev.kauanallyson.images.exceptions;

import org.springframework.http.HttpStatus;

public final class FileIntegrityException extends BusinessException {
    public FileIntegrityException() {
        super(HttpStatus.UNPROCESSABLE_CONTENT, "File integrity check failed: SHA-256 mismatch");
    }
}
