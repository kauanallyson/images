package dev.kauanallyson.images.service;

import dev.kauanallyson.images.exceptions.StorageException;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.storage.ImageStorage;
import dev.kauanallyson.images.validation.ValidatedUpload;
import dev.kauanallyson.images.validation.VerifiedContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.util.Optional;

// keeps each image row and its storage object consistent: an object is kept only while a committed row references it
@Slf4j
@Component
public class ImageStore {
    private final ImageRepository repository;
    private final ImageStorage storage;

    public ImageStore(ImageRepository repository, ImageStorage storage) {
        this.repository = repository;
        this.storage = storage;
    }

    // returns the existing image for an already stored hash; throws FileIntegrityException if the content mismatches
    @Transactional
    public Image store(ValidatedUpload upload) {
        try (VerifiedContent content = upload.content()) {
            Optional<Image> existing = repository.findByHash(upload.hash());
            if (existing.isPresent()) {
                // the declared hash alone must not be enough to obtain an existing image
                content.readAndVerify();
                return existing.get();
            }

            Image saved = repository.save(Image.of(upload.hash(), upload.originalFileName(), upload.mimeType()));
            // the content is hashed while streaming to storage, so a mismatch is only known after the upload;
            // throwing rolls the row back and the rollback hook removes the object
            deleteObjectOnRollback(upload.hash());
            storage.upload(content, upload.size(), upload.hash(), upload.mimeType());
            content.verify();
            return saved;
        }
    }

    @Transactional(readOnly = true)
    public Optional<Image> find(String hash) {
        return repository.findByHash(hash);
    }

    @Transactional(readOnly = true)
    public Page<Image> findAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional
    public void remove(String hash) {
        repository.findByHash(hash).ifPresent(image -> {
            repository.delete(image);
            deleteObjectAfterCommit(image.getHash());
        });
    }

    public URI downloadUrl(Image image) {
        return storage.presignedGetUrl(image.getHash(), image.getFileName());
    }

    // the key is the content hash, so skip cleanup if a concurrent upload of the same file committed the row
    private void deleteObjectOnRollback(String hash) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED && !repository.existsByHash(hash)) {
                    deleteObject(hash);
                }
            }
        });
    }

    // a failed object delete only leaves an orphan, so it is logged instead of failing the committed request
    private void deleteObjectAfterCommit(String hash) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (!repository.existsByHash(hash)) {
                    deleteObject(hash);
                }
            }
        });
    }

    private void deleteObject(String hash) {
        try {
            storage.delete(hash);
        } catch (StorageException e) {
            log.warn("Object '{}' could not be removed from storage and is now orphaned", hash, e);
        }
    }
}
