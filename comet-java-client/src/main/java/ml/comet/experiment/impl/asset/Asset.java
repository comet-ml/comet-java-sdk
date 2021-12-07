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
public class Asset {
    private File file;
    private byte[] fileLikeData;
    private String fileExtension;

    String fileName;
    AssetType type;
    Boolean overwrite;
    Long step;
    Long epoch;
    String context;
    String groupingName;
    Map<String, Object> metadata;

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
