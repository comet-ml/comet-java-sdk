package ml.comet.experiment.impl.asset;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.context.ExperimentContext;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Describes asset data.
 */
@ToString(onlyExplicitlyIncluded = true)
public class AssetImpl implements Asset {
    @ToString.Include
    @Getter
    @Setter
    private File rawFile;
    @Getter
    @Setter
    private byte[] rawFileLikeData;
    @Getter
    @Setter
    private String fileExtension;

    @ToString.Include
    @Setter
    String logicalPath;
    @ToString.Include
    @Setter
    String type;
    @ToString.Include
    @Getter
    @Setter
    Boolean overwrite;
    @Setter
    String groupingName;

    @Getter
    ExperimentContext context;
    @Setter
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

    @Override
    public String getLogicalPath() {
        return this.logicalPath;
    }

    @Override
    public String getType() {
        return this.type;
    }

    public Optional<String> getGroupingName() {
        return Optional.ofNullable(this.groupingName);
    }
}
