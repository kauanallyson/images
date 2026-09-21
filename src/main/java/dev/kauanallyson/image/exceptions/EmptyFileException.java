package dev.kauanallyson.image.exceptions;

import org.springframework.http.HttpStatus;

public final class EmptyFileException extends BusinessException {
    public EmptyFileException() {
        super(HttpStatus.BAD_REQUEST, "File cannot be empty");
    }
}
