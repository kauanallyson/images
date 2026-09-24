package dev.kauanallyson.images.validation;

import dev.kauanallyson.images.config.ImageProperties;
import dev.kauanallyson.images.exceptions.EmptyFileException;
import dev.kauanallyson.images.exceptions.FileIntegrityException;
import dev.kauanallyson.images.exceptions.UnsupportedMediaTypeException;
import dev.kauanallyson.images.service.UploadSource;
import dev.kauanallyson.images.utils.HashUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidatorTest {

    // Minimal 1x1 PNG.
    static final byte[] PNG = java.util.Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    static final String PNG_HASH = HashUtils.sha256Hex(PNG);

    final FileValidator validator = new FileValidator(new ImageProperties(List.of("image/png")));

    static UploadSource source(byte[] data, String fileName) {
        return new UploadSource(new ByteArrayInputStream(data), data.length, fileName);
    }

    @Test
    void normalizesDeclaredHashAndDetectsMimeTypeWithoutConsumingContent() throws IOException {
        ValidatedUpload upload = validator.validate("  " + PNG_HASH.toUpperCase() + " ", source(PNG, "pic.png"));

        assertThat(upload.hash()).isEqualTo(PNG_HASH);
        assertThat(upload.mimeType()).isEqualTo("image/png");
        assertThat(upload.originalFileName()).isEqualTo("pic.png");
        assertThat(upload.size()).isEqualTo(PNG.length);
        assertThat(upload.content().readAllBytes()).isEqualTo(PNG);
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> validator.validate(PNG_HASH, source(new byte[0], "x")))
                .isInstanceOf(EmptyFileException.class);
        assertThatThrownBy(() -> validator.validate(PNG_HASH, null))
                .isInstanceOf(EmptyFileException.class);
    }

    @Test
    void rejectsDisallowedMimeType() {
        byte[] text = "hello".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> validator.validate(HashUtils.sha256Hex(text), source(text, "a.txt")))
                .isInstanceOf(UnsupportedMediaTypeException.class);
    }

    @Test
    void rejectsMalformedDeclaredHash() {
        for (String bad : new String[]{null, "", "deadbeef", "../" + PNG_HASH.substring(3), PNG_HASH.replace('a', 'g')}) {
            assertThatThrownBy(() -> validator.validate(bad, source(PNG, "pic.png")))
                    .isInstanceOf(FileIntegrityException.class);
        }
    }

    @Test
    void verifyHashRejectsMismatch() {
        ValidatedUpload upload = validator.validate(PNG_HASH, source(PNG, "pic.png"));

        assertThatCode(() -> validator.verifyHash(upload, PNG_HASH)).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.verifyHash(upload, HashUtils.sha256Hex(new byte[]{1})))
                .isInstanceOf(FileIntegrityException.class);
    }
}
