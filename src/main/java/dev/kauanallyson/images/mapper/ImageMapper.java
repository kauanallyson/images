package dev.kauanallyson.images.mapper;

import dev.kauanallyson.images.dto.ImageResponse;
import dev.kauanallyson.images.dto.ImageUploadResponse;
import dev.kauanallyson.images.model.Image;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.URI;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    @Mapping(target = "uri", source = "presignedUri")
    ImageUploadResponse toResponse(Image image, URI presignedUri);

    ImageResponse toImageResponse(Image image);
}
