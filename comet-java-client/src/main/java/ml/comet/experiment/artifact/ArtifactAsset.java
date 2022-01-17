package ml.comet.experiment.artifact;

import ml.comet.experiment.asset.RemoteAsset;

import java.util.Optional;

/**
 * Defines the public contract of the asset associated with specific artifact.
 */
public interface ArtifactAsset extends RemoteAsset {

    /**
     * Returns the optional size of this asset if appropriate.
     *
     * @return the optional size of this asset if appropriate.
     */
    Optional<Long> getSize();

    /**
     * Returns {@code true} if this is remote asset, i.e., providing {@code URI} to the remote location to download
     * content.
     *
     * @return {@code true} if this is remote asset.
     */
    boolean isRemote();
}
