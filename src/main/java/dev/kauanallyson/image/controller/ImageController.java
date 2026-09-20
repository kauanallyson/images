package dev.kauanallyson.image.controller;

import dev.kauanallyson.image.model.Image;
import dev.kauanallyson.image.service.ImageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;


@RestController
@RequestMapping("/api/v1/images")
public final class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Image> uploadImage(@RequestPart("image") MultipartFile file) {
        Image image = imageService.uploadImage(file);
        return ResponseEntity.created(image.getUri()).build();
    }

    @GetMapping
    public ResponseEntity<PagedModel<Image>> getAllImages(
            @PageableDefault(size = 20, sort = "uuid", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<Image> page = imageService.getAllImages(pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<Image> findImageByUUID(@PathVariable String uuid){
        Image image = imageService.findImageByUUID(UUID.fromString(uuid));
        return ResponseEntity.ok(image);
    }
}