package dev.kauanallyson.images.storage;

import dev.kauanallyson.images.exceptions.StorageException;
import dev.kauanallyson.images.storage.ImageStorage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
@Component
public final class S3ImageStorage implements ImageStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final Duration presignTtl;

    public S3ImageStorage(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.presign-ttl}") Duration presignTtl
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.presignTtl = presignTtl;
    }

    @Override
    public URI objectUri(String key) {
        try {
            return s3Client.utilities()
                    .getUrl(b -> b.bucket(bucketName).key(key))
                    .toURI();
        } catch (URISyntaxException e) {
            throw new StorageException("Storage returned an invalid URL for object '" + key + "'", e);
        }
    }

    @Override
    public void upload(InputStream content, long size, String key, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromInputStream(content, size));
            log.info("Object '{}' uploaded successfully to bucket '{}'", key, bucketName);
        } catch (SdkException e) {
            throw new StorageException("Failed to upload object '" + key + "' to storage", e);
        }
    }

    @Override
    public void delete(String key) {
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

    @Override
    public URI presignedGetUrl(String key, String downloadFileName) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .responseContentDisposition(contentDisposition(downloadFileName))
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(presignTtl)
                .getObjectRequest(getRequest)
                .build();

        try {
            return s3Presigner.presignGetObject(presignRequest).url().toURI();
        } catch (SdkException e) {
            throw new StorageException("Failed to presign GET URL for object '" + key + "'", e);
        } catch (URISyntaxException e) {
            throw new StorageException("Storage returned an invalid presigned URL for object '" + key + "'", e);
        }
    }

    private static String contentDisposition(String fileName) {
        ContentDisposition.Builder builder = ContentDisposition.attachment();
        if (fileName != null && !fileName.isBlank()) {
            builder.filename(fileName, StandardCharsets.UTF_8);
        }
        return builder.build().toString();
    }
}
