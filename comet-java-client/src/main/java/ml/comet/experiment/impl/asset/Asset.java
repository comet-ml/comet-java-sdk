package ml.comet.experiment.impl.asset;

import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.model.AssetType;

import java.io.File;
import java.util.Map;

/**
 * Defines public contract of the assets associated with particular Comet experiment.
 */
public interface Asset {
    /**
     * Returns the asset's file or {@code null} if no file associated.
     *
     * @return the asset's file or {@code null} if no file associated.
     */
    File getFile();

    /**
     * Returns the asset's data array or {@code null} if no data associated.
     *
     * @return the asset's data array or {@code null} if no data associated.
     */
    byte[] getFileLikeData();

    /**
     * Returns the custom file name for this asset or {@code null} if default should be used.
     *
     * @return the custom file name for this asset or {@code null} if default should be used.
     */
    String getFileName();

    /**
     * Returns the file name extension or {@code null} if not applicable.
     *
     * @return the file name extension or {@code null} if not applicable.
     */
    String getFileExtension();

    /**
     * Sets the {@link ExperimentContext} to be associated with this asset.
     *
     * @param context the {@link ExperimentContext} to be associated with this asset.
     */
    void setExperimentContext(ExperimentContext context);

    /**
     * Returns the {@link ExperimentContext} associated with this asset.
     *
     * @return the {@link ExperimentContext} associated with this asset or {@code null} if no context assigned.
     */
    ExperimentContext getExperimentContext();

    /**
     * Returns the type of this asset.
     *
     * @return the type of this asset.
     */
    AssetType getType();

    /**
     * Returns flag indicating if existing asset with the same name should be overwritten.
     *
     * @return the flag indicating if existing asset with the same name should be overwritten.
     */
    Boolean getOverwrite();

    /**
     * Returns metadata associated with this asset or {@code null} if there is no metadata.
     *
     * @return the metadata associated with this asset or {@code null} if there is no metadata.
     */
    Map<String, Object> getMetadata();

    /**
     * Associates provided metadata with this asset.
     *
     * @param metadata the metadata to be associated.
     */
    void setMetadata(Map<String, Object> metadata);
}
