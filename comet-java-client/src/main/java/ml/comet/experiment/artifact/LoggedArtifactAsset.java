package ml.comet.experiment.artifact;

import java.net.URI;
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
     * Returns the optional asset file name.
     *
     * @return the optional asset file name.
     */
    Optional<String> getFileName();

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
}
