package com.example.photogallery.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@ConditionalOnProperty(
    name = {
        "B2_ACCESS_KEY_ID",
        "B2_SECRET_ACCESS_KEY",
        "B2_REGION",
        "B2_S3_ENDPOINT"
    }
)
public class B2S3PresignerConfig {

    @Bean
    public S3Presigner s3Presigner(
        @Value("${B2_ACCESS_KEY_ID}") String accessKeyId,
        @Value("${B2_SECRET_ACCESS_KEY}") String secretAccessKey,
        @Value("${B2_REGION}") String region,
        @Value("${B2_S3_ENDPOINT}") String endpoint
    ) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(
            accessKeyId,
            secretAccessKey
        );

        return S3Presigner
            .builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .build();
    }
}
