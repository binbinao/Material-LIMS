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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 文件存储服务。
 * 优先用 MinIO；当 MinIO 不可达（dev / 离线），自动降级到本地文件系统 ${java.io.tmpdir}/lims-files/。
 *
 * Issue #9 hardening:
 *   - Server-side MIME map (MIME_BY_EXT) — never trust URLConnection's guess
 *     which varies by JRE and can be tricked by crafted extensions.
 *   - Allowed-extension whitelist (ALLOWED_EXTS) — rejects .html / .htm /
 *     .svg / .xhtml (XSS vectors) and .exe / .bat / .sh / .msi / .jar
 *     (RCE vectors).
 *   - sanitizeFilename strips control chars and path separators before
 *     the source filename is concatenated into the MinIO object name.
 *   - Presigned URL adds response-content-disposition=attachment so
 *     browsers download rather than render the file, defeating any
 *     text/html content-type that slipped through.
 */
@Slf4j
@Service
public class FileStorageService {

    /** Server-side MIME map keyed by lower-case extension (with leading dot). */
    private static final Map<String, String> MIME_BY_EXT = Map.ofEntries(
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".ppt", "application/vnd.ms-powerpoint"),
            Map.entry(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".csv", "text/csv"),
            Map.entry(".json", "application/json"),
            Map.entry(".xml", "application/xml"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".zip", "application/zip"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".webm", "video/webm")
    );

    /** Allow-list of accepted upload extensions. */
    private static final Set<String> ALLOWED_EXTS = Set.of(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".csv", ".json", ".xml",
            ".png", ".jpg", ".jpeg", ".gif", ".webp",
            ".zip", ".mp4", ".webm"
    );

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
     * Sanitize a source filename before it's used as part of an object
     * key. Strips ASCII control characters (incl. NULL), slashes, and
     * backslashes so a filename like {@code ../../etc/passwd} or
     * {@code evil .html} cannot escape the configured
     * {@code objectKeyPrefix}.
     */
    static String sanitizeFilename(String raw) {
        if (raw == null || raw.isEmpty()) return "file";
        String clean = raw.replaceAll("[\\p{Cntrl}/\\\\]", "_");
        clean = clean.replaceAll("_+", "_").replaceAll("^[._]+|[._]+$", "");
        if (clean.length() > 80) clean = clean.substring(0, 80);
        return clean.isEmpty() ? "file" : clean;
    }

    /**
     * Look up the server-side MIME type for an extension, lower-cased
     * with a leading dot. Returns {@code application/octet-stream} for
     * any unknown or absent extension — the browser will always download
     * rather than render an unknown file.
     */
    static String mimeFor(String filename) {
        if (filename == null) return "application/octet-stream";
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return "application/octet-stream";
        String ext = filename.substring(dot).toLowerCase(Locale.ROOT);
        return MIME_BY_EXT.getOrDefault(ext, "application/octet-stream");
    }

    /**
     * Throw if the filename's extension isn't on the allow-list. Catches
     * uploads of {@code .html}, {@code .exe}, {@code .bat} etc. before
     * they reach the object store.
     */
    static void assertAllowedExtension(String filename) {
        if (filename == null) {
            throw new IllegalArgumentException("Filename is required");
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalArgumentException("File has no extension: " + filename);
        }
        String ext = filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTS.contains(ext)) {
            throw new IllegalArgumentException(
                    "File extension '" + ext + "' is not allowed. Allowed: " + ALLOWED_EXTS);
        }
    }

    /**
     * 上传文件，返回访问 URL（MinIO 预签名或 file://）
     */
    public String upload(Path source, String objectKeyPrefix) {
        String rawName = source.getFileName().toString();
        assertAllowedExtension(rawName);
        String safeName = sanitizeFilename(rawName);
        String objectName = objectKeyPrefix + "/" + UUID.randomUUID() + "_" + safeName;
        if (minioClient != null) {
            try (InputStream is = Files.newInputStream(source)) {
                String contentType = mimeFor(rawName);
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket).object(objectName)
                        .stream(is, Files.size(source), -1)
                        .contentType(contentType)
                        .build());
                // Issue #9: force the browser to download rather than render
                // the file. MinIO honors response-content-disposition as an
                // extra query parameter on the presigned URL.
                String url = minioClient.getPresignedObjectUrl(
                        GetPresignedObjectUrlArgs.builder()
                                .method(Method.GET)
                                .bucket(bucket)
                                .object(objectName)
                                .expiry(7, TimeUnit.DAYS)
                                .extraQueryParams(Map.of(
                                        "response-content-disposition",
                                        "attachment; filename=\"" + safeName + "\""))
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
