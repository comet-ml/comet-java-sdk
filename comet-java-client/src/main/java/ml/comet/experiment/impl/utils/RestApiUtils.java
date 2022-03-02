package ml.comet.experiment.impl.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.artifact.GetArtifactOptions;
import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.ArtifactImpl;
import ml.comet.experiment.impl.RegistryModelImpl;
import ml.comet.experiment.impl.asset.AssetImpl;
import ml.comet.experiment.impl.asset.DownloadArtifactAssetOptions;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.rest.AddExperimentTagsRest;
import ml.comet.experiment.impl.rest.AddGraphRest;
import ml.comet.experiment.impl.rest.ArtifactRequest;
import ml.comet.experiment.impl.rest.ArtifactVersionState;
import ml.comet.experiment.impl.rest.ExperimentTimeRequest;
import ml.comet.experiment.impl.rest.GitMetadataRest;
import ml.comet.experiment.impl.rest.HtmlRest;
import ml.comet.experiment.impl.rest.LogOtherRest;
import ml.comet.experiment.impl.rest.MetricRest;
import ml.comet.experiment.impl.rest.OutputLine;
import ml.comet.experiment.impl.rest.OutputUpdate;
import ml.comet.experiment.impl.rest.ParameterRest;
import ml.comet.experiment.impl.rest.RegistryModelCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelItemCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelNotesUpdateRequest;
import ml.comet.experiment.impl.rest.RegistryModelUpdateRequest;
import ml.comet.experiment.model.GitMetaData;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ml.comet.experiment.impl.constants.QueryParamName.ALIAS;
import static ml.comet.experiment.impl.constants.QueryParamName.ARTIFACT_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.ARTIFACT_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.ARTIFACT_VERSION_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.ASSET_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.CONSUMER_EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.CONTEXT;
import static ml.comet.experiment.impl.constants.QueryParamName.EPOCH;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.EXTENSION;
import static ml.comet.experiment.impl.constants.QueryParamName.FILE_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.GROUPING_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.MODEL_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.OVERWRITE;
import static ml.comet.experiment.impl.constants.QueryParamName.PROJECT;
import static ml.comet.experiment.impl.constants.QueryParamName.STAGE;
import static ml.comet.experiment.impl.constants.QueryParamName.STEP;
import static ml.comet.experiment.impl.constants.QueryParamName.TYPE;
import static ml.comet.experiment.impl.constants.QueryParamName.VERSION;
import static ml.comet.experiment.impl.constants.QueryParamName.VERSION_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.VERSION_OR_ALIAS;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE_NAME;
import static ml.comet.experiment.impl.utils.CometUtils.putNotNull;

/**
 * The common factory methods to create initialized DTO instances used in REST API.
 */
@UtilityClass
public class RestApiUtils {
    /**
     * The factory to create {@link MetricRest} instance.
     *
     * @param metricName  the metric name
     * @param metricValue the metric value
     * @param context     the current context
     * @return the initialized {@link MetricRest} instance.
     */
    public static MetricRest createLogMetricRequest(
            @NonNull String metricName, @NonNull Object metricValue, @NonNull ExperimentContext context) {
        MetricRest request = new MetricRest();
        request.setMetricName(metricName);
        request.setMetricValue(metricValue.toString());
        request.setStep(context.getStep());
        request.setEpoch(context.getEpoch());
        request.setTimestamp(System.currentTimeMillis());
        request.setContext(context.getContext());
        return request;
    }

    /**
     * The factory to create {@link ParameterRest} instance.
     *
     * @param parameterName the name of the parameter
     * @param paramValue    the value of the parameter
     * @param context       the current context
     * @return the initialized {@link ParameterRest} instance.
     */
    public static ParameterRest createLogParamRequest(
            @NonNull String parameterName, @NonNull Object paramValue, @NonNull ExperimentContext context) {
        ParameterRest request = new ParameterRest();
        request.setParameterName(parameterName);
        request.setParameterValue(paramValue.toString());
        request.setStep(context.getStep());
        request.setTimestamp(System.currentTimeMillis());
        request.setContext(context.getContext());
        return request;
    }

    /**
     * The factory to create {@link OutputUpdate} instance.
     *
     * @param line    the log line
     * @param offset  the log line offset
     * @param stderr  the flag to indicate if it's from StdErr
     * @param context the current context
     * @return the initialized {@link OutputUpdate} instance.
     */
    public static OutputUpdate createLogLineRequest(@NonNull String line, long offset, boolean stderr, String context) {
        OutputLine outputLine = new OutputLine();
        outputLine.setOutput(line);
        outputLine.setStderr(stderr);
        outputLine.setLocalTimestamp(System.currentTimeMillis());
        outputLine.setOffset(offset);

        OutputUpdate outputUpdate = new OutputUpdate();
        outputUpdate.setRunContext(context);
        outputUpdate.setOutputLines(Collections.singletonList(outputLine));
        return outputUpdate;
    }

