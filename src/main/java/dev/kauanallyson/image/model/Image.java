package dev.kauanallyson.image.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.net.URI;
import java.sql.Timestamp;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "images")
public final class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    private String originalName;
    private String contentType;
    private URI uri;

    @CreationTimestamp
    private Timestamp createdAt;
}
