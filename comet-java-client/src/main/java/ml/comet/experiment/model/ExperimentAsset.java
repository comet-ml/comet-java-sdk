package ml.comet.experiment.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents asset associated with particular experiment.
 */
@Data
@NoArgsConstructor
public class ExperimentAsset {
    private String assetId;
    private String type;
    private String fileName;
    private String link;
    private boolean remote;
    private Instant createdAt;
    private String metadataJson;
    private String context;
    private Long fileSize;
    private Long step;
    private String curlDownload;
}
