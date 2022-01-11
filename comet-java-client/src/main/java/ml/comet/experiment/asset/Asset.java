package ml.comet.experiment.asset;

import ml.comet.experiment.context.ExperimentContext;

import java.io.File;
import java.util.Map;
import java.util.Optional;

/**
 * Defines public contract of the assets associated with particular Comet experiment.
 */
public interface Asset {
    /**
     * Returns the optional asset's file.
     *
     * @return the optional asset's file.
     */
    Optional<File> getFile();

    /**
     * Returns the optional asset's data array.
     *
     * @return the optional asset's data array.
     */
    Optional<byte[]> getFileLikeData();

    /**
     * Returns the logical file path for this asset.
     *
     * @return the logical file path for this asset.
     */
    String getLogicalPath();

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
     * Returns metadata associated with this asset or {@link Map} if there is no metadata.
     *
     * @return the metadata associated with this asset or empty {@link Map} if there is no metadata.
     */
    Map<String, Object> getMetadata();
}
