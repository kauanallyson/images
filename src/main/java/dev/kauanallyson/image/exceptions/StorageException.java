package dev.kauanallyson.image.exceptions;

import org.springframework.http.HttpStatus;

public final class StorageException extends BusinessException {
    public StorageException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message, cause);
    }
}
