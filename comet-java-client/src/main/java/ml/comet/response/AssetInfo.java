package ml.comet.response;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class AssetInfo {
    private String fileName;
    private long fileSize;
    private String runContext;
    private Integer step;
    private String link;
    private Timestamp createdAt;
    private String dir;
    private boolean canView = false;
    private boolean audio = false;
    private boolean video = false;
    private boolean histogram = false;
    private boolean image = false;
    private String type;
    private String metadata;
    private String assetId;
}
