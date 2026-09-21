package dev.kauanallyson.images.repository;

import dev.kauanallyson.images.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ImageRepository extends JpaRepository<Image, UUID> {
    boolean existsByHash(String fileHash);

    Optional<Image> findByHash(String hash);
}
