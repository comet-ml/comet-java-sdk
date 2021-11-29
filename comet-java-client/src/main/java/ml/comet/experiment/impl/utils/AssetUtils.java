package ml.comet.experiment.impl.utils;

import lombok.experimental.UtilityClass;
import ml.comet.experiment.impl.asset.Asset;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.stream.Stream;

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
     * @return the stream of {@link Asset} objects.
     * @throws IOException if an I/O exception occurred.
     */
    public static Stream<Asset> walkFolderAssets(File folder, boolean logFilePath, boolean recursive)
            throws IOException {
        // list files in the directory and process each file as an asset
        return FileUtils.listFiles(folder, recursive)
                .map(path -> mapToFileAsset(folder, path, logFilePath, true));
    }

    /**
     * Allows mapping file denoted by {@code assetPath} into {@link Asset} object.
     *
     * @param folder       the root folder for the asset.
     * @param assetPath    the path to the asset file.
     * @param logFilePath  if {@code true} the absolute of relative file path to be used.
     * @param absolutePath if {@code true} the absolute file path going to be used.
     * @return the fully initialized {@link Asset} object encapsulating information about asset file.
     */
    public static Asset mapToFileAsset(File folder, Path assetPath, boolean logFilePath, boolean absolutePath) {
        Asset asset = new Asset();
        asset.setFile(assetPath.toFile());
        String fileName = FileUtils.resolveAssetFileName(folder, assetPath, logFilePath, absolutePath);
        asset.setFileName(fileName);
        asset.setFileExtension(FilenameUtils.getExtension(fileName));
        return asset;
    }

}
