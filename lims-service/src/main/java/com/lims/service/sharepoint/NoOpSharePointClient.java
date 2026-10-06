package com.lims.service.sharepoint;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(SharePointClient.class)
public class NoOpSharePointClient implements SharePointClient {

    @Override public boolean isEnabled() { return false; }

    @Override public SharePointUploadResult uploadDocx(String localPath, SharePointPath path) {
        throw new SharePointException("SharePoint integration is not enabled in this environment");
    }

    @Override public byte[] downloadDocx(String sharepointFileId) {
        throw new SharePointException("SharePoint integration is not enabled in this environment");
    }

    @Override public String composeEditUrl(String sharepointFileId) {
        throw new SharePointException("SharePoint integration is not enabled in this environment");
    }
}