package dev.kauanallyson.images.model;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Entity
@Table(name = "images")
public final class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, unique = true)
    private UUID uuid;

    @Column(nullable = false, unique = true)
    private String hash;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String contentType;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public static Image of(String hash, String fileName, String contentType) {
        Image image = new Image();
        image.hash = hash;
        image.fileName = fileName;
        image.contentType = contentType;
        return image;
    }
}
