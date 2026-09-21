package dev.kauanallyson.images.service;

import dev.kauanallyson.images.dto.ImageResponse;
import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.*;
import dev.kauanallyson.images.mapper.ImageMapper;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.ports.StoragePort;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.utils.HashUtils;
import dev.kauanallyson.images.utils.MediaTypeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;

@Service
public class ImageService {
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp");

    private final StoragePort storage;
    private final ImageMapper imageMapper;
    private final ImageRepository imageRepository;

    public ImageService(StoragePort storage, ImageMapper imageMapper, ImageRepository imageRepository) {
        this.storage = storage;
        this.imageMapper = imageMapper;
        this.imageRepository = imageRepository;
    }

    @Transactional
    public ImageUploadResponse uploadImage(String hash, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException();
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new FileReadException(e);
        }

        // verify if the magic bytes matches
        String contentType = MediaTypeUtils.detectMimeType(bytes);
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new UnsupportedMediaTypeException(contentType, ALLOWED_TYPES);
        }

        // compare with the request header hash
        if (!hash.equalsIgnoreCase(HashUtils.sha256Hex(bytes))) {
            throw new FileIntegrityException();
        }

        Image image = imageRepository.findByHash(hash)
                .orElseGet(() -> {
                    URI uri = storage.uploadFile(bytes, hash, contentType);
                    return imageRepository.save(Image.of(hash, file.getOriginalFilename(), contentType, uri));
                });

        return toResponse(image);
    }

    private ImageUploadResponse toResponse(Image image) {
        return imageMapper.toResponse(image, storage.presignedGetUrl(image.getHash()));
    }

    public Page<ImageResponse> getAllImages(Pageable pageable) {
        return imageRepository.findAll(pageable).map(imageMapper::toImageResponse);
    }

    public ImageUploadResponse findImageByHash(String hash) {
        return imageRepository.findByHash(hash)
                .map(this::toResponse)
                .orElseThrow(() -> new ImageNotFoundException(hash));
    }

    @Transactional
    public void deleteImageByHash(String hash) {
        imageRepository.findByHash(hash).ifPresent(image -> {
            storage.deleteFile(image.getHash());
            imageRepository.delete(image);
        });
    }
}