package dev.kauanallyson.images.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.List;

@ConfigurationProperties(prefix = "images")
public record ImageProperties(@DefaultValue({"image/jpeg", "image/png", "image/webp"}) List<String> allowedTypes) {
}
