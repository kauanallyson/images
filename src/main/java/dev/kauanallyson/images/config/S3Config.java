package dev.kauanallyson.images.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

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

    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.endpoint:}") String endpoint
    ) {
        S3Presigner.Builder builder = S3Presigner.builder().region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder.endpointOverride(URI.create(endpoint.trim().replaceAll("/+$", "")))
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
        }

        return builder.build();
    }
}
