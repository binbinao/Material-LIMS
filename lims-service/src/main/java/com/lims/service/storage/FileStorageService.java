package com.lims.service.storage;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件存储服务。
 * 优先用 MinIO；当 MinIO 不可达（dev / 离线），自动降级到本地文件系统 ${java.io.tmpdir}/lims-files/。
 */
@Slf4j
@Service
public class FileStorageService {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;
    @Value("${minio.access-key:minioadmin}")
    private String accessKey;
    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;
    @Value("${minio.bucket:lims}")
    private String bucket;
    @Value("${minio.enabled:false}")
    private boolean minioEnabled;

    @Value("${storage.local-base:${java.io.tmpdir}/lims-files}")
    private String localBase;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        if (!minioEnabled) {
            log.info("MinIO disabled (minio.enabled=false). Falling back to local filesystem at {}", localBase);
            ensureLocalDir();
            return;
        }
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            log.info("MinIO ready. endpoint={}, bucket={}", endpoint, bucket);
        } catch (Exception e) {
            log.warn("MinIO unavailable, falling back to local filesystem: {}", e.getMessage());
            minioClient = null;
            ensureLocalDir();
        }
    }

    private void ensureLocalDir() {
        try {
            Files.createDirectories(Paths.get(localBase));
        } catch (IOException ignored) {}
    }

    /**
     * 上传文件，返回访问 URL（MinIO 预签名或 file://）
     */
    public String upload(Path source, String objectKeyPrefix) {
        String objectName = objectKeyPrefix + "/" + UUID.randomUUID() + "_" + source.getFileName();
        if (minioClient != null) {
            try (InputStream is = Files.newInputStream(source)) {
                String contentType = URLConnection.guessContentTypeFromName(source.getFileName().toString());
                if (contentType == null) contentType = "application/octet-stream";
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket).object(objectName)
                        .stream(is, Files.size(source), -1)
                        .contentType(contentType)
                        .build());
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucket)
                                .object(objectName)
                                .expiry(7, TimeUnit.DAYS)
                                .build());
                log.info("Uploaded to MinIO: {}", objectName);
                return url;
            } catch (MinioException | IOException | java.security.GeneralSecurityException e) {
                log.warn("MinIO upload failed, falling back to local: {}", e.getMessage());
            }
        }
        // local fallback
        try {
            Path target = Paths.get(localBase).resolve(objectName.replace('/', '_'));
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return target.toUri().toString();
        } catch (IOException e) {
            throw new RuntimeException("Local file storage failed: " + e.getMessage(), e);
        }
    }
}
