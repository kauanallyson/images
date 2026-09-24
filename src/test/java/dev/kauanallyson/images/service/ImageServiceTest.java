package dev.kauanallyson.images.service;

import dev.kauanallyson.images.config.ImageProperties;
import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.FileIntegrityException;
import dev.kauanallyson.images.exceptions.ImageNotFoundException;
import dev.kauanallyson.images.exceptions.StorageException;
import dev.kauanallyson.images.mapper.ImageMapperImpl;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.storage.InMemoryImageStorage;
import dev.kauanallyson.images.validation.FileValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

import static dev.kauanallyson.images.validation.VerifiedContent.sha256Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// runs against a real database and transactions, with storage replaced by an in-memory adapter
@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({ImageService.class, ImageStore.class, FileValidator.class, ImageMapperImpl.class, ImageServiceTest.Config.class})
class ImageServiceTest {
    // Minimal 1x1 PNG.
    static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");
    static final String HASH = sha256Hex(PNG);

    @TestConfiguration
    static class Config {
        @Bean
        InMemoryImageStorage storage() {
            return new InMemoryImageStorage();
        }

        @Bean
        ImageProperties imageProperties() {
            return new ImageProperties(List.of("image/png"));
        }
    }

    @Autowired ImageService service;
    @Autowired ImageRepository repository;
    @Autowired InMemoryImageStorage storage;
    @Autowired PlatformTransactionManager transactionManager;

    static UploadSource source() {
        return new UploadSource(() -> new ByteArrayInputStream(PNG), PNG.length, "pic.png");
    }

    @AfterEach
    void reset() {
        repository.deleteAll();
        storage.objects.clear();
        storage.beforeUpload = () -> {
        };
        storage.failUploads = false;
        storage.failDeletes = false;
        storage.failFirstAttemptMidway = false;
    }

    @Test
    void uploadStoresRowAndObject() {
        ImageUploadResponse response = service.uploadImage(HASH, source());

        assertThat(response.fileName()).isEqualTo("pic.png");
        assertThat(response.uri().toString()).contains(HASH);
        assertThat(repository.existsByHash(HASH)).isTrue();
        assertThat(storage.objects.get(HASH)).isEqualTo(PNG);
    }

    @Test
    void uploadOfExistingHashReturnsExistingWithoutUploadingAgain() {
        service.uploadImage(HASH, source());
        storage.failUploads = true;

        assertThat(service.uploadImage(HASH, source()).fileName()).isEqualTo("pic.png");
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void uploadOfExistingHashRejectsContentThatDoesNotMatch() {
        service.uploadImage(HASH, source());
        byte[] other = PNG.clone();
        other[other.length - 1] ^= 1;

        UploadSource tampered = new UploadSource(() -> new ByteArrayInputStream(other), other.length, "x.png");

        assertThatThrownBy(() -> service.uploadImage(HASH, tampered)).isInstanceOf(FileIntegrityException.class);
    }

    @Test
    void uploadVerifiesContentWhenStorageRetriesAfterPartialRead() {
        storage.failFirstAttemptMidway = true;

        service.uploadImage(HASH, source());

        assertThat(storage.objects.get(HASH)).isEqualTo(PNG);
    }

    @Test
    void uploadRejectsContentThatDoesNotMatchDeclaredHashAndLeavesNothingBehind() {
        String otherHash = sha256Hex(new byte[]{9});

        assertThatThrownBy(() -> service.uploadImage(otherHash, source())).isInstanceOf(FileIntegrityException.class);

        assertThat(repository.existsByHash(otherHash)).isFalse();
        assertThat(storage.objects).isEmpty();
    }

    @Test
    void uploadRollsBackRowWhenStorageFails() {
        storage.failUploads = true;

        assertThatThrownBy(() -> service.uploadImage(HASH, source())).isInstanceOf(StorageException.class);

        assertThat(repository.existsByHash(HASH)).isFalse();
    }

    @Test
    void uploadKeepsObjectWhenConcurrentUploadOfSameHashCommitsFirst() {
        TransactionTemplate concurrent = new TransactionTemplate(transactionManager);
        concurrent.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
        storage.beforeUpload = () -> concurrent.executeWithoutResult(
                status -> repository.save(Image.of(HASH, "other.png", "image/png")));

        assertThatThrownBy(() -> service.uploadImage(HASH, source()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(repository.existsByHash(HASH)).isTrue();
        assertThat(storage.objects).containsKey(HASH);
    }

    @Test
    void deleteRemovesRowAndObject() {
        service.uploadImage(HASH, source());

        service.deleteImageByHash(HASH);

        assertThat(repository.existsByHash(HASH)).isFalse();
        assertThat(storage.objects).isEmpty();
    }

    @Test
    void deleteKeepsObjectWhenOuterTransactionRollsBack() {
        service.uploadImage(HASH, source());

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            service.deleteImageByHash(HASH);
            status.setRollbackOnly();
        });

        assertThat(repository.existsByHash(HASH)).isTrue();
        assertThat(storage.objects).containsKey(HASH);
    }

    @Test
    void deleteSucceedsEvenWhenObjectCannotBeRemoved() {
        service.uploadImage(HASH, source());
        storage.failDeletes = true;

        service.deleteImageByHash(HASH);

        assertThat(repository.existsByHash(HASH)).isFalse();
    }

    @Test
    void deleteOfUnknownHashIsNoop() {
        service.deleteImageByHash(HASH);

        assertThat(repository.count()).isZero();
    }

    @Test
    void findByHashThrowsWhenMissing() {
        assertThatThrownBy(() -> service.findImageByHash(HASH)).isInstanceOf(ImageNotFoundException.class);
    }
}
