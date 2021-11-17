package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.NonNull;
import ml.comet.experiment.impl.constants.AssetType;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.utils.JsonUtils;
import ml.comet.experiment.model.ExperimentAssetListResponse;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.GetExperimentsResponse;
import ml.comet.experiment.model.GetGraphResponse;
import ml.comet.experiment.model.GetHtmlResponse;
import ml.comet.experiment.model.GetOutputResponse;
import ml.comet.experiment.model.GetProjectsResponse;
import ml.comet.experiment.model.GetWorkspacesResponse;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.MinMaxResponse;
import ml.comet.experiment.model.TagsResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static ml.comet.experiment.impl.constants.ApiEndpoints.EXPERIMENTS;
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
import static ml.comet.experiment.impl.constants.ApiEndpoints.PROJECTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.WORKSPACES;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.PROJECT_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.TYPE;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE_NAME;

/**
 * Represents Comet REST API client providing access to all exposed REST endpoints.
 */
final class RestApiClient implements Disposable {
    private final Connection connection;
    private boolean disposed;

    RestApiClient(Connection connection) {
        this.connection = connection;
    }

    Connection getConnection() {
        return this.connection;
    }

    Observable<GetWorkspacesResponse> getAllWorkspaces() {
        return fromOptionalGetResponse(WORKSPACES, Collections.emptyMap(), GetWorkspacesResponse.class);
    }

    Observable<GetProjectsResponse> getAllProjects(String workspaceName) {
        return fromOptionalGetResponse(
                PROJECTS, Collections.singletonMap(WORKSPACE_NAME, workspaceName), GetProjectsResponse.class);
    }

    Observable<GetExperimentsResponse> getAllExperiments(String projectId) {
        return fromOptionalGetResponse(
                EXPERIMENTS, Collections.singletonMap(PROJECT_ID, projectId), GetExperimentsResponse.class);
    }

    Single<ExperimentMetadataRest> getMetadata(String experimentKey) {
        return singleGetForExperiment(GET_METADATA, experimentKey, ExperimentMetadataRest.class);
    }

    Single<GitMetadataRest> getGitMetadata(String experimentKey) {
        return singleGetForExperiment(GET_GIT_METADATA, experimentKey, GitMetadataRest.class);
    }

    Single<GetHtmlResponse> getHtml(String experimentKey) {
        return singleGetForExperiment(GET_HTML, experimentKey, GetHtmlResponse.class);
    }

    Single<GetOutputResponse> getOutput(String experimentKey) {
        return singleGetForExperiment(GET_OUTPUT, experimentKey, GetOutputResponse.class);
    }

    Single<GetGraphResponse> getGraph(String experimentKey) {
        return singleGetForExperiment(GET_GRAPH, experimentKey, GetGraphResponse.class);
    }

    Single<MinMaxResponse> getParameters(String experimentKey) {
        return singleGetForExperiment(GET_PARAMETERS, experimentKey, MinMaxResponse.class);
    }

    Single<MinMaxResponse> getMetrics(String experimentKey) {
        return singleGetForExperiment(GET_METRICS, experimentKey, MinMaxResponse.class);
    }

    Single<MinMaxResponse> getLogOther(String experimentKey) {
        return singleGetForExperiment(GET_LOG_OTHER, experimentKey, MinMaxResponse.class);
    }

    Single<TagsResponse> getTags(String experimentKey) {
        return singleGetForExperiment(GET_TAGS, experimentKey, TagsResponse.class);
    }

    Single<ExperimentAssetListResponse> getAssetList(String experimentKey, AssetType type) {
        HashMap<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, experimentKey);
            put(TYPE, type.type());
        }};
        return singleGetResponse(GET_ASSET_INFO, params, ExperimentAssetListResponse.class);
    }

    private <T> Single<T> singleGetForExperiment(@NonNull String endpoint,
                                                 @NonNull String experimentKey,
                                                 @NonNull Class<T> clazz) {
        return singleGetResponse(endpoint, Collections.singletonMap(EXPERIMENT_KEY, experimentKey), clazz);
    }

    private <T> Single<T> singleGetResponse(@NonNull String endpoint,
                                            @NonNull Map<QueryParamName, String> params,
                                            @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Single.error(new IllegalStateException("REST API client already disposed"));
        }
        return optionalGetRestObject(endpoint, params, clazz)
                .map(Single::just)
                .orElse(Single.error(new IllegalStateException(
                        String.format("No response was returned by endpoint %s", endpoint))));
    }


    private <T> Observable<T> fromOptionalGetResponse(@NonNull String endpoint,
                                                      @NonNull Map<QueryParamName, String> params,
                                                      @NonNull Class<T> clazz) {
        if (isDisposed()) {
            return Observable.empty();
        }
        return Observable.fromOptional(optionalGetRestObject(endpoint, params, clazz));
    }

    private <T> Optional<T> optionalGetRestObject(@NonNull String endpoint,
                                                  @NonNull Map<QueryParamName, String> params,
                                                  @NonNull Class<T> clazz) {
        return connection.sendGet(endpoint, params)
                .map(body -> JsonUtils.fromJson(body, clazz));
    }

    @Override
    public void dispose() {
        this.disposed = true;
    }

    @Override
    public boolean isDisposed() {
        return this.disposed;
    }
}
