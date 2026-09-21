package dev.kauanallyson.image.repository;

import dev.kauanallyson.image.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {
    boolean existsByFileHash(String fileHash);
    Optional<Image> findByHash(String hash);
}
