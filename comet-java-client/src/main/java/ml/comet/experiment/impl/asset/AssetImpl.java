package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.asset.AssetType;
import ml.comet.experiment.context.ExperimentContext;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Describes asset data.
 */
@Data
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class AssetImpl implements Asset {
    @ToString.Include
    private File rawFile;
    private byte[] rawFileLikeData;
    private String fileExtension;

    @ToString.Include
    String logicalPath;
    @ToString.Include
    AssetType type;
    @ToString.Include
    Boolean overwrite;

    ExperimentContext context;
    Map<String, Object> metadata;

    public void setContext(ExperimentContext context) {
        this.context = new ExperimentContext(context);
    }

    @Override
    public Map<String, Object> getMetadata() {
        if (this.metadata != null) {
            return this.metadata;
        }
        return Collections.emptyMap();
    }

    @Override
    public Optional<File> getFile() {
        return Optional.ofNullable(this.rawFile);
    }

    @Override
    public Optional<byte[]> getFileLikeData() {
        return Optional.ofNullable(this.rawFileLikeData);
    }

    @Override
    public Optional<ExperimentContext> getExperimentContext() {
        return Optional.ofNullable(this.context);
    }
}
