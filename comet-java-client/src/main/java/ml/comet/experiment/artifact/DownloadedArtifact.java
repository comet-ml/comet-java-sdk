package ml.comet.experiment.artifact;

import java.util.Map;
import java.util.Set;

/**
 * Defines the public contract of the Comet Artifact which was downloaded.
 *
 * <p>You can use this artifact object to update some properties of the  artifact as well
 * as to add new assets to it. After that, the artifact can be uploaded to the Comet
 * using {@link ml.comet.experiment.OnlineExperiment#logArtifact(Artifact)} method.
 */
public interface DownloadedArtifact extends Artifact {
    /**
     * Returns unique identifier of the artifact.
     *
     * @return the unique identifier of the artifact.
     */
    String getArtifactId();

    /**
     * Returns name of the artifact.
     *
     * @return the name of the artifact.
     */
    String getName();

    /**
     * Returns type of the artifact.
     *
     * @return the type of the artifact.
     */
    String getArtifactType();

    /**
     * Returns workspace name where artifact was logged.
     *
     * @return the workspace name where artifact was logged.
     */
    String getWorkspace();

    /**
     * Returns the version of this artifact represented in semantic version format.
     *
     * @return the version of this artifact represented in semantic version format.
     */
    String getVersion();

    /**
     * Sets new version for this artifact. If provided version is less than current version it would not be applied
     * and {@code false} will be returned. The version string should follow the semantic versioning rules
     * and be in the form: {@code 1.2.3-beta.4+sha899d8g79f87}.
     *
     * <p>See {@link ArtifactBuilder#withVersion(String)} for details about version format.
     *
     * @param version the new version of the artifact.
     * @return {@code true} if new version was successfully set.
     */
    boolean setVersion(String version);

    /**
     * Allows bumping artifact version to the next major version.
     *
     * <p>See {@link ArtifactBuilder#withVersion(String)} for details about version format.
     *
     * @return the new artifact version.
     */
    String incrementMajorVersion();

    /**
     * Allows bumping artifact version to the next minor version.
     *
     * <p>See {@link ArtifactBuilder#withVersion(String)} for details about version format.
     *
     * @return the new artifact version.
     */
    String incrementMinorVersion();

    /**
     * Allows bumping artifact version to the next patch version.
     *
     * <p>See {@link ArtifactBuilder#withVersion(String)} for details about version format.
     *
     * @return the new artifact version.
     */
    String incrementPatchVersion();

    /**
     * Returns set of TAGs associated with current version of the artifact.
     *
     * @return the set of TAGs associated with current version of the artifact.
     */
    Set<String> getVersionTags();

    /**
     * Sets the TAGs for the new version of the artifact. The current version TAGs will be replaced by
     * provided ones.
     *
     * @param tags the set of version TAGs to replace existing.
     */
    void setVersionTags(Set<String> tags);

    /**
     * Returns set of aliases associated with the artifact.
     *
     * @return the set of aliases associated with artifact.
     */
    Set<String> getAliases();

    /**
     * Sets the new aliases to be associated with the artifact. The current aliases
     * will be replaced by provided ones.
     *
     * @param aliases the set of aliases to be associated with artifact.
     */
    void setAliases(Set<String> aliases);

    /**
     * Returns metadata associated with artifact.
     *
     * @return the optional metadata associated with artifact.
     */
    Map<String, Object> getMetadata();
}
