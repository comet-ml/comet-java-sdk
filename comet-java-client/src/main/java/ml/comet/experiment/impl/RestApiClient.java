package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.utils.JsonUtils;
import ml.comet.experiment.model.GetExperimentsResponse;
import ml.comet.experiment.model.GetProjectsResponse;
import ml.comet.experiment.model.GetWorkspacesResponse;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static ml.comet.experiment.impl.constants.ApiEndpoints.EXPERIMENTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.PROJECTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.WORKSPACES;
import static ml.comet.experiment.impl.constants.QueryParamName.PROJECT_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE_NAME;

/**
 * Represents Comet REST API client providing access to all exposed REST endpoints.
 */
final class RestApiClient {
    private final Connection connection;

    RestApiClient(Connection connection) {
        this.connection = connection;
    }

    /**
     * Gets all workspaces available for current API key.
     *
     * @return {@link GetWorkspacesResponse} with workspace names.
     */
    Observable<GetWorkspacesResponse> getAllWorkspaces() {
        return fromOptionalResponse(WORKSPACES, Collections.emptyMap(), GetWorkspacesResponse.class);
    }

    /**
     * Gets all project DTOs under specified workspace name.
     *
     * @param workspaceName workspace name
     * @return {@link GetProjectsResponse} with projects in the workspace.
     */
    Observable<GetProjectsResponse> getAllProjects(String workspaceName) {
        return fromOptionalResponse(
                PROJECTS, Collections.singletonMap(WORKSPACE_NAME, workspaceName), GetProjectsResponse.class);
    }

    /**
     * Gets all experiment DTOs under specified project id.
     *
     * @param projectId Project id
     * @return {@link GetExperimentsResponse} with experiments associated with given project ID.
     */
    Observable<GetExperimentsResponse> getAllExperiments(String projectId) {
        return fromOptionalResponse(
                EXPERIMENTS, Collections.singletonMap(PROJECT_ID, projectId), GetExperimentsResponse.class);
    }

    private <T> Observable<T> fromOptionalResponse(@NonNull String endpoint, @NonNull Map<QueryParamName,
            String> params, @NonNull Class<T> clazz) {
        return Observable.fromOptional(getOptionalRestObject(endpoint, params, clazz));
    }

    private <T> Optional<T> getOptionalRestObject(@NonNull String endpoint, @NonNull Map<QueryParamName,
            String> params, @NonNull Class<T> clazz) {
        return connection.sendGet(endpoint, params)
                .map(body -> JsonUtils.fromJson(body, clazz));
    }
}
