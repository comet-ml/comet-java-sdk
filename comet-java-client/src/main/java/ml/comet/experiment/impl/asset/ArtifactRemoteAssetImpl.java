package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ml.comet.experiment.asset.RemoteAsset;

import java.net.URI;

/**
 * Describes artifact's remote asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArtifactRemoteAssetImpl extends ArtifactAssetImpl implements RemoteAsset {
    private URI link;

    /**
     * The copy constructor.
     *
     * @param asset the {@link RemoteAssetImpl} to copy relevant data from.
     */
    public ArtifactRemoteAssetImpl(RemoteAssetImpl asset) {
        this.setLink(asset.getLink());
        this.logicalPath = asset.getLogicalPath();
        this.type = asset.getType();
        this.overwrite = asset.getOverwrite();
        this.metadata = asset.getMetadata();
    }
}
