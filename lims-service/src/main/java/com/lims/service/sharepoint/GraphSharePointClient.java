package com.lims.service.sharepoint;

import com.lims.service.sync.MicrosoftGraphClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("'${sharepoint.enabled:false}'.equals('true') and '${azure.ad.enabled:false}'.equals('true')")
public class GraphSharePointClient implements SharePointClient {

    private static final String DOCX_MIME =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private final MicrosoftGraphClient graph;
    private final SharePointProperties props;

    public GraphSharePointClient(MicrosoftGraphClient graph, SharePointProperties props) {
        this.graph = graph;
        this.props = props;
    }

    @Override public boolean isEnabled() { return true; }

    @Override
    public SharePointUploadResult uploadDocx(String localPath, SharePointPath path) {
        String driveId;
        try {
            driveId = graph.resolveDriveId();
        } catch (Exception e) {
            throw new SharePointException("drive resolution failed: " + e.getMessage(), e);
        }

        String parentPath = path.driveRelativePath();
        String filename = path.filename();
        byte[] body;
        try { body = Files.readAllBytes(Path.of(localPath)); }
        catch (IOException e) {
            throw new SharePointException("read local docx failed: " + e.getMessage(), e);
        }

        try {
            Map<String, Object> item = graph.putDriveItemContent(driveId, parentPath, filename, body, DOCX_MIME);
            String id = (String) item.get("id");
            String webUrl = (String) item.get("webUrl");
            return new SharePointUploadResult(id, webUrl + props.editUrlSuffix());
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("409")) {
                // 409 = file already exists. Look it up by path and return existing metadata.
                log.info("SharePoint upload 409; looking up existing item at {}/{}", parentPath, filename);
                Map<String, Object> existing = graph.getDriveItemByPath(driveId, parentPath, filename);
                String id = (String) existing.get("id");
                String webUrl = (String) existing.get("webUrl");
                return new SharePointUploadResult(id, webUrl + props.editUrlSuffix());
            }
            throw new SharePointException("Graph upload failed: " + msg, e);
        }
    }

    @Override
    public byte[] downloadDocx(String sharepointFileId) {
        try {
            String driveId = graph.resolveDriveId();
            return graph.getDriveItemContent(driveId, sharepointFileId);
        } catch (SharePointException e) {
            throw e;
        } catch (Exception e) {
            throw new SharePointException("download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String composeEditUrl(String sharepointFileId) {
        try {
            String driveId = graph.resolveDriveId();
            Map<String, Object> item = graph.getDriveItem(driveId, sharepointFileId, "webUrl");
            String webUrl = (String) item.get("webUrl");
            return webUrl + props.editUrlSuffix();
        } catch (Exception e) {
            throw new SharePointException("compose edit url failed: " + e.getMessage(), e);
        }
    }
}