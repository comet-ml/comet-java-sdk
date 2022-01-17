package ml.comet.experiment.artifact;

import java.io.InputStream;
import java.io.OutputStream;
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
     * Returns the optional size of the asset.
     *
     * @return the optional size of the asset.
     */
    Optional<Long> getSize();

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
     * @return the {@link ArtifactAsset} representing downloaded asset file.
     * @throws ArtifactException if operation failed.
     */
    ArtifactAsset download(Path dir, Path file, AssetOverwriteStrategy overwriteStrategy) throws ArtifactException;

    ArtifactAsset download(Path dir, AssetOverwriteStrategy overwriteStrategy) throws ArtifactException;

    ArtifactAsset download(Path dir) throws ArtifactException;

    /**
     * Allows loading of asset bytes from Comet server and write to the provided {@link OutputStream}.
     *
     * @param out the {@link OutputStream} where downloaded asset bytes will be written.
     * @throws ArtifactException if operation failed.
     */
    void writeTo(OutputStream out) throws ArtifactException;

    /**
     * Opens a connection to this {@code LoggedArtifactAsset} and returns an {@link InputStream} for reading from
     * this connection.
     *
     * <p>NOTE: You are responsible to close an {@link InputStream} to avoid resource leak.
     *
     * @return an input stream for reading asset's data bytes.
     * @throws ArtifactException if operation failed.
     */
    InputStream openStream() throws ArtifactException;
}
