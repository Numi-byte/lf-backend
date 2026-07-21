package it.bz.sta.lf.storage;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;


import java.io.InputStream;
import java.net.URI;
import java.time.Duration;


@Service
public class S3StorageService {
    private final S3Client client;
    private final S3Presigner presigner;
    private final String bucket;


    public S3StorageService(
            @Value("${s3.endpoint}") String endpoint,
            @Value("${s3.accessKey}") String accessKey,
            @Value("${s3.secretKey}") String secretKey,
            @Value("${s3.bucket}") String bucket,
            @Value("${s3.region:eu-west-1}") String region) {
            if (bucket == null || bucket.isBlank()) {
                throw new IllegalArgumentException("s3.bucket must be configured");
            }
        this.bucket = bucket;

        URI endpointUri = URI.create(endpoint);
        Region awsRegion = Region.of(region == null || region.isBlank() ? "eu-west-1" : region);
        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)
        );
        S3Configuration s3Configuration = S3Configuration.builder()
                .pathStyleAccessEnabled(true)
                .build();

        this.client = S3Client.builder()
                .endpointOverride(endpointUri)
                .region(awsRegion)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration)
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpointUri)
                .region(awsRegion)
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(s3Configuration)
                .build();

    }


    public void put(String objectKey, InputStream in, long size, String contentType) {
        client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(objectKey)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(in, size));
    }


    public String presignGet(String objectKey, Duration ttl) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(getObjectRequest)
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }


    public InputStream get(String objectKey) {
        return client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build());
    }


    public void delete(String objectKey) {
        client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
    }
}