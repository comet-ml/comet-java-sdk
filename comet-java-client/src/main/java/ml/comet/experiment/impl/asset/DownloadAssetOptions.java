package ml.comet.experiment.impl.asset;

import lombok.Data;

import java.nio.file.Path;

/**
 * Holds options used to download particular experiment asset.
 */
@Data
public class DownloadAssetOptions {
    String assetId;
    String experimentKey;
    Path dir;
    Path fileName;
}
