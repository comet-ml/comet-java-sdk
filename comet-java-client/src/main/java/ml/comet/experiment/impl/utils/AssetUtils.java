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
     * @param folder               the folder where to look for asset files
     * @param logFilePath          if {@code true} the file path relative to the folder will be used.
     *                             Otherwise, basename of the asset file will be used.
     * @param recursive            if {@code true} then subfolder files will be included recursively.
     * @param prefixWithFolderName if {@code true} then path of each asset file will be prefixed with folder name
     *                             in case if {@code logFilePath} is {@code true}.
     * @return the stream of {@link Asset} objects.
     * @throws IOException if an I/O exception occurred.
     */
    public static Stream<Asset> walkFolderAssets(File folder, boolean logFilePath,
                                                 boolean recursive, boolean prefixWithFolderName)
            throws IOException {
        // list files in the directory and process each file as an asset
        return FileUtils.listFiles(folder, recursive)
                .map(path -> mapToFileAsset(folder, path, logFilePath, prefixWithFolderName));
    }

    static Asset mapToFileAsset(File folder, Path assetPath,
                                boolean logFilePath, boolean prefixWithFolderName) {
        Asset asset = new Asset();
        asset.setFile(assetPath.toFile());
        String fileName = FileUtils.resolveAssetFileName(folder, assetPath, logFilePath, prefixWithFolderName);
        asset.setFileName(fileName);
        asset.setFileExtension(FilenameUtils.getExtension(fileName));
        return asset;
    }

}
