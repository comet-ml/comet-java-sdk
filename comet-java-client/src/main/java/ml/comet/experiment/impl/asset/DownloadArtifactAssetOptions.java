package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Holds options used to download particular artifact asset.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DownloadArtifactAssetOptions extends DownloadAssetOptions {
    String artifactVersionId;
}
