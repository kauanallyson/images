package dev.kauanallyson.image.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean(destroyMethod = "close")
    public S3Client s3Client(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.endpoint:}") String endpoint
    ) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim().replaceAll("/+$", "")))
                    .forcePathStyle(true);
        }

        return builder.build();
    }
}