package dev.kauanallyson.images.service;

import dev.kauanallyson.images.dto.ImageResponse;
import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.exceptions.ImageNotFoundException;
import dev.kauanallyson.images.mapper.ImageMapper;
import dev.kauanallyson.images.model.Image;
import dev.kauanallyson.images.validation.FileValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ImageService {
    private final ImageStore store;
    private final ImageMapper imageMapper;
    private final FileValidator fileValidator;

    public ImageService(ImageStore store, ImageMapper imageMapper, FileValidator fileValidator) {
        this.store = store;
        this.imageMapper = imageMapper;
        this.fileValidator = fileValidator;
    }

    public ImageUploadResponse uploadImage(String hash, UploadSource source) {
        return withDownloadUrl(store.store(fileValidator.validate(hash, source)));
    }

    public Page<ImageResponse> getAllImages(Pageable pageable) {
        return store.findAll(pageable).map(imageMapper::toImageResponse);
    }

    public ImageUploadResponse findImageByHash(String hash) {
        return store.find(hash)
                .map(this::withDownloadUrl)
                .orElseThrow(() -> new ImageNotFoundException(hash));
    }

    public void deleteImageByHash(String hash) {
        store.remove(hash);
    }

    private ImageUploadResponse withDownloadUrl(Image image) {
        return imageMapper.toResponse(image, store.downloadUrl(image));
    }
}
