package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Describes artifact's asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ArtifactAsset extends Asset {
    String artifactVersionId;
}
