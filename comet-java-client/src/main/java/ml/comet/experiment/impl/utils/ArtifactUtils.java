package ml.comet.experiment.impl.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.artifact.GetArtifactOptions;
import ml.comet.experiment.impl.asset.DownloadArtifactAssetOptions;
import ml.comet.experiment.impl.constants.QueryParamName;

import java.util.HashMap;
import java.util.Map;

import static ml.comet.experiment.impl.constants.QueryParamName.ALIAS;
import static ml.comet.experiment.impl.constants.QueryParamName.ARTIFACT_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.ARTIFACT_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.ARTIFACT_VERSION_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.ASSET_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.CONSUMER_EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.PROJECT;
import static ml.comet.experiment.impl.constants.QueryParamName.VERSION;
import static ml.comet.experiment.impl.constants.QueryParamName.VERSION_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.VERSION_OR_ALIAS;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE;

/**
 * Utilities to work with artifacts.
 */
@UtilityClass
public class ArtifactUtils {

    /**
     * Extracts query parameters from provided {@link GetArtifactOptions} object to be used for getting details about
     * particular artifact version.
     *
     * @param options       the {@link GetArtifactOptions}
     * @param experimentKey the current experiment's key
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> versionDetailsParams(
            @NonNull final GetArtifactOptions options, @NonNull String experimentKey) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(EXPERIMENT_KEY, experimentKey);
        queryParams.put(WORKSPACE, options.getWorkspace());
        queryParams.put(PROJECT, options.getProject());
        queryParams.put(ARTIFACT_NAME, options.getArtifactName());
        queryParams.put(ARTIFACT_ID, options.getArtifactId());
        queryParams.put(VERSION_ID, options.getVersionId());
        queryParams.put(VERSION, options.getVersion());
        queryParams.put(ALIAS, options.getAlias());
        queryParams.put(VERSION_OR_ALIAS, options.getVersionOrAlias());
        queryParams.put(CONSUMER_EXPERIMENT_KEY, options.getConsumerExperimentKey());
        return queryParams;
    }

    /**
     * Extracts query parameters from provided {@link GetArtifactOptions} object to be used for getting list of assets
     * associated with particular artifact.
     *
     * @param options the {@link GetArtifactOptions}
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> versionFilesParams(@NonNull final GetArtifactOptions options) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(WORKSPACE, options.getWorkspace());
        queryParams.put(ARTIFACT_NAME, options.getArtifactName());
        queryParams.put(ARTIFACT_ID, options.getArtifactId());
        queryParams.put(VERSION_ID, options.getVersionId());
        queryParams.put(VERSION, options.getVersion());
        queryParams.put(ALIAS, options.getAlias());
        return queryParams;
    }

    /**
     * Extracts query parameters from provided {@link DownloadArtifactAssetOptions} to be used to download specific
     * asset associated with Comet artifact.
     *
     * @param options       the {@link DownloadArtifactAssetOptions}
     * @param experimentKey the current experiment's key
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> downloadArtifactAssetParams(
            @NonNull final DownloadArtifactAssetOptions options, @NonNull String experimentKey) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(EXPERIMENT_KEY, experimentKey);
        queryParams.put(ASSET_ID, options.getAssetId());
        queryParams.put(ARTIFACT_VERSION_ID, options.getArtifactVersionId());
        return queryParams;
    }
}
