package it.bz.sta.lf.storage;


import io.minio.*;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


import java.io.InputStream;
import java.time.Duration;
import java.util.Map;


@Service
public class S3StorageService {
    private final MinioClient client;
    private final String bucket;


    public S3StorageService(
            @Value("${s3.endpoint}") String endpoint,
            @Value("${s3.accessKey}") String accessKey,
            @Value("${s3.secretKey}") String secretKey,
            @Value("${s3.bucket}") String bucket) throws Exception {
        this.bucket = bucket;
        this.client = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
    }


    public void put(String objectKey, InputStream in, long size, String contentType) throws Exception {
        client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .stream(in, size, -1)
                .contentType(contentType)
                .build());
    }


    public String presignGet(String objectKey, Duration ttl) throws Exception {
        return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .method(Method.GET)
                .expiry((int) ttl.toSeconds())
                .build());
    }


    public InputStream get(String objectKey) throws Exception {
        return client.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .build());
    }


    public void delete(String objectKey) throws Exception {
        client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
    }
}