package dev.kauanallyson.images.validation;

import dev.kauanallyson.images.config.ImageProperties;
import dev.kauanallyson.images.exceptions.EmptyFileException;
import dev.kauanallyson.images.exceptions.FileIntegrityException;
import dev.kauanallyson.images.exceptions.UnsupportedMediaTypeException;
import dev.kauanallyson.images.utils.HashUtils;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    // Minimal 1x1 PNG.
    static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    final FileValidator validator = new FileValidator(new ImageProperties(List.of("image/png")));

    @Test
    void returnsServerComputedLowercaseHashAndDetectedMimeType() {
        String upper = HashUtils.sha256Hex(PNG).toUpperCase();

        ValidatedUpload upload = validator.validate(upper, new MockMultipartFile("file", "pic.png", null, PNG));

        assertThat(upload.hash()).isEqualTo(HashUtils.sha256Hex(PNG));
        assertThat(upload.mimeType()).isEqualTo("image/png");
        assertThat(upload.originalFileName()).isEqualTo("pic.png");
        assertThat(upload.data()).isEqualTo(PNG);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> validator.validate("x", new MockMultipartFile("file", new byte[0])))
                .isInstanceOf(EmptyFileException.class);
        assertThatThrownBy(() -> validator.validate("x", null))
                .isInstanceOf(EmptyFileException.class);
    }

    @Test
    void rejectsDisallowedMimeType() {
        byte[] text = "hello".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> validator.validate(HashUtils.sha256Hex(text), new MockMultipartFile("file", text)))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsHashMismatch() {
        assertThatThrownBy(() -> validator.validate("deadbeef", new MockMultipartFile("file", "pic.png", null, PNG)))
                .isInstanceOf(FileIntegrityException.class);
    }
}
