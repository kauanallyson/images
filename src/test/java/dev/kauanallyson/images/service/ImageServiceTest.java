package dev.kauanallyson.images.service;

import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.ImageNotFoundException;
import dev.kauanallyson.images.exceptions.StorageException;
import dev.kauanallyson.images.mapper.ImageMapper;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.storage.ImageStorage;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.validation.FileValidator;
import dev.kauanallyson.images.config.ImageProperties;
import dev.kauanallyson.images.exceptions.FileIntegrityException;
import dev.kauanallyson.images.utils.HashUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {
    static final byte[] DATA = {1, 2, 3};
    static final String HASH = HashUtils.sha256Hex(DATA);
    static final String MIME = "application/octet-stream";
    static final URI OBJECT_URI = URI.create("https://bucket/abc123");
    static final URI PRESIGNED = URI.create("https://bucket/abc123?sig");

    @Mock ImageStorage storage;
    @Mock ImageMapper mapper;
    @Mock ImageRepository repository;
    @Spy FileValidator validator = new FileValidator(new ImageProperties(List.of(MIME)));
    @InjectMocks ImageService service;

    Image image = Image.of(HASH, "pic.png", MIME, OBJECT_URI);
    ImageUploadResponse response = new ImageUploadResponse(PRESIGNED, "pic.png");

    @BeforeEach
    void startSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    static UploadSource source() {
        return new UploadSource(new ByteArrayInputStream(DATA), DATA.length, "pic.png");
    }

    // storage reads the stream like the real S3 client, which is what feeds the digest
    void storageDrainsUploads() {
        doAnswer(inv -> inv.<InputStream>getArgument(0).readAllBytes())
                .when(storage).upload(any(), anyLong(), anyString(), anyString());
    }

    @AfterEach
    void clearSynchronization() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void uploadSavesRowBeforeUploadingObject() {
        stubSuccessfulUpload();

        ImageUploadResponse result = service.uploadImage(HASH, source());

        assertThat(result).isSameAs(response);
        InOrder order = inOrder(repository, storage);
        order.verify(repository).save(any(Image.class));
        order.verify(storage).upload(any(), eq((long) DATA.length), eq(HASH), eq(MIME));
    }

    @Test
    void uploadDeletesObjectWhenTransactionRollsBack() {
        stubSuccessfulUpload();
        when(repository.existsByHash(HASH)).thenReturn(false);

        service.uploadImage(HASH, source());
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete(HASH);
    }

    @Test
    void uploadKeepsObjectWhenTransactionCommits() {
        stubSuccessfulUpload();

        service.uploadImage(HASH, source());
        completeTransaction(TransactionSynchronization.STATUS_COMMITTED);

        verify(storage, never()).delete(anyString());
    }

    @Test
    void uploadKeepsObjectOnRollbackWhenConcurrentUploadCommittedSameHash() {
        stubSuccessfulUpload();
        when(repository.existsByHash(HASH)).thenReturn(true);

        service.uploadImage(HASH, source());
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage, never()).delete(anyString());
    }

    private void stubSuccessfulUpload() {
        storageDrainsUploads();
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
    void uploadRejectsContentThatDoesNotMatchDeclaredHashAndCleansUpOnRollback() {
        String otherHash = HashUtils.sha256Hex(new byte[]{9});
        storageDrainsUploads();
        when(repository.findByHash(otherHash)).thenReturn(Optional.empty());
        when(storage.objectUri(otherHash)).thenReturn(OBJECT_URI);
        when(repository.save(any(Image.class))).thenReturn(image);
        when(repository.existsByHash(otherHash)).thenReturn(false);

        assertThatThrownBy(() -> service.uploadImage(otherHash, source())).isInstanceOf(FileIntegrityException.class);
        completeTransaction(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(storage).delete(otherHash);
        verify(storage, never()).presignedGetUrl(anyString(), anyString());
    }

    @Test
    void uploadPropagatesStorageFailureAfterSave() {
        when(repository.findByHash(HASH)).thenReturn(Optional.empty());
        when(storage.objectUri(HASH)).thenReturn(OBJECT_URI);
        when(repository.save(any(Image.class))).thenReturn(image);
        doThrow(new StorageException("boom", null)).when(storage).upload(any(), anyLong(), anyString(), anyString());

        assertThatThrownBy(() -> service.uploadImage(HASH, source())).isInstanceOf(StorageException.class);

        verify(repository).save(any(Image.class));
        verify(storage, never()).presignedGetUrl(anyString(), anyString());
    }

    @Test
    void uploadOfExistingHashReturnsExistingWithoutTouchingStorage() {
        when(repository.findByHash(HASH)).thenReturn(Optional.of(image));
        when(storage.presignedGetUrl(HASH, "pic.png")).thenReturn(PRESIGNED);
        when(mapper.toResponse(image, PRESIGNED)).thenReturn(response);

        assertThat(service.uploadImage(HASH, source())).isSameAs(response);

        verify(repository, never()).save(any());
        verify(storage, never()).upload(any(), anyLong(), anyString(), anyString());
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
