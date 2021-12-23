package ml.comet.experiment.artifact;

/**
 * Defines overwrite strategy to use for handling conflicts when trying to download an artifact version
 * asset to a path with an existing file with the same name.
 */
public enum AssetOverwriteStrategy {
    /**
     * If an asset file already exists and its content is different, raises exception.
     */
    FAIL,
    /**
     * If an asset file already exists and its content is different, show a WARNING but
     * preserve the existing content.
     */
    PRESERVE,
    /**
     * If an asset file already exists and its content is different, replace it
     * by the new version of asset.
     */
    OVERWRITE
}
