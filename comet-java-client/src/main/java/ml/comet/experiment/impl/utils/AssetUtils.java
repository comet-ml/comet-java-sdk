package ml.comet.experiment.impl.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.asset.RemoteAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.asset.AssetImpl;
import ml.comet.experiment.impl.asset.AssetType;
import ml.comet.experiment.impl.asset.RemoteAssetImpl;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static ml.comet.experiment.impl.asset.AssetType.POINTS_3D;
import static ml.comet.experiment.impl.asset.AssetType.UNKNOWN;
import static ml.comet.experiment.impl.constants.QueryParamName.CONTEXT;
import static ml.comet.experiment.impl.constants.QueryParamName.EPOCH;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.EXTENSION;
import static ml.comet.experiment.impl.constants.QueryParamName.FILE_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.GROUPING_NAME;
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
     * @param metadata             the optional metadata to associate with assets.
     * @param type                 optional type of the asset (default: ASSET).
     * @param groupingName         optional name of group the assets should belong.
     * @return the stream of {@link AssetImpl} objects.
     * @throws IOException if an I/O exception occurred.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Stream<AssetImpl> walkFolderAssets(
            @NonNull File folder, boolean logFilePath, boolean recursive, boolean prefixWithFolderName,
            @NonNull Optional<Map<String, Object>> metadata, @NonNull Optional<String> type,
            @NonNull Optional<String> groupingName)
            throws IOException {
        // list files in the directory and process each file as an asset
        return FileUtils.listFiles(folder, recursive)
                .map(path -> mapToFileAsset(
                        folder, path, logFilePath, prefixWithFolderName, metadata, type, groupingName));
    }

    /**
     * Creates remote asset representation encapsulated into {@link RemoteAsset} instance.
     *
     * @param uri         the {@link URI} pointing to the remote asset location. There is no imposed format,
     *                    and it could be a private link.
     * @param logicalPath the optional "name" of the remote asset, could be a dataset name, a model file name.
     * @param overwrite   if {@code true} will overwrite all existing assets with the same name.
     * @param metadata    Some additional data to attach to the remote asset.
     *                    The dictionary values must be JSON compatible.
     * @param type        the type of the asset. If not specified the default type {@code AssetType.ASSET}
     *                    will be assigned.
     * @return the initialized {@link RemoteAssetImpl} instance.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static RemoteAssetImpl createRemoteAsset(@NonNull URI uri, Optional<String> logicalPath, boolean overwrite,
                                                    Optional<Map<String, Object>> metadata, Optional<String> type) {
        RemoteAssetImpl asset = new RemoteAssetImpl();
        asset.setUri(uri);
        asset.setLogicalPath(logicalPath.orElse(remoteAssetFileName(uri)));

        return (RemoteAssetImpl) updateAsset(asset, overwrite, metadata, type);
    }

    /**
     * Creates the {@link Asset} from the local file.
     *
     * @param file        the asset file.
     * @param logicalPath the logical name for the asset file.
     * @param overwrite   if {@code true} mark as override
     * @param metadata    the metadata to associate with asset. The dictionary values must be JSON compatible.
     * @param type        the type of the asset. If not specified the default type {@code AssetType.ASSET}
     *                    will be assigned.
     * @return the instance of the {@link AssetImpl} from the local file.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static AssetImpl createAssetFromFile(@NonNull File file, Optional<String> logicalPath, boolean overwrite,
                                                @NonNull Optional<Map<String, Object>> metadata,
                                                @NonNull Optional<String> type) {
        String logicalFileName = logicalPath.orElse(file.getName());
        AssetImpl asset = new AssetImpl();
        asset.setRawFile(file);
        asset.setLogicalPath(logicalFileName);
        asset.setFileExtension(FilenameUtils.getExtension(logicalFileName));

        return updateAsset(asset, overwrite, metadata, type);
    }

    /**
     * Creates the {@link Asset} from the file-like data.
     *
     * @param data        the asset's data.
     * @param logicalPath the logical name for the asset file.
     * @param overwrite   if {@code true} mark as override
     * @param metadata    the metadata to associate with asset. The dictionary values must be JSON compatible.
     * @param type        the type of the asset. If not specified the default type {@code AssetType.ASSET_TYPE_ASSET}
     *                    will be assigned.
     * @return the instance of the {@link AssetImpl} from the file-like data.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static AssetImpl createAssetFromData(byte[] data, @NonNull String logicalPath, boolean overwrite,
                                                @NonNull Optional<Map<String, Object>> metadata,
                                                @NonNull Optional<String> type) {
        AssetImpl asset = new AssetImpl();
        asset.setRawFileLikeData(data);
        asset.setLogicalPath(logicalPath);
        asset.setFileExtension(FilenameUtils.getExtension(logicalPath));

        return updateAsset(asset, overwrite, metadata, type);
    }

    /**
     * Extracts query parameters from the provided {@link Asset}.
     *
     * @param asset         the {@link AssetImpl} to extract HTTP query parameters from.
     * @param experimentKey the key of the Comet experiment.
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> assetQueryParameters(
            @NonNull final AssetImpl asset, @NonNull String experimentKey) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(EXPERIMENT_KEY, experimentKey);
        queryParams.put(TYPE, asset.getType());

        putNotNull(queryParams, OVERWRITE, asset.getOverwrite());
        putNotNull(queryParams, FILE_NAME, asset.getLogicalPath());
        putNotNull(queryParams, EXTENSION, asset.getFileExtension());

        if (asset.getExperimentContext().isPresent()) {
            ExperimentContext context = asset.getExperimentContext().get();
            putNotNull(queryParams, CONTEXT, context.getContext());
            putNotNull(queryParams, STEP, context.getStep());
            putNotNull(queryParams, EPOCH, context.getEpoch());
        }

        if (asset.getGroupingName().isPresent()) {
            queryParams.put(GROUPING_NAME, asset.getGroupingName().get());
        }

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
     * Updates provided {@link AssetImpl} with values from optionals or with defaults.
     *
     * @param asset     the {@link AssetImpl} to be updated.
     * @param overwrite if {@code true} will overwrite all existing assets with the same name.
     * @param metadata  Some additional data to attach to the remote asset.
     *                  The dictionary values must be JSON compatible.
     * @param type      the type of the asset. If not specified the default type {@code AssetType.ASSET_TYPE_ASSET}
     *                  will be assigned.
     * @return the updated {@link AssetImpl} with values from optionals or with defaults.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static AssetImpl updateAsset(AssetImpl asset, boolean overwrite,
                                        Optional<Map<String, Object>> metadata, Optional<String> type) {
        asset.setOverwrite(overwrite);
        metadata.ifPresent(asset::setMetadata);
        asset.setType(type.orElse(AssetType.ASSET.type()));

        return asset;
    }

    /**
     * Allows converting type name to the {@link AssetType}.
     *
     * @param typeName the type name.
     * @return the appropriate {@link AssetType} enum value or {@code UNKNOWN} value if failed.
     */
    public static AssetType toAssetType(@NonNull String typeName) {
        if (typeName.equals(POINTS_3D.type())) {
            // special case
            return POINTS_3D;
        }
        typeName = typeName.toUpperCase(Locale.ROOT);
        typeName = typeName.replace("-", "_");
        try {
            return Enum.valueOf(AssetType.class, typeName);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static AssetImpl mapToFileAsset(@NonNull File folder, @NonNull Path assetPath,
                                    boolean logFilePath, boolean prefixWithFolderName,
                                    @NonNull Optional<Map<String, Object>> metadata, @NonNull Optional<String> type,
                                    @NonNull Optional<String> groupingName) {
        AssetImpl asset = new AssetImpl();
        asset.setRawFile(assetPath.toFile());
        String fileName = FileUtils.resolveAssetFileName(folder, assetPath, logFilePath, prefixWithFolderName);
        asset.setLogicalPath(fileName);
        asset.setFileExtension(FilenameUtils.getExtension(fileName));

        metadata.ifPresent(asset::setMetadata);
        if (type.isPresent()) {
            asset.setType(type.get());
        } else {
            asset.setType(AssetType.ASSET.type());
        }
        groupingName.ifPresent(asset::setGroupingName);

        return asset;
    }

}
