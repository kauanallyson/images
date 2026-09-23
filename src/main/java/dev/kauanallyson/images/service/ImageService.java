package dev.kauanallyson.images.service;

import dev.kauanallyson.images.dto.ImageResponse;
import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.*;
import dev.kauanallyson.images.mapper.ImageMapper;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.ports.StoragePort;
import dev.kauanallyson.images.repository.ImageRepository;
import dev.kauanallyson.images.utils.FileMetadata;
import dev.kauanallyson.images.utils.HashUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

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
        // TODO: create file validator class
        validateFileIsNotEmpty(file);

        byte[] fileData = getFileData(file);

        validateRequestHashWithFileDataHash(hash, fileData);

        String mimeType = getFileMimeType(fileData);

        Optional<Image> existing = imageRepository.findByHash(hash);
        if (existing.isPresent()) {
            return withPresignedUrl(existing.get());
        }

        URI uri = storage.uploadFile(fileData, hash, mimeType);
        Image savedImage = imageRepository.save(Image.of(hash, file.getOriginalFilename(), mimeType, uri));
        return withPresignedUrl(savedImage);
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
            storage.deleteFile(image.getHash());
            imageRepository.delete(image);
        });
    }

    private void validateFileIsNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new EmptyFileException();
        }
    }

    private byte[] getFileData(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new FileReadException(e);
        }
    }

    private String getFileMimeType(byte[] fileData) {
        String mimeType = FileMetadata.mimeType(fileData);
        if (!ALLOWED_TYPES.contains(mimeType)) {
            throw new UnsupportedMediaTypeException(mimeType, ALLOWED_TYPES);
        }
        return mimeType;
    }

    private void validateRequestHashWithFileDataHash(String hash, byte[] fileData) {
        if (!hash.equalsIgnoreCase(HashUtils.sha256Hex(fileData))) {
            throw new FileIntegrityException();
        }
    }

    private ImageUploadResponse withPresignedUrl(Image image) {
        return imageMapper.toResponse(image, storage.presignedGetUrl(image.getHash(), image.getFileName()));
    }
}