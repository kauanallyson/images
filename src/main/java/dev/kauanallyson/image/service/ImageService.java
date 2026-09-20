package dev.kauanallyson.image.service;

import dev.kauanallyson.image.model.Image;
import dev.kauanallyson.image.ports.StoragePort;
import dev.kauanallyson.image.repository.ImageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

@Service
public final class ImageService {
    private final StoragePort storage;
    private final ImageRepository imageRepository;

    public ImageService(StoragePort storage, ImageRepository imageRepository) {
        this.storage = storage;
        this.imageRepository = imageRepository;
    }

    public Image uploadImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new RuntimeException("Empty file");
        }

        // add validation of the file type to confirm it is an image
        byte[] fileData;
        try {
            fileData = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();
        URI uri = storage.uploadFile(fileData, originalName, contentType);

        Image image = new Image();
        image.setOriginalName(originalName);
        image.setContentType(contentType);
        image.setUri(uri);

        return imageRepository.save(image);
    }

    public Page<Image> getAllImages(Pageable pageable){
        return imageRepository.findAll(pageable);
    }

    public Image findImageByUUID(UUID uuid){
        return imageRepository.findById(uuid)
                .orElseThrow(()-> new RuntimeException("Image not found"));
    }
}
