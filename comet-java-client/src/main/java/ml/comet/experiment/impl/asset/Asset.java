package ml.comet.experiment.impl.asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.context.ExperimentContext;

import java.io.File;
import java.util.Map;

/**
 * Describes asset data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Asset {
    private File file;
    private byte[] fileLikeData;
    private String fileName;
    private AssetType type;
    private boolean overwrite;
    private long step;
    private long epoch;
    private String groupingName;
    private Map<String, Object> metadata;
    private String assetId;
    private String fileExtension;
    private String context;
    private boolean remote;

    /**
     * Updates this asset with values from provided {@link ExperimentContext}.
     *
     * @param context the {@link ExperimentContext} with context values.
     */
    public void setExperimentContext(ExperimentContext context) {
        this.step = context.getStep();
        this.epoch = context.getEpoch();
        this.context = context.getContext();
    }
}
