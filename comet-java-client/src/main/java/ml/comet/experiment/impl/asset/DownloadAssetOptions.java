package ml.comet.experiment.impl.asset;

import lombok.Data;

import java.io.File;
import java.nio.file.Path;

/**
 * Holds options used to download particular artifact asset.
 */
@Data
public class DownloadAssetOptions {
    String assetId;
    String experimentKey;
    File file;
}
