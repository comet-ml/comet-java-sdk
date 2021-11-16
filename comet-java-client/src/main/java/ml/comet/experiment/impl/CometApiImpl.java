package ml.comet.experiment.impl;

import lombok.NonNull;
import ml.comet.experiment.CometApi;
import ml.comet.experiment.builder.BaseCometBuilder;
import ml.comet.experiment.builder.CometApiBuilder;
import ml.comet.experiment.impl.config.CometConfig;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.http.ConnectionInitializer;
import ml.comet.experiment.impl.utils.CometUtils;
import ml.comet.experiment.impl.utils.JsonUtils;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.GetExperimentsResponse;
import ml.comet.experiment.model.GetProjectsResponse;
import ml.comet.experiment.model.GetWorkspacesResponse;
import ml.comet.experiment.model.RestProject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_BASE_URL;
import static ml.comet.experiment.impl.config.CometConfig.COMET_MAX_AUTH_RETRIES;
import static ml.comet.experiment.impl.constants.ApiEndpoints.EXPERIMENTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.PROJECTS;
import static ml.comet.experiment.impl.constants.ApiEndpoints.WORKSPACES;
import static ml.comet.experiment.impl.constants.QueryParamName.PROJECT_ID;
import static ml.comet.experiment.impl.constants.QueryParamName.WORKSPACE_NAME;

/**
 * The implementation of the {@link  CometApi}.
 */
public final class CometApiImpl implements CometApi {
    private Logger logger = LoggerFactory.getLogger(CometApiImpl.class);
    private final Connection connection;

    CometApiImpl(@NonNull String apiKey, @NonNull String baseUrl, int maxAuthRetries, Logger logger) {
        if (logger != null) {
            this.logger = logger;
        }
        this.connection = ConnectionInitializer.initConnection(apiKey, baseUrl, maxAuthRetries, this.logger);
        CometUtils.printCometSdkVersion();
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public List<String> getAllWorkspaces() {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllWorkspaces invoked");
        }
        GetWorkspacesResponse response = getObject(WORKSPACES, Collections.emptyMap(), GetWorkspacesResponse.class);
        return response.getWorkspaceNames();
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public List<RestProject> getAllProjects(@NonNull String workspaceName) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllProjects invoked");
        }
        Map<QueryParamName, String> params = Collections.singletonMap(WORKSPACE_NAME, workspaceName);
        GetProjectsResponse response = getObject(PROJECTS, params, GetProjectsResponse.class);
        return response.getProjects();
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public List<ExperimentMetadataRest> getAllExperiments(@NonNull String projectId) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllExperiments invoked");
        }
        Map<QueryParamName, String> params = Collections.singletonMap(PROJECT_ID, projectId);
        GetExperimentsResponse response = getObject(EXPERIMENTS, params, GetExperimentsResponse.class);
        return response.getExperiments();
    }

    /**
     * Release all resources hold by this instance, such as connection to the Comet server.
     *
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void close() throws IOException {
        if (this.connection != null) {
            // no need to wait for asynchronous requests to proceed because this class use only synchronous requests
            this.connection.close();
        }
    }

    /**
     * Returns builder to be used to properly create instance of this class.
     *
     * @return the builder to be used to properly create instance of this class.
     */
    public static CometApiBuilder builder() {
        return new CometApiBuilderImpl();
    }

    private <T> T getObject(@NonNull String endpoint, @NonNull Map<QueryParamName, String> params, @NonNull Class<T> clazz) {
        return connection.sendGet(endpoint, params)
                .map(body -> JsonUtils.fromJson(body, clazz))
                .orElseThrow(() -> new IllegalArgumentException("Failed to parse endpoint response " + endpoint));
    }

    /**
     * The builder to create properly configured instance of the CometApiImpl.
     */
    static final class CometApiBuilderImpl implements CometApiBuilder {
        private String apiKey;
        private Logger logger;

        public CometApiBuilder withConfigOverride(@NonNull File overrideConfig) {
            CometConfig.applyConfigOverride(overrideConfig);
            return this;
        }

        @Override
        public BaseCometBuilder<CometApi> withLogger(@NonNull Logger logger) {
            this.logger = logger;
            return this;
        }

        public CometApiBuilder withApiKey(@NonNull String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Factory method to build fully initialized instance of the CometApiImpl.
         *
         * @return the fully initialized instance of the CometApiImpl.
         */
        public CometApi build() {
            if (StringUtils.isEmpty(this.apiKey)) {
                this.apiKey = COMET_API_KEY.getString();
            }
            return new CometApiImpl(
                    this.apiKey, COMET_BASE_URL.getString(), COMET_MAX_AUTH_RETRIES.getInt(), this.logger);
        }
    }
}
