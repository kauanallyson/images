package dev.kauanallyson.images.service;

import dev.kauanallyson.images.dto.ImageResponse;
import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.ImageNotFoundException;
import dev.kauanallyson.images.exceptions.StorageException;
import dev.kauanallyson.images.mapper.ImageMapper;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.storage.ImageStorage;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.validation.FileValidator;
import dev.kauanallyson.images.validation.ValidatedUpload;
import dev.kauanallyson.images.utils.HashUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Optional;

@Slf4j
@Service
public class ImageService {
    private final ImageStorage storage;
    private final ImageMapper imageMapper;
    private final ImageRepository imageRepository;
    private final FileValidator fileValidator;

    public ImageService(ImageStorage storage, ImageMapper imageMapper, ImageRepository imageRepository,
                        FileValidator fileValidator) {
        this.storage = storage;
        this.imageMapper = imageMapper;
        this.imageRepository = imageRepository;
        this.fileValidator = fileValidator;
    }

    @Transactional
    public ImageUploadResponse uploadImage(String hash, UploadSource source) {
        ValidatedUpload upload = fileValidator.validate(hash, source);

        Optional<Image> existing = imageRepository.findByHash(upload.hash());
        if (existing.isPresent()) {
            return withPresignedUrl(existing.get());
        }

        Image saved = imageRepository.save(Image.of(
                upload.hash(), upload.originalFileName(), upload.mimeType(), storage.objectUri(upload.hash())));
        // the content is hashed while streaming to storage, so a mismatch is only known after the upload;
        // throwing rolls the row back and the rollback hook removes the object
        MessageDigest digest = HashUtils.sha256();
        storage.upload(new DigestInputStream(upload.content(), digest), upload.size(), upload.hash(), upload.mimeType());
        deleteObjectOnRollback(upload.hash());
        fileValidator.verifyHash(upload, HashUtils.hex(digest));
        return withPresignedUrl(saved);
    }

    public Page<ImageResponse> getAllImages(Pageable pageable) {
        return imageRepository.findAll(pageable).map(imageMapper::toImageResponse);
    }

    public ImageUploadResponse findImageByHash(String hash) {
        return imageRepository.findByHash(hash)
                .map(this::withPresignedUrl)
                .orElseThrow(() -> new ImageNotFoundException(hash));
    }

    @Transactional
    public void deleteImageByHash(String hash) {
        imageRepository.findByHash(hash).ifPresent(image -> {
            imageRepository.delete(image);
            deleteObjectAfterCommit(image.getHash());
        });
    }

    // the key is the content hash, so skip cleanup if a concurrent upload of the same file committed the row
    private void deleteObjectOnRollback(String hash) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED && !imageRepository.existsByHash(hash)) {
                    storage.delete(hash);
                }
            }
        });
    }

    // a failed object delete only leaves an orphan, so it is logged instead of failing the committed request
    private void deleteObjectAfterCommit(String hash) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (imageRepository.existsByHash(hash)) {
                    return;
                }
                try {
                    storage.delete(hash);
                } catch (StorageException e) {
                    log.warn("Image '{}' deleted but its object could not be removed from storage", hash, e);
                }
            }
        });
    }

    private ImageUploadResponse withPresignedUrl(Image image) {
        return imageMapper.toResponse(image, storage.presignedGetUrl(image.getHash(), image.getFileName()));
    }
}
