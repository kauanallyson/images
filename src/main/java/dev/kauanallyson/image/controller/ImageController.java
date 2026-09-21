package dev.kauanallyson.image.controller;

import dev.kauanallyson.image.dto.ImageResponse;
import dev.kauanallyson.image.dto.ImageUploadResponse;
import dev.kauanallyson.image.service.ImageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;



@RestController
@RequestMapping("/api/v1/images")
public class ImageController {
    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @RequestHeader("X-File-SHA256") String hash,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageService.uploadImage(hash, file));
    }

    @GetMapping
    public ResponseEntity<PagedModel<ImageResponse>> getAllImages(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<ImageResponse> page = imageService.getAllImages(pageable);
        return ResponseEntity.ok(new PagedModel<>(page));
    }

    @GetMapping("/{hash}")
    public ResponseEntity<ImageUploadResponse> findImageByHash(@PathVariable String hash) {
        return ResponseEntity.ok(imageService.findImageByHash(hash));
    }

    @DeleteMapping("/{hash}")
    public ResponseEntity<HttpStatus> deleteImageByHash(@PathVariable String hash) {
        imageService.deleteImageByHash(hash);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}