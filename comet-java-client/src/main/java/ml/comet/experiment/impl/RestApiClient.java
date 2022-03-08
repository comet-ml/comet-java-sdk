package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.NonNull;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.GetArtifactOptions;
import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.asset.RemoteAsset;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import ml.comet.experiment.impl.asset.AssetImpl;
import ml.comet.experiment.impl.asset.DownloadArtifactAssetOptions;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.rest.AddExperimentTagsRest;
import ml.comet.experiment.impl.rest.AddGraphRest;
import ml.comet.experiment.impl.rest.ArtifactEntry;
import ml.comet.experiment.impl.rest.ArtifactRequest;
import ml.comet.experiment.impl.rest.ArtifactVersionAssetResponse;
import ml.comet.experiment.impl.rest.ArtifactVersionDetail;
import ml.comet.experiment.impl.rest.CometWebJavaSdkException;
import ml.comet.experiment.impl.rest.CreateExperimentRequest;
import ml.comet.experiment.impl.rest.CreateExperimentResponse;
import ml.comet.experiment.impl.rest.ExperimentAssetListResponse;
import ml.comet.experiment.impl.rest.ExperimentMetadataRest;
import ml.comet.experiment.impl.rest.ExperimentModelListResponse;
import ml.comet.experiment.impl.rest.ExperimentStatusResponse;
import ml.comet.experiment.impl.rest.ExperimentTimeRequest;
import ml.comet.experiment.impl.rest.GetExperimentsResponse;
import ml.comet.experiment.impl.rest.GetGraphResponse;
import ml.comet.experiment.impl.rest.GetHtmlResponse;
import ml.comet.experiment.impl.rest.GetOutputResponse;
import ml.comet.experiment.impl.rest.GetProjectsResponse;
import ml.comet.experiment.impl.rest.GetWorkspacesResponse;
import ml.comet.experiment.impl.rest.GitMetadataRest;
import ml.comet.experiment.impl.rest.HtmlRest;
import ml.comet.experiment.impl.rest.LogOtherRest;
import ml.comet.experiment.impl.rest.MetricRest;
import ml.comet.experiment.impl.rest.MinMaxResponse;
import ml.comet.experiment.impl.rest.OutputUpdate;
import ml.comet.experiment.impl.rest.ParameterRest;
import ml.comet.experiment.impl.rest.RegistryModelCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelCreateResponse;
import ml.comet.experiment.impl.rest.RegistryModelDetailsResponse;
import ml.comet.experiment.impl.rest.RegistryModelItemCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelItemCreateResponse;
import ml.comet.experiment.impl.rest.RegistryModelNotesResponse;
import ml.comet.experiment.impl.rest.RegistryModelNotesUpdateRequest;
import ml.comet.experiment.impl.rest.RegistryModelOverviewListResponse;
import ml.comet.experiment.impl.rest.RegistryModelUpdateItemRequest;
import ml.comet.experiment.impl.rest.RegistryModelUpdateRequest;
import ml.comet.experiment.impl.rest.RestApiResponse;
import ml.comet.experiment.impl.rest.SetSystemDetailsRequest;
import ml.comet.experiment.impl.rest.TagsResponse;
import ml.comet.experiment.impl.utils.JsonUtils;
import ml.comet.experiment.impl.utils.RestApiUtils;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import org.asynchttpclient.Response;

