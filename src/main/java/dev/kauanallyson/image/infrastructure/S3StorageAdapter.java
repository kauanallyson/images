package dev.kauanallyson.image.infrastructure;

import dev.kauanallyson.image.ports.StoragePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public final class S3StorageAdapter implements StoragePort {

    private final S3Client s3Client;
    private final String bucketName;
    private final String region;
    private final String endpoint;

    public S3StorageAdapter(
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.endpoint:}") String endpoint
    ) {
        this.bucketName = bucketName;
        this.region = region;
        this.endpoint = endpoint == null ? "" : endpoint.trim().replaceAll("/+$", "");

        S3ClientBuilder builder = S3Client.builder().region(Region.of(this.region));
        if (!this.endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(this.endpoint))
                    .forcePathStyle(true);
        }
        this.s3Client = builder.build();
    }

    @Override
    public URI uploadFile(byte[] fileData, String fileName, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileData));

        String urlString = endpoint.isEmpty()
                ? String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, fileName)
                : String.format("%s/%s/%s", endpoint, bucketName, fileName);
        try {
            return new URI(urlString);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
