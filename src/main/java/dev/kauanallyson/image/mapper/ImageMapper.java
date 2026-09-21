package dev.kauanallyson.image.mapper;

import dev.kauanallyson.image.dto.ImageResponse;
import dev.kauanallyson.image.dto.ImageUploadResponse;
import dev.kauanallyson.image.model.Image;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.net.URI;

@Mapper(componentModel = "spring")
public interface ImageMapper {
    @Mapping(target = "uri", source = "presignedUri")
    ImageUploadResponse toResponse(Image image, URI presignedUri);

    ImageResponse toImageResponse(Image image);
}
