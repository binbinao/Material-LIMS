package com.lims.service.sharepoint;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sharepoint")
public record SharePointProperties(
    boolean enabled,
    String strategy,
    String hostname,
    String sitePath,
    String library,
    String folderRoot,
    String driveId,
    String editUrlSuffix
) {}