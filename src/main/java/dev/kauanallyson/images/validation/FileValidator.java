package dev.kauanallyson.images.validation;

import dev.kauanallyson.images.config.ImageProperties;
import dev.kauanallyson.images.exceptions.EmptyFileException;
import dev.kauanallyson.images.exceptions.FileIntegrityException;
import dev.kauanallyson.images.exceptions.FileReadException;
import dev.kauanallyson.images.exceptions.UnsupportedMediaTypeException;
import dev.kauanallyson.images.utils.FileMetadata;
import dev.kauanallyson.images.utils.HashUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Component
public class FileValidator {
    private final ImageProperties properties;

    public FileValidator(ImageProperties properties) {
        this.properties = properties;
    }

    public ValidatedUpload validate(String expectedHash, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException();
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new FileReadException(e);
        }

        String mimeType = FileMetadata.mimeType(data);
        if (!properties.allowedTypes().contains(mimeType)) {
            throw new UnsupportedMediaTypeException(mimeType, properties.allowedTypes());
        }

        String hash = HashUtils.sha256Hex(data);
        if (expectedHash == null || !hash.equalsIgnoreCase(expectedHash.trim())) {
            throw new FileIntegrityException();
        }

        return new ValidatedUpload(data, hash, mimeType, file.getOriginalFilename());
    }
}
