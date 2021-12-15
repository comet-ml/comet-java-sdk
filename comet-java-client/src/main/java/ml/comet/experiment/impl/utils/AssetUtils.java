package ml.comet.experiment.impl.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.impl.asset.Asset;
import ml.comet.experiment.impl.asset.RemoteAsset;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.model.AssetType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static ml.comet.experiment.impl.constants.QueryParamName.CONTEXT;
import static ml.comet.experiment.impl.constants.QueryParamName.EPOCH;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.EXTENSION;
import static ml.comet.experiment.impl.constants.QueryParamName.FILE_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.OVERWRITE;
import static ml.comet.experiment.impl.constants.QueryParamName.STEP;
import static ml.comet.experiment.impl.constants.QueryParamName.TYPE;
import static ml.comet.experiment.impl.utils.CometUtils.putNotNull;

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
    public static Stream<Asset> walkFolderAssets(@NonNull File folder, boolean logFilePath,
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
     * @param type      the type of the asset. If not specified the default type {@code AssetType.ASSET_TYPE_ASSET}
     *                  will be assigned.
     * @return the initialized {@link RemoteAsset} instance.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static RemoteAsset createRemoteAsset(@NonNull URI uri, Optional<String> fileName, boolean overwrite,
                                                Optional<Map<String, Object>> metadata, Optional<AssetType> type) {
        RemoteAsset asset = new RemoteAsset();
        asset.setLink(uri);
        asset.setFileName(fileName.orElse(remoteAssetFileName(uri)));

        return (RemoteAsset) updateAsset(asset, overwrite, metadata, type);
    }

    /**
     * Creates the {@link Asset} from the local file.
     *
     * @param file      the asset file.
     * @param fileName  the logical name for the asset file.
     * @param overwrite if {@code true} mark as override
     * @param metadata  the metadata to associate with asset. The dictionary values must be JSON compatible.
     * @param type      the type of the asset. If not specified the default type {@code AssetType.ASSET}
     *                  will be assigned.
     * @return the instance of the {@link Asset} from the local file.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Asset createAssetFromFile(@NonNull File file, Optional<String> fileName, boolean overwrite,
                                            @NonNull Optional<Map<String, Object>> metadata,
                                            @NonNull Optional<AssetType> type) {
        String logicalFileName = fileName.orElse(file.getName());
        Asset asset = new Asset();
        asset.setFile(file);
        asset.setFileName(logicalFileName);
        asset.setFileExtension(FilenameUtils.getExtension(logicalFileName));

        return updateAsset(asset, overwrite, metadata, type);
    }

    /**
     * Creates the {@link Asset} from the file-like data.
     *
     * @param data      the asset's data.
     * @param fileName  the logical name for the asset file.
     * @param overwrite if {@code true} mark as override
     * @param metadata  the metadata to associate with asset. The dictionary values must be JSON compatible.
     * @param type      the type of the asset. If not specified the default type {@code AssetType.ASSET_TYPE_ASSET}
     *                  will be assigned.
     * @return the instance of the {@link Asset} from the file-like data.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Asset createAssetFromData(byte[] data, @NonNull String fileName, boolean overwrite,
                                            @NonNull Optional<Map<String, Object>> metadata,
                                            @NonNull Optional<AssetType> type) {
        Asset asset = new Asset();
        asset.setFileLikeData(data);
        asset.setFileName(fileName);
        asset.setFileExtension(FilenameUtils.getExtension(fileName));

        return updateAsset(asset, overwrite, metadata, type);
    }

    /**
     * Extracts query parameters from the provided {@link Asset}.
     *
     * @param asset         the {@link Asset} to extract HTTP query parameters from.
     * @param experimentKey the key of the Comet experiment.
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> assetQueryParameters(
            @NonNull final Asset asset, @NonNull String experimentKey) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(EXPERIMENT_KEY, experimentKey);
        queryParams.put(TYPE, asset.getType().type());

        putNotNull(queryParams, OVERWRITE, asset.getOverwrite());
        putNotNull(queryParams, FILE_NAME, asset.getFileName());
        putNotNull(queryParams, EXTENSION, asset.getFileExtension());
        putNotNull(queryParams, CONTEXT, asset.getContext());
        putNotNull(queryParams, STEP, asset.getStep());
        putNotNull(queryParams, EPOCH, asset.getEpoch());

        return queryParams;
    }

    /**
     * Extracts form parameters from the provided {@link Asset}.
     *
     * @param asset the {@link Asset} to extract HTTP form parameters from.
     * @return the map with form parameters.
     */
    public static Map<FormParamName, Object> assetFormParameters(@NonNull final Asset asset) {
        Map<FormParamName, Object> map = new HashMap<>();
        if (asset.getMetadata() != null) {
            // encode metadata to JSON and store
            map.put(FormParamName.METADATA, JsonUtils.toJson(asset.getMetadata()));
        }
        return map;
    }

    /**
     * Updates provided {@link Asset} with values from optionals or with defaults.
     *
     * @param asset     the {@link Asset} to be updated.
     * @param overwrite if {@code true} will overwrite all existing assets with the same name.
     * @param metadata  Some additional data to attach to the remote asset.
     *                  The dictionary values must be JSON compatible.
     * @param type      the type of the asset. If not specified the default type {@code AssetType.ASSET_TYPE_ASSET}
     *                  will be assigned.
     * @return the updated {@link Asset} with values from optionals or with defaults.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Asset updateAsset(Asset asset, boolean overwrite,
                                    Optional<Map<String, Object>> metadata, Optional<AssetType> type) {
        asset.setOverwrite(overwrite);
        metadata.ifPresent(asset::setMetadata);
        asset.setType(type.orElse(AssetType.ASSET));

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

    static Asset mapToFileAsset(@NonNull File folder, @NonNull Path assetPath,
                                boolean logFilePath, boolean prefixWithFolderName) {
        Asset asset = new Asset();
        asset.setFile(assetPath.toFile());
        String fileName = FileUtils.resolveAssetFileName(folder, assetPath, logFilePath, prefixWithFolderName);
        asset.setFileName(fileName);
        asset.setFileExtension(FilenameUtils.getExtension(fileName));
        asset.setType(AssetType.ASSET);
        return asset;
    }

}