import java.io.File;
import java.io.OutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_ASSET;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_GIT_METADATA;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_GRAPH;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_HTML;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_LOG_OTHER;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_METRIC;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_OUTPUT;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_PARAMETER;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_START_END_TIME;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_TAG;
import static ml.comet.experiment.impl.constants.ApiEndpoints.CREATE_REGISTRY_MODEL;
import static ml.comet.experiment.impl.constants.ApiEndpoints.CREATE_REGISTRY_MODEL_ITEM;
import static ml.comet.experiment.impl.constants.ApiEndpoints.DELETE_REGISTRY_MODEL;
import static ml.comet.experiment.impl.constants.ApiEndpoints.DOWNLOAD_REGISTRY_MODEL;
import static ml.comet.experiment.impl.constants.ApiEndpoints.EXPERIMENTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_ARTIFACT_VERSION_DETAIL;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_ARTIFACT_VERSION_FILES;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_ASSETS_LIST;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_EXPERIMENT_ASSET;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_EXPERIMENT_MODEL_LIST;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_GIT_METADATA;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_GRAPH;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_HTML;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_LOG_OTHER;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_METADATA;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_METRICS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_OUTPUT;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_PARAMETERS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_REGISTRY_MODEL_DETAILS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_REGISTRY_MODEL_LIST;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_REGISTRY_MODEL_NOTES;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_TAGS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.NEW_EXPERIMENT;
import static ml.comet.experiment.impl.constants.ApiEndpoints.PROJECTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.SET_EXPERIMENT_STATUS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.SET_SYSTEM_DETAILS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.UPDATE_ARTIFACT_STATE;
import static ml.comet.experiment.impl.constants.ApiEndpoints.UPDATE_REGISTRY_MODEL;
import static ml.comet.experiment.impl.constants.ApiEndpoints.UPDATE_REGISTRY_MODEL_NOTES;
import static ml.comet.experiment.impl.constants.ApiEndpoints.UPDATE_REGISTRY_MODEL_VERSION;
import static ml.comet.experiment.impl.constants.ApiEndpoints.UPSERT_ARTIFACT;
import static ml.comet.experiment.impl.constants.ApiEndpoints.WORKSPACES;
import static ml.comet.experiment.impl.constants.FormParamName.LINK;
import static ml.comet.experiment.impl.constants.QueryParamName.ARTIFACT_VERSION_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.IS_REMOTE;
import static ml.comet.experiment.impl.constants.QueryParamName.MODEL_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.PROJECT_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.TYPE;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE_NAME;
import static ml.comet.experiment.impl.http.ConnectionUtils.checkResponseStatus;
import static ml.comet.experiment.impl.resources.LogMessages.NO_RESPONSE_RETURNED_BY_REMOTE_ENDPOINT;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.RestApiUtils.artifactDownloadAssetParams;
import static ml.comet.experiment.impl.utils.RestApiUtils.artifactVersionDetailsParams;
import static ml.comet.experiment.impl.utils.RestApiUtils.artifactVersionFilesParams;
import static ml.comet.experiment.impl.utils.RestApiUtils.downloadModelParams;

/**
 * Represents Comet REST API client providing access to all exposed REST endpoints.
 */
final class RestApiClient implements Disposable {
    private Connection connection;
    private boolean disposed;

    static final IllegalStateException ALREADY_DISPOSED = new IllegalStateException("REST API client already disposed");

    RestApiClient(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void dispose() {
        this.disposed = true;
        // release reference to the connection
        this.connection = null;
    }

    @Override
    public boolean isDisposed() {
        return this.disposed;
    }

    Single<GetWorkspacesResponse> getAllWorkspaces() {
        return singleFromSyncGetWithRetries(WORKSPACES, Collections.emptyMap(), GetWorkspacesResponse.class);
    }

    Single<GetProjectsResponse> getAllProjects(String workspaceName) {
        return singleFromSyncGetWithRetries(
                PROJECTS, Collections.singletonMap(WORKSPACE_NAME, workspaceName), GetProjectsResponse.class);
    }

    Single<GetExperimentsResponse> getAllExperiments(String projectId) {
        return singleFromSyncGetWithRetries(
                EXPERIMENTS, Collections.singletonMap(PROJECT_ID, projectId), GetExperimentsResponse.class);
    }

    Single<ExperimentMetadataRest> getMetadata(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_METADATA, experimentKey, ExperimentMetadataRest.class);
    }

