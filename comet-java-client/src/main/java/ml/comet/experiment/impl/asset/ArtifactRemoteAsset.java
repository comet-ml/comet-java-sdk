package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Describes artifact's remote asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArtifactRemoteAsset extends RemoteAsset {
    String artifactVersionId;

    /**
     * The copy constructor.
     *
     * @param asset the {@link RemoteAsset} to copy relevant data from.
     */
    public ArtifactRemoteAsset(RemoteAsset asset) {
        this.setLink(asset.getLink());
        this.fileName = asset.fileName;
        this.type = asset.type;
        this.overwrite = asset.overwrite;
        this.metadata = asset.metadata;
    }
}
