package ml.comet.experiment.impl.utils;

import lombok.experimental.UtilityClass;
import ml.comet.experiment.impl.asset.Asset;
import ml.comet.experiment.impl.asset.AssetType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

import static ml.comet.experiment.impl.asset.AssetType.ASSET_TYPE_ASSET;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Utilities to work with assets.
 */
@UtilityClass
public class AssetUtils {

    /**
     * Walks through th asset files in the given folder and produce stream of {@link Asset} objects holding information
     * about file assets found in the folder.
     *
     * @param folder      the folder where to look for asset files
     * @param logFilePath if {@code true} the absolute of relative file path to be used.
     * @param recursive   if {@code true} then subfolder files will be included recursively.
     * @param metadata    the metadata to be associated with the asset.
     * @param step        the step to be associated with each asset.
     * @param epoch       the training epoch to bbe associated with each asset.
     * @return the stream of {@link Asset} objects.
     * @throws IOException if an I/O exception occurred.
     */
    public static Stream<Asset> walkFolderAssets(File folder, boolean logFilePath, boolean recursive,
                                                 Map<String, Object> metadata, long step, long epoch)
            throws IOException {
        // list files in the directory and process each file as an asset
        return FileUtils.listFiles(folder, recursive)
                .map(path -> mapToFileAsset(
                        folder, path, logFilePath, true, ASSET_TYPE_ASSET, EMPTY, metadata, step, epoch));
    }

    /**
     * Allows mapping file denoted by {@code assetPath} into {@link Asset} object.
     *
     * @param folder       the root folder for the asset.
     * @param assetPath    the path to the asset file.
     * @param logFilePath  if {@code true} the absolute of relative file path to be used.
     * @param absolutePath if {@code true} the absolute file path going to be used.
     * @param type         the type of the asset.
     * @param assetId      the ID of the asset. If not provided the new GUID will be generted.
     * @param metadata     the metadata to be associated with the asset.
     * @param step         the step to be associated with the asset.
     * @param epoch        the training epoch to bbe associated with the asset.
     * @return the fully initialized {@link Asset} object encapsulating information about asset file.
     */
    public static Asset mapToFileAsset(File folder, Path assetPath, boolean logFilePath, boolean absolutePath,
                                       AssetType type, String assetId, Map<String, Object> metadata,
                                       long step, long epoch) {
        Asset asset = new Asset();
        if (StringUtils.isEmpty(assetId)) {
            assetId = CometUtils.generateGUID();
        }
        asset.setAssetId(assetId);
        asset.setFile(assetPath.toFile());
        String fileName = FileUtils.resolveAssetFileName(folder, assetPath, logFilePath, absolutePath);
        asset.setFileName(fileName);
        asset.setType(type);
        asset.setStep(step);
        asset.setEpoch(epoch);
        asset.setMetadata(metadata);
        asset.setExtension(FilenameUtils.getExtension(fileName));
        return asset;
    }

}
