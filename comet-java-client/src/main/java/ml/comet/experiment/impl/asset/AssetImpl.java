package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.model.AssetType;

import java.io.File;
import java.util.Map;

/**
 * Describes asset data.
 */
@Data
@NoArgsConstructor
public class AssetImpl implements Asset {
    private File file;
    private byte[] fileLikeData;
    private String fileExtension;

    String fileName;
    AssetType type;
    Boolean overwrite;
    ExperimentContext experimentContext;
    Map<String, Object> metadata;

    @Override
    public void setExperimentContext(ExperimentContext context) {
        this.experimentContext = new ExperimentContext(context);
    }
}
