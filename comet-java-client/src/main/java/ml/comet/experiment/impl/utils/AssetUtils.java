package ml.comet.experiment.impl.utils;

import lombok.experimental.UtilityClass;
import ml.comet.experiment.impl.asset.Asset;
import ml.comet.experiment.impl.asset.RemoteAsset;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utilities to work with assets.
 */
@UtilityClass
public class AssetUtils {

    public static final String REMOTE_FILE_NAME_DEFAULT = "remote";

    /**
     * Walks through the asset files in the given folder and produce stream of {@link Asset} objects holding information
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

    /**
     * Creates remote asset representation encapsulated into {@link RemoteAsset} instance.
     *
     * @param uri       the {@link URI} pointing to the remote asset location. There is no imposed format,
     *                  and it could be a private link.
     * @param fileName  the optional "name" of the remote asset, could be a dataset name, a model file name.
     * @param overwrite if {@code true} will overwrite all existing assets with the same name.
     * @param metadata  Some additional data to attach to the remote asset.
     *                  The dictionary values must be JSON compatible.
     * @return the initialized {@link RemoteAsset} instance.
     */
    public static RemoteAsset createRemoteAsset(URI uri, String fileName, boolean overwrite,
                                                Map<String, Object> metadata) {
        RemoteAsset asset = new RemoteAsset();
        asset.setLink(uri);
        asset.setOverwrite(overwrite);
        asset.setMetadata(metadata);
        if (StringUtils.isBlank(fileName)) {
            asset.setFileName(remoteAssetFileName(uri));
        } else {
            asset.setFileName(fileName);
        }
        return asset;
    }

    static String remoteAssetFileName(URI uri) {
        String fileName = uri.getPath();
        if (StringUtils.isBlank(fileName)) {
            return REMOTE_FILE_NAME_DEFAULT;
        }
        // get last portion of the name
        int lastSlashIndex = fileName.lastIndexOf("/");
        if (lastSlashIndex >= 0) {
            return fileName.substring(lastSlashIndex + 1);
        } else {
            return fileName;
        }
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
