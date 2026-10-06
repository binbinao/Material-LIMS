package com.lims.service.sharepoint;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Demo-mode mock that pretends to upload to SharePoint by copying the source file to a local
 * staging directory. The mapping fileId -> filePath is kept in-memory; downloads replay the
 * bytes from the copy.
 *
 * Wired by {@code @ConditionalOnProperty(lims.demo.enabled=true)}. Production never instantiates it.
 *
 * Staging directory default is {@code java.io.tmpdir}/lims-sharepoint-mock. Tests may inject
 * {@code @TempDir} via the {@code Path} constructor.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "lims.demo.enabled", havingValue = "true")
public class MockSharePointClient implements SharePointClient {

    private final Path stagingDir;
    private final ConcurrentMap<String, Path> stagedFiles = new ConcurrentHashMap<>();

    /** Spring production wiring: derive staging dir from java.io.tmpdir. */
    @Autowired
    public MockSharePointClient(@Value("${java.io.tmpdir:/tmp}") String tmpRoot) {
        this(Paths.get(tmpRoot, "lims-sharepoint-mock"));
    }

    /** Test / explicit wiring. */
    public MockSharePointClient(Path stagingDir) {
        this.stagingDir = stagingDir;
        try {
            Files.createDirectories(stagingDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create mock staging dir " + stagingDir, e);
        }
    }

    @Override public boolean isEnabled() { return true; }

    @Override
    public SharePointUploadResult uploadDocx(String localPath, SharePointPath path) {
        String fileId = "mock-" + UUID.randomUUID();
        Path target = stagingDir.resolve(fileId + ".docx");
        try {
            Files.copy(Paths.get(localPath), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new SharePointException("Mock: copy " + localPath + " -> " + target + " failed: " + e.getMessage(), e);
        }
        stagedFiles.put(fileId, target);
        String editUrl = "https://example.invalid/mock-sharepoint/" + fileId + "?action=edit";
        log.info("MockSharePoint upload: fileId={} path={} -> {}", fileId, path, target);
        return new SharePointUploadResult(fileId, editUrl);
    }

    @Override
    public byte[] downloadDocx(String sharepointFileId) {
        Path p = stagedFiles.get(sharepointFileId);
        if (p == null) {
            throw new SharePointException("Mock: unknown fileId " + sharepointFileId);
        }
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new SharePointException("Mock: read " + p + " failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String composeEditUrl(String sharepointFileId) {
        return "https://example.invalid/mock-sharepoint/" + sharepointFileId + "?action=edit";
    }
}