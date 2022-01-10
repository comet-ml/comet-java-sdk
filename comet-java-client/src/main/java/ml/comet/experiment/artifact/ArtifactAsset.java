package ml.comet.experiment.artifact;

import ml.comet.experiment.asset.Asset;

import java.util.Optional;

/**
 * Defines the public contract of the asset associated with specific artifact.
 */
public interface ArtifactAsset extends Asset {

    /**
     * Returns the optional size of this asset if appropriate.
     *
     * @return the optional size of this asset if appropriate.
     */
    Optional<Long> getSize();
}
