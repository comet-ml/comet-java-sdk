package ml.comet.experiment.model;

import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

@Data
public class ExperimentAssetLink {
    private String fileName;
    private long fileSize;
    private String runContext;
    private Integer step;
    private boolean remote = false;
    private String link;
    private String compressedAssetLink;
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
    private List<String> tags;
    private String curlDownload;

}