    Single<GitMetadataRest> getGitMetadata(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_GIT_METADATA, experimentKey, GitMetadataRest.class);
    }

    Single<GetHtmlResponse> getHtml(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_HTML, experimentKey, GetHtmlResponse.class);
    }

    Single<GetOutputResponse> getOutput(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_OUTPUT, experimentKey, GetOutputResponse.class);
    }

    Single<GetGraphResponse> getGraph(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_GRAPH, experimentKey, GetGraphResponse.class);
    }

    Single<MinMaxResponse> getParameters(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_PARAMETERS, experimentKey, MinMaxResponse.class);
    }

    Single<MinMaxResponse> getMetrics(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_METRICS, experimentKey, MinMaxResponse.class);
    }

    Single<MinMaxResponse> getLogOther(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_LOG_OTHER, experimentKey, MinMaxResponse.class);
    }

    Single<TagsResponse> getTags(String experimentKey) {
        return singleFromSyncGetWithRetries(GET_TAGS, experimentKey, TagsResponse.class);
    }

    Single<ExperimentAssetListResponse> getAssetList(String experimentKey, String type) {
        HashMap<QueryParamName, String> params = new HashMap<>();
        params.put(EXPERIMENT_KEY, experimentKey);
        params.put(TYPE, type);
        return singleFromSyncGetWithRetries(GET_ASSETS_LIST, params, ExperimentAssetListResponse.class);
    }

    Single<ExperimentStatusResponse> sendExperimentStatus(String experimentKey) {
        return singleFromSyncGetWithRetries(SET_EXPERIMENT_STATUS, experimentKey, ExperimentStatusResponse.class);
    }

    Single<RestApiResponse> logMetric(final MetricRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_METRIC, RestApiResponse.class);
    }

    Single<RestApiResponse> logParameter(final ParameterRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_PARAMETER, RestApiResponse.class);
    }

    Single<RestApiResponse> logOutputLine(final OutputUpdate request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_OUTPUT, RestApiResponse.class);
    }

    Single<RestApiResponse> logHtml(final HtmlRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_HTML, RestApiResponse.class);
    }

    Single<RestApiResponse> logOther(final LogOtherRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_LOG_OTHER, RestApiResponse.class);
    }

    Single<RestApiResponse> addTag(final AddExperimentTagsRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_TAG, RestApiResponse.class);
    }

    Single<RestApiResponse> logGraph(final AddGraphRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_GRAPH, RestApiResponse.class);
    }

    Single<RestApiResponse> logStartEndTime(final ExperimentTimeRequest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_START_END_TIME, RestApiResponse.class);
    }

    Single<RestApiResponse> logGitMetadata(final GitMetadataRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_GIT_METADATA, RestApiResponse.class);
    }

    Single<CreateExperimentResponse> registerExperiment(final CreateExperimentRequest request) {
        return singleFromSyncPostWithRetries(request, NEW_EXPERIMENT, true, CreateExperimentResponse.class);
    }

    Single<RestApiResponse> logSystemDetails(final SetSystemDetailsRequest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, SET_SYSTEM_DETAILS, RestApiResponse.class);
    }

    <T extends Asset> Single<RestApiResponse> logAsset(final T asset, String experimentKey) {
        Map<QueryParamName, String> queryParams = RestApiUtils.assetQueryParameters((AssetImpl) asset, experimentKey);
        Map<FormParamName, Object> formParams = RestApiUtils.assetFormParameters(asset);
        if (asset instanceof ArtifactAsset) {
            queryParams.put(ARTIFACT_VERSION_ID, ((ArtifactAssetImpl) asset).getArtifactVersionId());
        }

        // call appropriate send method
        if (asset.getFile().isPresent()) {
            return singleFromAsyncPost(asset.getFile().get(), ADD_ASSET, queryParams,
                    formParams, RestApiResponse.class);
        } else if (asset.getFileLikeData().isPresent()) {
            return singleFromAsyncPost(asset.getFileLikeData().get(), ADD_ASSET, queryParams,
                    formParams, RestApiResponse.class);
        }

        // no data response
        RestApiResponse response = new RestApiResponse();
        response.setMsg("asset has no data");
        response.setCode(-1);
        return Single.just(response);
    }

    <T extends RemoteAsset> Single<RestApiResponse> logRemoteAsset(final T asset, String experimentKey) {
        Map<QueryParamName, String> queryParams = RestApiUtils.assetQueryParameters((AssetImpl) asset, experimentKey);
        queryParams.put(IS_REMOTE, Boolean.TRUE.toString());
        if (asset instanceof ArtifactAsset) {
            queryParams.put(ARTIFACT_VERSION_ID, ((ArtifactAssetImpl) asset).getArtifactVersionId());
        }

        Map<FormParamName, Object> formParams = RestApiUtils.assetFormParameters(asset);
        if (asset.getLink().isPresent()) {
            formParams.put(LINK, asset.getLink().get().toASCIIString());
        }

        return singleFromAsyncPost(ADD_ASSET, queryParams, formParams, RestApiResponse.class);
    }

    Single<ArtifactEntry> upsertArtifact(final ArtifactRequest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromSyncPostWithRetries(request, UPSERT_ARTIFACT, true, ArtifactEntry.class);
    }

    Single<RestApiResponse> updateArtifactState(final ArtifactRequest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromSyncPostWithRetriesEmptyBody(request, UPDATE_ARTIFACT_STATE);
    }

    Single<ArtifactVersionDetail> getArtifactVersionDetail(
            final GetArtifactOptions request, String experimentKey) {

        return this.singleFromSyncGetWithRetries(
                GET_ARTIFACT_VERSION_DETAIL, artifactVersionDetailsParams(request, experimentKey),
                true, ArtifactVersionDetail.class);
    }

    Single<ArtifactVersionAssetResponse> getArtifactVersionFiles(final GetArtifactOptions request) {
        return this.singleFromSyncGetWithRetries(
                GET_ARTIFACT_VERSION_FILES, artifactVersionFilesParams(request),
                true, ArtifactVersionAssetResponse.class);
    }

    Single<RestApiResponse> downloadArtifactAsset(final DownloadArtifactAssetOptions options, String experimentKey) {
        Map<QueryParamName, String> queryParams = artifactDownloadAssetParams(options, experimentKey);
        return this.singleFromAsyncDownload(options.getFile(), GET_EXPERIMENT_ASSET, queryParams);
    }

    Single<ExperimentModelListResponse> getExperimentModels(String experimentKey) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(EXPERIMENT_KEY, experimentKey);
        return this.singleFromSyncGetWithRetries(
                GET_EXPERIMENT_MODEL_LIST, queryParams, ExperimentModelListResponse.class);
    }

    Single<RegistryModelOverviewListResponse> getRegistryModelsForWorkspace(String workspaceName) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(WORKSPACE_NAME, workspaceName);
        return this.singleFromSyncGetWithRetries(
                GET_REGISTRY_MODEL_LIST, queryParams, RegistryModelOverviewListResponse.class);
    }

    Single<RegistryModelCreateResponse> createRegistryModel(final RegistryModelCreateRequest request) {
        return singleFromSyncPostWithRetries(request, CREATE_REGISTRY_MODEL, true,
                RegistryModelCreateResponse.class);
    }

    Single<RegistryModelItemCreateResponse> createRegistryModelItem(final RegistryModelItemCreateRequest request) {
        return singleFromSyncPostWithRetries(request, CREATE_REGISTRY_MODEL_ITEM, true,
                RegistryModelItemCreateResponse.class);
    }

    Single<RegistryModelDetailsResponse> getRegistryModelDetails(String modelName, String workspaceName) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(WORKSPACE_NAME, workspaceName);
        queryParams.put(MODEL_NAME, modelName);
        return singleFromSyncGetWithRetries(GET_REGISTRY_MODEL_DETAILS, queryParams, true,
                RegistryModelDetailsResponse.class);
    }

    Single<RestApiResponse> downloadRegistryModel(
            final OutputStream output, String workspace, String registryName, final DownloadModelOptions options) {
        Map<QueryParamName, String> queryParams = downloadModelParams(workspace, registryName, options);
        return this.singleFromAsyncDownload(output, DOWNLOAD_REGISTRY_MODEL, queryParams);
    }

    Single<RegistryModelNotesResponse> getRegistryModelNotes(String modelName, String workspaceName) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(WORKSPACE_NAME, workspaceName);
        queryParams.put(MODEL_NAME, modelName);
        return singleFromSyncGetWithRetries(GET_REGISTRY_MODEL_NOTES, queryParams, true,
                RegistryModelNotesResponse.class);
    }

    Single<RestApiResponse> updateRegistryModelNotes(RegistryModelNotesUpdateRequest request) {
        return singleFromAsyncPost(request, UPDATE_REGISTRY_MODEL_NOTES);
    }

    Single<RestApiResponse> updateRegistryModel(RegistryModelUpdateRequest request) {
        return singleFromAsyncPost(request, UPDATE_REGISTRY_MODEL);
    }

    Single<RestApiResponse> updateRegistryModelVersion(RegistryModelUpdateItemRequest request) {
        return singleFromAsyncPost(request, UPDATE_REGISTRY_MODEL_VERSION);
    }

    Single<RestApiResponse> deleteRegistryModel(String modelName, String workspaceName) {
        Map<QueryParamName, String> queryParams = new HashMap<>();
        queryParams.put(WORKSPACE_NAME, workspaceName);
        queryParams.put(MODEL_NAME, modelName);
        return singleFromSyncGetWithRetries(DELETE_REGISTRY_MODEL, queryParams);
    }

    private Single<RestApiResponse> singleFromAsyncDownload(@NonNull OutputStream output,
                                                            @NonNull String endpoint,
                                                            @NonNull Map<QueryParamName, String> queryParams) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }
        return Single.fromFuture(this.connection.downloadAsync(output, endpoint, queryParams))
                .map(RestApiClient::mapResponse);
    }

    private Single<RestApiResponse> singleFromAsyncDownload(@NonNull File file,
                                                            @NonNull String endpoint,
                                                            @NonNull Map<QueryParamName, String> queryParams) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }

        return Single.fromFuture(this.connection.downloadAsync(file, endpoint, queryParams))
                .map(RestApiClient::mapResponse);
    }

    private Single<RestApiResponse> singleFromAsyncPost(@NonNull Object payload,
                                                        @NonNull String endpoint) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }
        return Single.fromFuture(this.connection.sendPostAsync(JsonUtils.toJson(payload), endpoint))
                .onTerminateDetach()
                .map(RestApiClient::mapResponse);
    }

    private <T> Single<T> singleFromAsyncPost(@NonNull String endpoint,
                                              @NonNull Map<QueryParamName, String> queryParams,
                                              @NonNull Map<FormParamName, Object> formParams,
                                              @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }
        return Single.fromFuture(this.connection.sendPostAsync(endpoint, queryParams, formParams))
                .onTerminateDetach()
                .map(response -> JsonUtils.fromJson(response.getResponseBody(), clazz));
    }

    private <T> Single<T> singleFromAsyncPost(
            byte[] fileLikeData, @NonNull String endpoint,
            @NonNull Map<QueryParamName, String> queryParams, Map<FormParamName, Object> formParams,
            @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }

        return Single.fromFuture(this.connection.sendPostAsync(fileLikeData, endpoint, queryParams, formParams))
                .onTerminateDetach()
                .map(response -> JsonUtils.fromJson(response.getResponseBody(), clazz));
    }

    private <T> Single<T> singleFromAsyncPost(
            @NonNull File file, @NonNull String endpoint,
            @NonNull Map<QueryParamName, String> queryParams, Map<FormParamName, Object> formParams,
            @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }

        return Single.fromFuture(this.connection.sendPostAsync(file, endpoint, queryParams, formParams))
                .onTerminateDetach()
                .map(response -> JsonUtils.fromJson(response.getResponseBody(), clazz));
    }

    private <T> Single<T> singleFromAsyncPost(
            @NonNull Object payload, @NonNull String endpoint, @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }

        return Single.fromFuture(this.connection.sendPostAsync(JsonUtils.toJson(payload), endpoint))
                .onTerminateDetach()
                .map(response -> JsonUtils.fromJson(response.getResponseBody(), clazz));
    }

    private <T> Single<T> singleFromSyncPostWithRetries(@NonNull Object payload,
                                                        @NonNull String endpoint,
                                                        boolean throwOnFailure,
                                                        @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }

        String request = JsonUtils.toJson(payload);
        return this.connection.sendPostWithRetries(request, endpoint, throwOnFailure)
                .map(body -> Single.just(JsonUtils.fromJson(body, clazz)))
                .orElse(Single.error(new CometApiException(
                        String.format("No response was returned by endpoint: %s", endpoint))));
    }

    private Single<RestApiResponse> singleFromSyncPostWithRetriesEmptyBody(@NonNull Object payload,
                                                                           @NonNull String endpoint) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }

        return this.connection.sendPostWithRetries(JsonUtils.toJson(payload), endpoint, true)
                .map(body -> Single.just(new RestApiResponse(200, body)))
                .orElse(Single.error(new CometApiException(
                        getString(NO_RESPONSE_RETURNED_BY_REMOTE_ENDPOINT, endpoint))));
    }

    private Single<RestApiResponse> singleFromSyncGetWithRetries(@NonNull String endpoint,
                                                                 @NonNull Map<QueryParamName, String> params) {
        return this.connection.sendGetWithRetries(endpoint, params, true)
                .map(body -> Single.just(new RestApiResponse(200, body)))
                .orElse(Single.error(new CometApiException(
                        getString(NO_RESPONSE_RETURNED_BY_REMOTE_ENDPOINT, endpoint))));
    }

    private <T> Single<T> singleFromSyncGetWithRetries(@NonNull String endpoint,
                                                       @NonNull String experimentKey,
                                                       @NonNull Class<T> clazz) {
        return singleFromSyncGetWithRetries(endpoint, Collections.singletonMap(EXPERIMENT_KEY, experimentKey),
                false, clazz);
    }

    private <T> Single<T> singleFromSyncGetWithRetries(@NonNull String endpoint,
                                                       @NonNull Map<QueryParamName, String> params,
                                                       @NonNull Class<T> clazz) {
        return singleFromSyncGetWithRetries(endpoint, params, false, clazz);
    }

    private <T> Single<T> singleFromSyncGetWithRetries(@NonNull String endpoint,
                                                       @NonNull Map<QueryParamName, String> queryParams,
                                                       boolean throwOnFailure,
                                                       @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }
        return this.connection.sendGetWithRetries(endpoint, queryParams, throwOnFailure)
                .map(body -> Single.just(JsonUtils.fromJson(body, clazz)))
                .orElse(Single.error(new CometApiException(
                        String.format("No response was returned by endpoint: %s", endpoint))));
    }

    private static RestApiResponse mapResponse(Response response) {
        try {
            checkResponseStatus(response);
        } catch (CometApiException ex) {
            return new RestApiResponse(ex.getStatusCode(), ex.getStatusMessage());
        } catch (CometWebJavaSdkException ex) {
            return new RestApiResponse(ex.getCode(), ex.getMsg(), ex.getSdkErrorCode());
        }
        return new RestApiResponse(200);
    }
}
