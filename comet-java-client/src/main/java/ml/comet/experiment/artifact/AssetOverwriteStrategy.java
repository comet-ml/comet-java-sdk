package ml.comet.experiment.artifact;

/**
 * Defines overwrite strategy to use for handling conflicts when trying to download an artifact version
 * asset to a path with an existing file with the same name.
 */
public enum AssetOverwriteStrategy {
    /**
     * If an asset file already exists raises exception.
     */
    FAIL,
    /**
     * If an asset file already exists, preserve the existing content (by renaming the original file).
     */
    PRESERVE,
    /**
     * If an asset file already exists, replace it by the new version of asset.
     */
    OVERWRITE
}
