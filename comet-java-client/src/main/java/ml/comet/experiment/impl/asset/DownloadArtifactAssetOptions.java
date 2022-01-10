package ml.comet.experiment.impl.asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.io.File;

/**
 * Holds options used to download particular artifact asset.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class DownloadArtifactAssetOptions extends DownloadAssetOptions {
    String artifactVersionId;

    public DownloadArtifactAssetOptions(String assetId,  String artifactVersionId, File file) {
        super(assetId, file);
        this.artifactVersionId = artifactVersionId;
    }
}
