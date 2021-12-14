package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.NonNull;
import ml.comet.experiment.artifact.GetArtifactOptions;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.impl.asset.Asset;
import ml.comet.experiment.impl.asset.RemoteAsset;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.rest.AddExperimentTagsRest;
import ml.comet.experiment.impl.rest.AddGraphRest;
import ml.comet.experiment.impl.rest.ArtifactEntry;
import ml.comet.experiment.impl.rest.ArtifactRequest;
import ml.comet.experiment.impl.rest.ArtifactVersionDetail;
import ml.comet.experiment.impl.rest.CreateExperimentRequest;
import ml.comet.experiment.impl.rest.CreateExperimentResponse;
import ml.comet.experiment.impl.rest.ExperimentAssetListResponse;
import ml.comet.experiment.impl.rest.ExperimentMetadataRest;
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
import ml.comet.experiment.impl.rest.LogDataResponse;
import ml.comet.experiment.impl.rest.LogOtherRest;
import ml.comet.experiment.impl.rest.MetricRest;
import ml.comet.experiment.impl.rest.MinMaxResponse;
import ml.comet.experiment.impl.rest.OutputUpdate;
import ml.comet.experiment.impl.rest.ParameterRest;
import ml.comet.experiment.impl.rest.TagsResponse;
import ml.comet.experiment.impl.utils.AssetUtils;
import ml.comet.experiment.impl.utils.JsonUtils;
import ml.comet.experiment.model.AssetType;

