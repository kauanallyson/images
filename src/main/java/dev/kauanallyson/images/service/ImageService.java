package dev.kauanallyson.images.service;

import dev.kauanallyson.images.dto.ImageResponse;
import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.ImageNotFoundException;
import dev.kauanallyson.images.mapper.ImageMapper;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.storage.ImageStorage;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.validation.FileValidator;
import dev.kauanallyson.images.validation.ValidatedUpload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

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
    public ImageUploadResponse uploadImage(String hash, MultipartFile file) {
        ValidatedUpload upload = fileValidator.validate(hash, file);

        Optional<Image> existing = imageRepository.findByHash(upload.hash());
        if (existing.isPresent()) {
            return withPresignedUrl(existing.get());
        }

        Image saved = imageRepository.save(Image.of(
                upload.hash(), upload.originalFileName(), upload.mimeType(), storage.objectUri(upload.hash())));
        storage.upload(upload.data(), upload.hash(), upload.mimeType());
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
            storage.delete(image.getHash());
        });
    }

    private ImageUploadResponse withPresignedUrl(Image image) {
        return imageMapper.toResponse(image, storage.presignedGetUrl(image.getHash(), image.getFileName()));
    }
}
