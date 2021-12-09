package ml.comet.experiment.artifact;

import lombok.NonNull;
import ml.comet.experiment.impl.ArtifactImpl;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Defines the public contract of the Comet Artifact.
 *
 * <p>Comet Artifacts allow keeping track of assets beyond any particular experiment.
 * You can keep track of Artifact versions, create many types of assets, manage them,
 * and use them in any step in your ML pipelines - from training to production deployment.
 *
 * <p>Artifacts live in a Comet Project, are identified by their name and version string number.
 */
public interface Artifact {

    /**
     * Adds a local asset file to the current pending artifact object.
     *
     * @param file      the path to the file asset.
     * @param name      the custom asset name to be displayed. If not
     *                  provided the filename from the {@code file} argument will be used.
     * @param overwrite if {@code true} will overwrite all existing assets with the same name.
     * @param metadata  Some additional data to attach to the asset. Must be a map with JSON-encodable values.
     * @throws ConflictingArtifactAssetName is name of the asset is not unique within this artifact.
     */
    void addAsset(File file, String name, boolean overwrite, Map<String, Object> metadata)
            throws ConflictingArtifactAssetName;

    void addAsset(File file, boolean overwrite, Map<String, Object> metadata) throws ConflictingArtifactAssetName;

    void addAsset(File file, boolean overwrite) throws ConflictingArtifactAssetName;

    /**
     * Adds a local asset data to the current pending artifact object.
     *
     * @param data      the data of the asset.
     * @param name      the custom asset name to be displayed.
     * @param overwrite if {@code true} will overwrite all existing assets with the same name.
     * @param metadata  some additional data to attach to the asset. Must be a map with JSON-encodable values.
     * @throws ConflictingArtifactAssetName is name of the asset is not unique within this artifact.
     */
    void addAsset(byte[] data, String name, boolean overwrite, Map<String, Object> metadata)
            throws ConflictingArtifactAssetName;

    void addAsset(byte[] data, String name, boolean overwrite) throws ConflictingArtifactAssetName;

    void addAsset(byte[] data, String name) throws ConflictingArtifactAssetName;

    /**
     * Adds remote asses to the current pending artifact object. A Remote Asset is an asset but
     * its content is not uploaded and stored on Comet. Rather a link for its location is stored, so
     * you can identify and distinguish between two experiment using different version of a dataset
     * stored somewhere else.
     *
     * @param uri       the {@link URI} pointing to the location of the remote asset.
     * @param name      the "name" of the remote asset, could be a dataset name, a model file name.
     * @param overwrite if {@code true} will overwrite all existing assets with the same name.
     * @param metadata  some additional data to attach to the asset. Must be a map with JSON-encodable values.
     * @throws ConflictingArtifactAssetName is name of the asset is not unique within this artifact.
     */
    void addRemoteAsset(URI uri, String name, boolean overwrite, Map<String, Object> metadata)
            throws ConflictingArtifactAssetName;

    void addRemoteAsset(URI uri, String name, boolean overwrite) throws ConflictingArtifactAssetName;

    void addRemoteAsset(URI uri, String name) throws ConflictingArtifactAssetName;

    /**
     * Adds all asset files located in the given folder to this artifact.
     *
     * @param folder      the folder you want to log.
     * @param logFilePath if {@code true}, log the file path with each file.
     * @param recursive   if {@code true}, recurse the folder.
     * @param metadata    some additional data to attach to the asset. Must be a map with JSON-encodable values.
     * @throws ConflictingArtifactAssetName is name of some asset is not unique within this artifact.
     * @throws IOException                  if an I/O exception occurred when walking through the folder.
     */
    void addAssetFolder(File folder, boolean logFilePath, boolean recursive, Map<String, Object> metadata)
            throws ConflictingArtifactAssetName, IOException;

    void addAssetFolder(File folder, boolean logFilePath, boolean recursive)
            throws ConflictingArtifactAssetName, IOException;

    void addAssetFolder(File folder, boolean logFilePath) throws ConflictingArtifactAssetName, IOException;

    void addAssetFolder(File folder) throws ConflictingArtifactAssetName, IOException;

    /**
     * Creates new {@link ArtifactBuilder} instance which can be used to create properly initialized instances of the
     * {@link Artifact}.
     *
     * @param name the name of the artifact.
     * @param type the type of the artifact.
     * @return the {@link ArtifactBuilder} instance to create properly initialized instances of the {@link Artifact}.
     */
    static ArtifactBuilder newArtifact(@NonNull String name, @NonNull String type) {
        return ArtifactImpl.builder(name, type);
    }
}
