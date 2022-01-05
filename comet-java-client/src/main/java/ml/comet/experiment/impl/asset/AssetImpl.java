package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.model.AssetType;

import java.io.File;
import java.util.Map;

/**
 * Describes asset data.
 */
@Data
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class AssetImpl implements Asset {
    @ToString.Include
    private File file;
    private byte[] fileLikeData;
    private String fileExtension;

    @ToString.Include
    String fileName;
    @ToString.Include
    AssetType type;
    @ToString.Include
    Boolean overwrite;
    ExperimentContext experimentContext;
    Map<String, Object> metadata;

    @Override
    public void setExperimentContext(ExperimentContext context) {
        this.experimentContext = new ExperimentContext(context);
    }
}
