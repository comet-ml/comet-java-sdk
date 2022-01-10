package ml.comet.experiment.asset;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

import java.nio.file.Path;
import java.util.Map;

/**
 * Represents the asset downloaded from the Comet server to the local file system.
 */
@Data
@AllArgsConstructor
@ToString
public class FileAsset {
    Path path;
    long size;
    Map<String, Object> metadata;
    AssetType assetType;
}
