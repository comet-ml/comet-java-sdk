package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ml.comet.experiment.model.AssetType;

import java.io.File;
import java.util.Map;

/**
 * Describes artifact's asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArtifactAsset extends Asset {
    String artifactVersionId;

    /**
     * The copy constructor.
     *
     * @param asset the {@link Asset} to copy data from.
     */
    public ArtifactAsset(Asset asset) {
        this.setFile(asset.getFile());
        this.setFileLikeData(asset.getFileLikeData());
        this.setFileExtension(asset.getFileExtension());
        this.fileName = asset.fileName;
        this.type = asset.type;
        this.overwrite = asset.overwrite;
        this.metadata = asset.metadata;
    }
}
