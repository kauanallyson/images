package dev.kauanallyson.images.service;

import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.ImageNotFoundException;
import dev.kauanallyson.images.exceptions.StorageException;
import dev.kauanallyson.images.mapper.ImageMapper;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.storage.ImageStorage;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.validation.FileValidator;
import dev.kauanallyson.images.validation.ValidatedUpload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {
    static final String HASH = "abc123";
    static final byte[] DATA = {1, 2, 3};
    static final URI OBJECT_URI = URI.create("https://bucket/abc123");
    static final URI PRESIGNED = URI.create("https://bucket/abc123?sig");
    static final MultipartFile FILE = new MockMultipartFile("file", "pic.png", "image/png", DATA);

    @Mock ImageStorage storage;
    @Mock ImageMapper mapper;
    @Mock ImageRepository repository;
    @Mock FileValidator validator;
    @InjectMocks ImageService service;

    ValidatedUpload upload = new ValidatedUpload(DATA, HASH, "image/png", "pic.png");
    Image image = Image.of(HASH, "pic.png", "image/png", OBJECT_URI);
    ImageUploadResponse response = new ImageUploadResponse(PRESIGNED, "pic.png");

    @BeforeEach
    void startSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void uploadSavesRowBeforeUploadingObject() {
        when(validator.validate(HASH, FILE)).thenReturn(upload);
        when(repository.findByHash(HASH)).thenReturn(Optional.empty());
        when(storage.objectUri(HASH)).thenReturn(OBJECT_URI);
        when(repository.save(any(Image.class))).thenReturn(image);
        when(storage.presignedGetUrl(HASH, "pic.png")).thenReturn(PRESIGNED);
        when(mapper.toResponse(image, PRESIGNED)).thenReturn(response);

        ImageUploadResponse result = service.uploadImage(HASH, FILE);

        assertThat(result).isSameAs(response);
        InOrder order = inOrder(repository, storage);
        order.verify(repository).save(any(Image.class));
        order.verify(storage).upload(DATA, HASH, "image/png");
    }

    @Test
    void uploadDeletesObjectWhenTransactionRollsBack() {
        stubSuccessfulUpload();
        when(repository.existsByHash(HASH)).thenReturn(false);

        service.uploadImage(HASH, FILE);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete(HASH);
    }

    @Test
    void uploadKeepsObjectWhenTransactionCommits() {
        stubSuccessfulUpload();

        service.uploadImage(HASH, FILE);
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(anyString());
    }

    @Test
    void uploadKeepsObjectOnRollbackWhenConcurrentUploadCommittedSameHash() {
        stubSuccessfulUpload();
        when(repository.existsByHash(HASH)).thenReturn(true);

        service.uploadImage(HASH, FILE);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage, never()).delete(anyString());
    }

    private void stubSuccessfulUpload() {
        when(validator.validate(HASH, FILE)).thenReturn(upload);
        when(repository.findByHash(HASH)).thenReturn(Optional.empty());
        when(storage.objectUri(HASH)).thenReturn(OBJECT_URI);
        when(repository.save(any(Image.class))).thenReturn(image);
        when(storage.presignedGetUrl(HASH, "pic.png")).thenReturn(PRESIGNED);
        when(mapper.toResponse(image, PRESIGNED)).thenReturn(response);
    }

    private static void completeTransaction(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(status));
    }

    @Test
    void uploadPropagatesStorageFailureAfterSave() {
        when(validator.validate(HASH, FILE)).thenReturn(upload);
        when(repository.findByHash(HASH)).thenReturn(Optional.empty());
        when(storage.objectUri(HASH)).thenReturn(OBJECT_URI);
        when(repository.save(any(Image.class))).thenReturn(image);
        doThrow(new StorageException("boom", null)).when(storage).upload(DATA, HASH, "image/png");

        assertThatThrownBy(() -> service.uploadImage(HASH, FILE)).isInstanceOf(StorageException.class);

        verify(repository).save(any(Image.class));
        verify(storage, never()).presignedGetUrl(anyString(), anyString());
    }

    @Test
    void uploadOfExistingHashReturnsExistingWithoutTouchingStorage() {
        when(validator.validate(HASH, FILE)).thenReturn(upload);
        when(repository.findByHash(HASH)).thenReturn(Optional.of(image));
        when(storage.presignedGetUrl(HASH, "pic.png")).thenReturn(PRESIGNED);
        when(mapper.toResponse(image, PRESIGNED)).thenReturn(response);

        assertThat(service.uploadImage(HASH, FILE)).isSameAs(response);

        verify(repository, never()).save(any());
        verify(storage, never()).upload(any(), anyString(), anyString());
    }

    @Test
    void deleteRemovesObjectOnlyAfterCommit() {
        when(repository.findByHash(HASH)).thenReturn(Optional.of(image));
        when(repository.existsByHash(HASH)).thenReturn(false);

        service.deleteImageByHash(HASH);

        verify(repository).delete(image);
        verify(storage, never()).delete(anyString());

        commitTransaction();

        verify(storage).delete(HASH);
    }

    @Test
    void deleteKeepsObjectWhenTransactionRollsBack() {
        when(repository.findByHash(HASH)).thenReturn(Optional.of(image));

        service.deleteImageByHash(HASH);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage, never()).delete(anyString());
    }

    @Test
    void deleteKeepsObjectWhenSameHashWasReuploadedBeforeCleanup() {
        when(repository.findByHash(HASH)).thenReturn(Optional.of(image));
        when(repository.existsByHash(HASH)).thenReturn(true);

        service.deleteImageByHash(HASH);
        commitTransaction();

        verify(storage, never()).delete(anyString());
    }

    @Test
    void deleteSwallowsStorageFailureAfterCommit() {
        when(repository.findByHash(HASH)).thenReturn(Optional.of(image));
        when(repository.existsByHash(HASH)).thenReturn(false);
        doThrow(new StorageException("boom", null)).when(storage).delete(HASH);

        service.deleteImageByHash(HASH);
        commitTransaction();

        verify(storage).delete(HASH);
    }

    private static void commitTransaction() {
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);
    }

    @Test
    void deleteOfUnknownHashIsNoop() {
        when(repository.findByHash(HASH)).thenReturn(Optional.empty());

        service.deleteImageByHash(HASH);

        verify(repository, never()).delete(any());
        verify(storage, never()).delete(anyString());
    }

    @Test
    void findByHashThrowsWhenMissing() {
        when(repository.findByHash(HASH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findImageByHash(HASH)).isInstanceOf(ImageNotFoundException.class);
    }
}
