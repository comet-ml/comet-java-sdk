package ml.comet.experiment.artifact;

import ml.comet.experiment.model.FileAsset;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Defines the public contract of the asset logged by the Comet artifact.
 */
public interface LoggedArtifactAsset {
    /**
     * Returns unique identifier of the artifact.
     *
     * @return the unique identifier of the artifact.
     */
    String getArtifactId();

    /**
     * Returns the unique identifier of the artifact version.
     *
     * @return the unique identifier of the artifact version.
     */
    String getArtifactVersionId();

    /**
     * Returns unique identifier of this asset.
     *
     * @return the unique identifier of this asset.
     */
    String getAssetId();

    /**
     * Returns type of this asset.
     *
     * @return the type of this asset.
     */
    String getAssetType();

    /**
     * Returns the asset file name.
     *
     * @return the asset file name.
     */
    String getFileName();

    /**
     * Returns the optional size of the asset file.
     *
     * @return the optional size of the asset file.
     */
    Optional<Long> getFileSize();

    /**
     * Returns the optional {@link URI} of the asset if this is remote asset.
     *
     * @return the optional {@link URI} of the asset if this is remote asset.
     */
    Optional<URI> getLink();

    /**
     * Returns metadata assigned to this asset.
     *
     * @return the metadata assigned to this asset or empty {@link Map} if no metadata was assigned.
     */
    Map<String, Object> getMetadata();

    /**
     * Allows checking if this is remote asset.
     *
     * @return {@code true} if this is remote asset.
     */
    boolean isRemote();

    /**
     * Downloads asset to the given directory.
     *
     * @param dir               the root folder to which to download.
     * @param file              the path relative to the root. If not provided the asset file name will be used as path
     *                          relative to the root directory.
     * @param overwriteStrategy overwrite strategy to handle conflicting file names.
     * @return the {@link FileAsset} representing downloaded asset file.
     * @throws ArtifactException if operation failed.
     */
    FileAsset download(Path dir, Path file, AssetOverwriteStrategy overwriteStrategy) throws ArtifactException;

    FileAsset download(Path dir, AssetOverwriteStrategy overwriteStrategy) throws ArtifactException;

    FileAsset download(Path dir) throws ArtifactException;
}