import java.io.File;
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
import static ml.comet.experiment.impl.constants.ApiEndpoints.EXPERIMENTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_ARTIFACT_VERSION_DETAIL;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_ASSET_INFO;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_GIT_METADATA;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_GRAPH;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_HTML;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_LOG_OTHER;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_METADATA;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_METRICS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_OUTPUT;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_PARAMETERS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.GET_TAGS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.NEW_EXPERIMENT;
import static ml.comet.experiment.impl.constants.ApiEndpoints.PROJECTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.SET_EXPERIMENT_STATUS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.UPDATE_ARTIFACT_STATE;
import static ml.comet.experiment.impl.constants.ApiEndpoints.UPSERT_ARTIFACT;
import static ml.comet.experiment.impl.constants.ApiEndpoints.WORKSPACES;
import static ml.comet.experiment.impl.constants.FormParamName.LINK;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.IS_REMOTE;
import static ml.comet.experiment.impl.constants.QueryParamName.PROJECT_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.TYPE;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE_NAME;
import static ml.comet.experiment.impl.utils.ArtifactUtils.versionDetailsParams;

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

    Single<GetWorkspacesResponse> getAllWorkspaces() {
        return singleFromSyncGet(WORKSPACES, Collections.emptyMap(), GetWorkspacesResponse.class);
    }

    Single<GetProjectsResponse> getAllProjects(String workspaceName) {
        return singleFromSyncGet(
                PROJECTS, Collections.singletonMap(WORKSPACE_NAME, workspaceName), GetProjectsResponse.class);
    }

    Single<GetExperimentsResponse> getAllExperiments(String projectId) {
        return singleFromSyncGet(
                EXPERIMENTS, Collections.singletonMap(PROJECT_ID, projectId), GetExperimentsResponse.class);
    }

    Single<ExperimentMetadataRest> getMetadata(String experimentKey) {
        return singleFromSyncGet(GET_METADATA, experimentKey, ExperimentMetadataRest.class);
    }

    Single<GitMetadataRest> getGitMetadata(String experimentKey) {
        return singleFromSyncGet(GET_GIT_METADATA, experimentKey, GitMetadataRest.class);
    }

    Single<GetHtmlResponse> getHtml(String experimentKey) {
        return singleFromSyncGet(GET_HTML, experimentKey, GetHtmlResponse.class);
    }

    Single<GetOutputResponse> getOutput(String experimentKey) {
        return singleFromSyncGet(GET_OUTPUT, experimentKey, GetOutputResponse.class);
    }

    Single<GetGraphResponse> getGraph(String experimentKey) {
        return singleFromSyncGet(GET_GRAPH, experimentKey, GetGraphResponse.class);
    }

    Single<MinMaxResponse> getParameters(String experimentKey) {
        return singleFromSyncGet(GET_PARAMETERS, experimentKey, MinMaxResponse.class);
    }

    Single<MinMaxResponse> getMetrics(String experimentKey) {
        return singleFromSyncGet(GET_METRICS, experimentKey, MinMaxResponse.class);
    }

    Single<MinMaxResponse> getLogOther(String experimentKey) {
        return singleFromSyncGet(GET_LOG_OTHER, experimentKey, MinMaxResponse.class);
    }

    Single<TagsResponse> getTags(String experimentKey) {
        return singleFromSyncGet(GET_TAGS, experimentKey, TagsResponse.class);
    }

    Single<ExperimentAssetListResponse> getAssetList(String experimentKey, AssetType type) {
        HashMap<QueryParamName, String> params = new HashMap<>();
        params.put(EXPERIMENT_KEY, experimentKey);
        params.put(TYPE, type.type());
        return singleFromSyncGet(GET_ASSET_INFO, params, ExperimentAssetListResponse.class);
    }

    Single<ExperimentStatusResponse> sendExperimentStatus(String experimentKey) {
        return singleFromSyncGet(SET_EXPERIMENT_STATUS, experimentKey, ExperimentStatusResponse.class);
    }

    Single<LogDataResponse> logMetric(final MetricRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_METRIC, LogDataResponse.class);
    }

    Single<LogDataResponse> logParameter(final ParameterRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_PARAMETER, LogDataResponse.class);
    }

    Single<LogDataResponse> logOutputLine(final OutputUpdate request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_OUTPUT, LogDataResponse.class);
    }

    Single<LogDataResponse> logHtml(final HtmlRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_HTML, LogDataResponse.class);
    }

    Single<LogDataResponse> logOther(final LogOtherRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_LOG_OTHER, LogDataResponse.class);
    }

    Single<LogDataResponse> addTag(final AddExperimentTagsRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_TAG, LogDataResponse.class);
    }

    Single<LogDataResponse> logGraph(final AddGraphRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_GRAPH, LogDataResponse.class);
    }

    Single<LogDataResponse> logStartEndTime(final ExperimentTimeRequest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_START_END_TIME, LogDataResponse.class);
    }

    Single<LogDataResponse> logGitMetadata(final GitMetadataRest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromAsyncPost(request, ADD_GIT_METADATA, LogDataResponse.class);
    }

    Single<CreateExperimentResponse> registerExperiment(final CreateExperimentRequest request) {
        return singleFromSyncPost(request, NEW_EXPERIMENT, true, CreateExperimentResponse.class);
    }

    Single<LogDataResponse> logAsset(final Asset asset, String experimentKey) {
        Map<QueryParamName, String> queryParams = AssetUtils.assetQueryParameters(asset, experimentKey);
        Map<FormParamName, Object> formParams = AssetUtils.assetFormParameters(asset);

        // call appropriate send method
        if (asset.getFile() != null) {
            return singleFromAsyncPost(asset.getFile(), ADD_ASSET, queryParams,
                    formParams, LogDataResponse.class);
        } else if (asset.getFileLikeData() != null) {
            return singleFromAsyncPost(asset.getFileLikeData(), ADD_ASSET, queryParams,
                    formParams, LogDataResponse.class);
        }

        // no data response
        LogDataResponse response = new LogDataResponse();
        response.setMsg("asset has no data");
        response.setCode(-1);
        return Single.just(response);
    }

    Single<LogDataResponse> logRemoteAsset(final RemoteAsset asset, String experimentKey) {
        Map<QueryParamName, String> queryParams = AssetUtils.assetQueryParameters(asset, experimentKey);
        queryParams.put(IS_REMOTE, Boolean.valueOf(true).toString());

        Map<FormParamName, Object> formParams = AssetUtils.assetFormParameters(asset);
        formParams.put(LINK, asset.getLink().toASCIIString());

        return singleFromAsyncPost(ADD_ASSET, queryParams, formParams, LogDataResponse.class);
    }

    Single<ArtifactEntry> upsertArtifact(final ArtifactRequest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromSyncPost(request, UPSERT_ARTIFACT, true, ArtifactEntry.class);
    }

    Single<LogDataResponse> updateArtifactState(final ArtifactRequest request, String experimentKey) {
        request.setExperimentKey(experimentKey);
        return singleFromSyncPost(request, UPDATE_ARTIFACT_STATE, true, LogDataResponse.class);
    }

    Single<ArtifactVersionDetail> getArtifactVersionDetail(
            final GetArtifactOptions request, String experimentKey) {

        return singleFromSyncGet(
                GET_ARTIFACT_VERSION_DETAIL, versionDetailsParams(request, experimentKey), ArtifactVersionDetail.class);
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

    private <T> Single<T> singleFromSyncPost(@NonNull Object payload,
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

    private <T> Single<T> singleFromSyncGet(@NonNull String endpoint,
                                            @NonNull String experimentKey,
                                            @NonNull Class<T> clazz) {
        return singleFromSyncGet(endpoint, Collections.singletonMap(EXPERIMENT_KEY, experimentKey), clazz);
    }

    private <T> Single<T> singleFromSyncGet(@NonNull String endpoint,
                                            @NonNull Map<QueryParamName, String> params,
                                            @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(ALREADY_DISPOSED);
        }
        return this.connection.sendGetWithRetries(endpoint, params)
                .map(body -> Single.just(JsonUtils.fromJson(body, clazz)))
                .orElse(Single.error(new CometApiException(
                        String.format("No response was returned by endpoint: %s", endpoint))));
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
}
