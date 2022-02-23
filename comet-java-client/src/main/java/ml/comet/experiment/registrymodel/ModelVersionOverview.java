package ml.comet.experiment.registrymodel;

import lombok.Data;
import ml.comet.experiment.asset.LoggedExperimentAsset;

import java.time.Instant;
import java.util.List;

/**
 * Holds overview of specific version of the registry model.
 */
@Data
public class ModelVersionOverview {
    private String registryModelItemId;
    private String version;
    private String comment;
    private List<String> stages;
    private List<LoggedExperimentAsset> assets;
    private String userName;
    private Instant createdAt;
    private Instant lastUpdated;
    private String restApiUrl;
}
