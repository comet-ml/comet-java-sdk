package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Describes artifact's remote asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArtifactRemoteAsset extends RemoteAsset {
    String artifactVersionId;
}
