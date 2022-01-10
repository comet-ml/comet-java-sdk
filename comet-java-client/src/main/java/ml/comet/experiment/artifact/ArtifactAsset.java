package ml.comet.experiment.artifact;

import ml.comet.experiment.asset.Asset;

/**
 * Defines the public contract of the asset associated with specific artifact.
 */
public interface ArtifactAsset extends Asset {
    /**
     * Returns version ID of the associated {@link ml.comet.experiment.artifact.Artifact}.
     *
     * @return the version ID of the associated {@link ml.comet.experiment.artifact.Artifact}.
     */
    String getArtifactVersionId();

    /**
     * Sets the version ID of the associated {@link ml.comet.experiment.artifact.Artifact}.
     *
     * @param artifactVersionId the version ID of the associated {@link ml.comet.experiment.artifact.Artifact}.
     */
    void setArtifactVersionId(String artifactVersionId);
}
