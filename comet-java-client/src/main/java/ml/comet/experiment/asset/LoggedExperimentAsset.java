package ml.comet.experiment.asset;

import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.context.ExperimentContext;

import java.time.Instant;
import java.util.Map;

/**
 * Represents asset associated with particular experiment which already logged to the Comet.
 */
@Data
@NoArgsConstructor
@SuppressWarnings("unused")
public class LoggedExperimentAsset {
    private AssetType type;
    private String logicalPath;
    private String link;
    private Map<String, Object> metadata;
    private ExperimentContext experimentContext;

    private String assetId;
    private boolean remote;
    private Instant createdAt;
    private Long fileSize;
    private String curlDownload;
}
