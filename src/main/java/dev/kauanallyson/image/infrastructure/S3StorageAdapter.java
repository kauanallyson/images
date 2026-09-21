package dev.kauanallyson.image.infrastructure;

import dev.kauanallyson.image.exceptions.StorageException;
import dev.kauanallyson.image.ports.StoragePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public final class S3StorageAdapter implements StoragePort {

    private final S3Client s3Client;
    private final String bucketName;

    public S3StorageAdapter(
            S3Client s3Client,
            @Value("${aws.s3.bucket-name}") String bucketName
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    @Override
    public URI uploadFile(byte[] fileData, String key, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(fileData));
            log.info("Object '{}' uploaded successfully to bucket '{}'", key, bucketName);

            return s3Client.utilities()
                    .getUrl(b -> b.bucket(bucketName).key(key))
                    .toURI();
        } catch (SdkException e) {
            throw new StorageException("Failed to upload object '" + key + "' to storage", e);
        } catch (URISyntaxException e) {
            throw new StorageException("Storage returned an invalid URL for object '" + key + "'", e);
        }
    }

    @Override
    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        try {
            s3Client.deleteObject(request);
            log.info("Object '{}' deletion requested from bucket '{}'", key, bucketName);
        } catch (SdkException e) {
            throw new StorageException("Failed to delete object '" + key + "' from storage", e);
        }
    }
}