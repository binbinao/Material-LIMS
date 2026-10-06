package com.lims.service.sharepoint;

import com.lims.model.entity.Report;
import com.lims.model.entity.Request;
import org.springframework.stereotype.Component;

@Component
public class SharePointPathResolver {

    private final SharePointProperties props;

    public SharePointPathResolver(SharePointProperties props) {
        this.props = props;
    }

    public SharePointPath resolve(Request parent, Report report) {
        String filename = parent.getRequestNo() + "_" + report.getVersionNumber() + ".docx";
        String path = switch (props.strategy()) {
            case "flat" -> "";
            default -> "%s/%04d/%02d/".formatted(
                props.folderRoot(),
                parent.getCreatedAt().getYear(),
                parent.getCreatedAt().getMonthValue());
        };
        return new SharePointPath(path, filename);
    }
}