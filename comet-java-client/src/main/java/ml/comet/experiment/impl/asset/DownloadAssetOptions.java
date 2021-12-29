package ml.comet.experiment.impl.asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;

/**
 * Holds options used to download particular artifact asset.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadAssetOptions {
    String assetId;
    File file;
}