    /**
     * The factory to create {@link HtmlRest} instance.
     *
     * @param html     the HTML code to be logged.
     * @param override the flag to indicate whether it should override already saved version.
     * @return the initialized {@link HtmlRest} instance.
     */
    public static HtmlRest createLogHtmlRequest(@NonNull String html, boolean override) {
        HtmlRest request = new HtmlRest();
        request.setHtml(html);
        request.setOverride(override);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    /**
     * The factory to create {@link LogOtherRest} instance.
     *
     * @param key   the parameter name/key.
     * @param value the parameter value.
     * @return the initialized {@link LogOtherRest} instance.
     */
    public static LogOtherRest createLogOtherRequest(@NonNull String key, @NonNull Object value) {
        LogOtherRest request = new LogOtherRest();
        request.setKey(key);
        request.setValue(value.toString());
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    /**
     * The factory to create {@link AddExperimentTagsRest} instance.
     *
     * @param tag the tag value
     * @return the initialized {@link AddExperimentTagsRest} instance
     */
    public static AddExperimentTagsRest createTagRequest(@NonNull String tag) {
        AddExperimentTagsRest request = new AddExperimentTagsRest();
        request.setAddedTags(Collections.singletonList(tag));
        return request;
    }

    /**
     * The factory to create {@link AddGraphRest} instance.
     *
     * @param graph the NN graph representation.
     * @return the initialized {@link AddGraphRest} instance.
     */
    public static AddGraphRest createGraphRequest(@NonNull String graph) {
        AddGraphRest request = new AddGraphRest();
        request.setGraph(graph);
        return request;
    }

    /**
     * The factory to create {@link ExperimentTimeRequest} instance.
     *
     * @param startTimeMillis the experiment's start time in milliseconds.
     * @return the initialized {@link ExperimentTimeRequest} instance.
     */
    public static ExperimentTimeRequest createLogStartTimeRequest(long startTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setStartTimeMillis(startTimeMillis);
        return request;
    }

    /**
     * The factory to create {@link ExperimentTimeRequest} instance.
     *
     * @param endTimeMillis the experiment's end time in milliseconds.
     * @return the initialized {@link ExperimentTimeRequest} instance.
     */
    public static ExperimentTimeRequest createLogEndTimeRequest(long endTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setEndTimeMillis(endTimeMillis);
        return request;
    }

    /**
     * The factory to create {@link GitMetadataRest} request instance.
     *
     * @param metaData the {@link GitMetaData} model object from public API.
     * @return the initialized {@link GitMetadataRest} instance.
     */
    public static GitMetadataRest createGitMetadataRequest(GitMetaData metaData) {
        GitMetadataRest g = new GitMetadataRest();
        g.setUser(metaData.getUser());
        g.setOrigin(metaData.getOrigin());
        g.setBranch(metaData.getBranch());
        g.setParent(metaData.getParent());
        g.setRoot(metaData.getRoot());
        return g;
    }

    /**
     * The factory to create {@link ArtifactRequest} instance to be used to upsert Comet artifact.
     *
     * @param artifact the {@link ArtifactImpl} instance.
     * @return the initialized {@link ArtifactRequest} instance.
     */
    public static ArtifactRequest createArtifactUpsertRequest(final ArtifactImpl artifact) {
        ArtifactRequest r = new ArtifactRequest();
        r.setArtifactName(artifact.getName());
        r.setArtifactType(artifact.getType());
        if (artifact.getSemanticVersion() != null) {
            r.setVersion(artifact.getSemanticVersion().getValue());
        }
        if (artifact.getAliases() != null && artifact.getAliases().size() > 0) {
            r.setAlias(artifact.getAliases().toArray(new String[0]));
        }
        if (artifact.getVersionTags() != null && artifact.getVersionTags().size() > 0) {
            r.setVersionTags(artifact.getVersionTags().toArray(new String[0]));
        }
        if (artifact.getMetadata() != null) {
            r.setVersionMetadata(JsonUtils.toJson(artifact.getMetadata()));
        }
        return r;
    }

    /**
     * Creates request to signal the state of the artifact version send operation.
     *
     * @param artifactVersionId the identifier of the artifact version.
     * @param state             the {@link ArtifactVersionState} signaling state of the operation.
     * @return the properly initialized {@link ArtifactRequest} instance.
     */
    public static ArtifactRequest createArtifactVersionStateRequest(
            String artifactVersionId, ArtifactVersionState state) {
        ArtifactRequest r = new ArtifactRequest();
        r.setArtifactVersionId(artifactVersionId);
        r.setState(state);
        return r;
    }

    /**
     * Prepares request to create new registry record of the model associated with particular experiment.
     *
     * @param model the {@link RegistryModelImpl} instance holding all required data.
     * @return the properly initialized instance of {@link RegistryModelCreateRequest}.
     */
    public static RegistryModelCreateRequest createRegistryModelCreateRequest(RegistryModelImpl model) {
        RegistryModelCreateRequest request = new RegistryModelCreateRequest();
        request.setExperimentModelId(model.getExperimentModelId());
        request.setRegistryModelName(model.getRegistryName());
        request.setVersion(model.getVersion());
        request.setPublic(model.isPublic());
        request.setComment(model.getComment());
        request.setDescription(model.getDescription());
        request.setStages(model.getStages());
        return request;
    }

    /**
     * Prepares request which can be used to update the existing registry record of the model associated
     * with particular experiment.
     *
     * @param model the {@link RegistryModelImpl} instance holding all required data.
     * @return the properly initialized instance of {@link RegistryModelItemCreateRequest}.
     */
    public static RegistryModelItemCreateRequest createRegistryModelItemCreateRequest(RegistryModelImpl model) {
        RegistryModelItemCreateRequest request = new RegistryModelItemCreateRequest();
        request.setExperimentModelId(model.getExperimentModelId());
        request.setRegistryModelName(model.getRegistryName());
        request.setVersion(model.getVersion());
        request.setComment(model.getComment());
        request.setStages(model.getStages());
        return request;
    }

    /**
     * Creates request to create/update notes of the registry model.
     *
     * @param notes        the notes to be created.
     * @param registryName the registry model name.
     * @param workspace    the workspace name.
     * @return the properly initialized instance of {@link RegistryModelNotesUpdateRequest}.
     */
    public static RegistryModelNotesUpdateRequest createRegistryModelNotesUpdateRequest(
            String notes, String registryName, String workspace) {
        return new RegistryModelNotesUpdateRequest(notes, registryName, workspace);
    }

    /**
     * Creates request to update the registry model.
     *
     * @param registryModelName the new name for the model.
     * @param description       the new description for the model.
     * @param isPublic          the flag to change visibility status of the model.
     * @return the property initialized instance of {@link RegistryModelUpdateRequest}.
     */
    public static RegistryModelUpdateRequest createRegistryModelUpdateRequest(
            String registryModelName, String description, Boolean isPublic) {
        RegistryModelUpdateRequest request = new RegistryModelUpdateRequest();
        request.setRegistryModelName(registryModelName);
        request.setDescription(description);
        request.setIsPublic(isPublic);
        return request;
    }

    /**
     * Converts JSON encoded metadata into {@link Map} object.
     *
     * @param json the JSON encoded metadata string.
     * @return the instance of the {@link Map} object.
     */
    public Map<String, Object> metadataFromJson(String json) {
        return JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
    }

    /**
     * Creates query parameters to be used to download model from the Comet registry.
     *
     * @param workspace    the name of the model's workspace.
     * @param registryName the model's name in the registry.
     * @param options      the additional download options.
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> downloadModelParams(
            @NonNull String workspace, @NonNull String registryName, @NonNull DownloadModelOptions options) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(WORKSPACE_NAME, workspace);
        queryParams.put(MODEL_NAME, registryName);
        if (StringUtils.isNotBlank(options.getVersion())) {
            queryParams.put(VERSION, options.getVersion());
        }
        if (StringUtils.isNotBlank(options.getStage())) {
            queryParams.put(STAGE, options.getStage());
        }
        return queryParams;
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
     * Extracts query parameters from provided {@link GetArtifactOptions} object to be used for getting details about
     * particular artifact version.
     *
     * @param options       the {@link GetArtifactOptions}
     * @param experimentKey the current experiment's key
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> artifactVersionDetailsParams(
            @NonNull final GetArtifactOptions options, @NonNull String experimentKey) {
        Map<QueryParamName, String> queryParams = artifactVersionParams(options);
        queryParams.put(CONSUMER_EXPERIMENT_KEY, options.getConsumerExperimentKey());
        queryParams.put(EXPERIMENT_KEY, experimentKey);
        queryParams.put(PROJECT, options.getProject());
        queryParams.put(VERSION_OR_ALIAS, options.getVersionOrAlias());
        return queryParams;
    }

    /**
     * Extracts query parameters from provided {@link GetArtifactOptions} object to be used for getting list of assets
     * associated with particular artifact.
     *
     * @param options the {@link GetArtifactOptions}
     * @return the map with query parameters.
     */
    public static Map<QueryParamName, String> artifactVersionFilesParams(@NonNull final GetArtifactOptions options) {
        return artifactVersionParams(options);
    }

    static Map<QueryParamName, String> artifactVersionParams(@NonNull final GetArtifactOptions options) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(ALIAS, options.getAlias());
        queryParams.put(ARTIFACT_ID, options.getArtifactId());
        queryParams.put(ARTIFACT_NAME, options.getArtifactName());
        queryParams.put(VERSION_ID, options.getVersionId());
        queryParams.put(VERSION, options.getVersion());
        queryParams.put(WORKSPACE, options.getWorkspace());
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
    public static Map<QueryParamName, String> artifactDownloadAssetParams(
            @NonNull final DownloadArtifactAssetOptions options, @NonNull String experimentKey) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(EXPERIMENT_KEY, experimentKey);
        queryParams.put(ASSET_ID, options.getAssetId());
        queryParams.put(ARTIFACT_VERSION_ID, options.getArtifactVersionId());
        return queryParams;
    }
}
