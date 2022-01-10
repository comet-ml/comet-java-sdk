package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactAsset;

/**
 * Describes artifact's asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArtifactAssetImpl extends AssetImpl implements ArtifactAsset {
    String artifactVersionId;

    /**
     * The copy constructor.
     *
     * @param asset the {@link AssetImpl} to copy data from.
     */
    public ArtifactAssetImpl(AssetImpl asset) {
        this.setFile(asset.getFile());
        this.setFileLikeData(asset.getFileLikeData());
        this.setFileExtension(asset.getFileExtension());
        this.logicalPath = asset.getLogicalPath();
        this.type = asset.getType();
        this.overwrite = asset.getOverwrite();
        this.metadata = asset.getMetadata();
    }
}
