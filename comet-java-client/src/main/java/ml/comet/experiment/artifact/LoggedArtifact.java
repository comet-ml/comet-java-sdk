package ml.comet.experiment.artifact;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Defines the public contract of the Comet artifact that already logged to the Comet servers.
 *
 * <p>{@link LoggedArtifact} allows reading artifact properties and downloading all or particular assets
 * associated with it.
 */
public interface LoggedArtifact {
    /**
     * Returns set of aliases associated with the artifact.
     *
     * @return the set of aliases associated with artifact.
     */
    Set<String> getAliases();

    /**
     * Returns unique identifier of the artifact.
     *
     * @return the unique identifier of the artifact.
     */
    String getArtifactId();

    /**
     * Returns set of TAGs associated with the artifact.
     *
     * @return the set of TAGs associated with the artifact.
     */
    Set<String> getArtifactTags();

    /**
     * Returns type of the artifact.
     *
     * @return the type of the artifact.
     */
    String getArtifactType();

    /**
     * Returns metadata associated with artifact.
     *
     * @return the optional metadata associated with artifact.
     */
    Map<String, Object> getMetadata();

    /**
     * Returns name of the artifact.
     *
     * @return the name of the artifact.
     */
    String getName();

    /**
     * Returns the fully qualified name of the artifact in form 'workspace/name:version'.
     *
     * @return the fully qualified name of the artifact.
     */
    String getFullName();

    /**
     * Returns the total size of logged artifact version; it is the sum of all the artifact version assets.
     *
     * @return the total size of logged artifact version; it is the sum of all the artifact version assets.
     */
    long getSize();

    /**
     * Returns key of the experiment that was used to create this artifact.
     *
     * @return the key of the experiment that was used to create this artifact.
     */
    String getSourceExperimentKey();

    /**
     * Returns the version of this artifact represented in semantic version format.
     *
     * @return the version of this artifact represented in semantic version format.
     */
    String getVersion();

    /**
     * Returns the unique identifier of the artifact version.
     *
     * @return the unique identifier of the artifact version.
     */
    String getVersionId();

    /**
     * Returns set of TAGs associated with current version of the artifact.
     *
     * @return the set of TAGs associated with current version of the artifact.
     */
    Set<String> getVersionTags();

    /**
     * Returns workspace name where artifact was logged.
     *
     * @return the workspace name where artifact was logged.
     */
    String getWorkspace();

    /**
     * Allows reading list of remote assets associated with this artifact from Comet backend.
     *
     * <p>This method is the remote method invocation and will contact the Comet backend.
     *
     * @return the list of {@link LoggedArtifactAsset} associated with this artifact.
     * @throws ArtifactException if failed to read assets from Comet.
     */
    Collection<LoggedArtifactAsset> getRemoteAssets() throws ArtifactException;

    /**
     * Allows reading list of assets associated with this artifact from the Comet backend.
     *
     * <p>This method is the remote method invocation and will contact the Comet backend.
     *
     * @return the list of {@link LoggedArtifactAsset} associated with this artifact.
     * @throws ArtifactException if failed to read assets from the Comet.
     */
    Collection<LoggedArtifactAsset> getAssets() throws ArtifactException;

    /**
     * Allows getting asset keyed by provided {@code logicalPath} from the Comet backend.
     *
     * @param assetLogicalPath the logical path of the asset.
     * @return the {@link LoggedArtifactAsset} instance associated with given logical path.
     * @throws ArtifactAssetNotFoundException if failed to find asset with given logical path.
     * @throws ArtifactException              if operation failed.
     */
    LoggedArtifactAsset getAsset(String assetLogicalPath) throws ArtifactAssetNotFoundException, ArtifactException;

    /**
     * Download the current Artifact Version assets to a given directory.
     * This downloads only non-remote assets.
     * You can access remote assets links using returned list of assets.
     *
     * @param folder            the path to the folder to keep downloaded files of the assets.
     * @param overwriteStrategy the overwriting strategy to apply when conflicting file name found.
     * @return the {@link Artifact} representing downloaded artifact with a list of all associated assets.
     * @throws ArtifactException thrown if operation failed.
     */
    DownloadedArtifact download(Path folder, AssetOverwriteStrategy overwriteStrategy) throws ArtifactException;

    DownloadedArtifact download(Path folder) throws ArtifactException;
}
