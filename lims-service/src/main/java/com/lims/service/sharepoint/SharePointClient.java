package com.lims.service.sharepoint;

public interface SharePointClient {

    boolean isEnabled();

    SharePointUploadResult uploadDocx(String localPath, SharePointPath path);

    byte[] downloadDocx(String sharepointFileId);

    String composeEditUrl(String sharepointFileId);

    record SharePointUploadResult(String fileId, String editUrl) {}
}