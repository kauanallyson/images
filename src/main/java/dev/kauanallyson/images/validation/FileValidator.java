package dev.kauanallyson.images.validation;

import dev.kauanallyson.images.config.ImageProperties;
import dev.kauanallyson.images.exceptions.EmptyFileException;
import dev.kauanallyson.images.exceptions.FileIntegrityException;
import dev.kauanallyson.images.exceptions.FileReadException;
import dev.kauanallyson.images.exceptions.UnsupportedMediaTypeException;
import dev.kauanallyson.images.service.UploadSource;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class FileValidator {
    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");
    private static final Tika TIKA = new Tika();

    private final ImageProperties properties;

    public FileValidator(ImageProperties properties) {
        this.properties = properties;
    }

    public ValidatedUpload validate(String expectedHash, UploadSource source) {
        if (source == null || source.content() == null || source.size() <= 0) {
            throw new EmptyFileException();
        }

        // the declared hash becomes the storage key before the content is verified, so it must be well-formed
        String hash = expectedHash == null ? "" : expectedHash.trim().toLowerCase(Locale.ROOT);
        if (!SHA256_HEX.matcher(hash).matches()) {
            throw new FileIntegrityException();
        }

        String mimeType;
        // sniffs only the leading bytes of a separately opened stream
        try (InputStream content = new BufferedInputStream(source.content().getInputStream())) {
            mimeType = TIKA.detect(content);
        } catch (IOException e) {
            throw new FileReadException(e);
        }
        if (!properties.allowedTypes().contains(mimeType)) {
            throw new UnsupportedMediaTypeException(mimeType, properties.allowedTypes());
        }

        VerifiedContent verified = new VerifiedContent(source.content(), source.size(), hash);
        return new ValidatedUpload(verified, source.size(), hash, mimeType, source.fileName());
    }
}
