package ml.comet.experiment;

import com.typesafe.config.Config;
import lombok.NonNull;
import ml.comet.experiment.http.Connection;
import ml.comet.experiment.http.ConnectionInitializer;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.GetExperimentsResponse;
import ml.comet.experiment.model.GetProjectsResponse;
import ml.comet.experiment.model.GetWorkspacesResponse;
import ml.comet.experiment.model.RestProject;
import ml.comet.experiment.utils.ConfigUtils;
import ml.comet.experiment.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ml.comet.experiment.constants.Constants.BASE_URL_PLACEHOLDER;
import static ml.comet.experiment.constants.Constants.COMET_API_KEY;
import static ml.comet.experiment.constants.Constants.EXPERIMENTS;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_PLACEHOLDER;
import static ml.comet.experiment.constants.Constants.PROJECTS;
import static ml.comet.experiment.constants.Constants.WORKSPACES;

/**
 * The CometApi implementation.
 */
public final class CometApiImpl implements CometApi {
    private final Connection connection;

    public CometApiImpl(@NonNull String apiKey, @NonNull String baseUrl, int maxAuthRetries) {
        Logger logger = LoggerFactory.getLogger(CometApiImpl.class);
        this.connection = ConnectionInitializer.initConnection(apiKey, baseUrl, maxAuthRetries, logger);
    }

    public List<String> getAllWorkspaces() {
        GetWorkspacesResponse response = getObject(WORKSPACES, Collections.emptyMap(), GetWorkspacesResponse.class);
        return response.getWorkspaceNames();
    }

    public List<RestProject> getAllProjects(@NonNull String workspaceName) {
        Map<String, String> params = Collections.singletonMap("workspaceName", workspaceName);
        GetProjectsResponse response = getObject(PROJECTS, params, GetProjectsResponse.class);
        return response.getProjects();
    }

    public List<ExperimentMetadataRest> getAllExperiments(@NonNull String projectId) {
        Map<String, String> params = Collections.singletonMap("projectId", projectId);
        GetExperimentsResponse response = getObject(EXPERIMENTS, params, GetExperimentsResponse.class);
        return response.getExperiments();
    }

    private <T> T getObject(@NonNull String endpoint, @NonNull Map<String, String> params, @NonNull Class<T> clazz) {
        return connection.sendGet(endpoint, params)
                .map(body -> JsonUtils.fromJson(body, clazz))
                .orElseThrow(() -> new IllegalArgumentException("Failed to parse endpoint response " + endpoint));
    }

    /**
     * Returns builder to be used to properly create instance of this class.
     *
     * @return the builder to be used to properly create instance of this class.
     */
    public static CometApiBuilder builder() {
        return new CometApiBuilder();
    }

    /**
     * Release all resources hold by this instance, such as connection to the Comet server.
     *
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void close() throws IOException {
        if (this.connection != null) {
            this.connection.close();
        }
    }

    /**
     * The builder to create properly configured instance of the CometApiImpl.
     */
    public static final class CometApiBuilder {
        private String apiKey;
        private String baseUrl;
        private int maxAuthRetries;

        CometApiBuilder() {
            this.apiKey = ConfigUtils.getApiKey().orElse(null);
            this.baseUrl = ConfigUtils.getBaseUrlOrDefault();
            this.maxAuthRetries = ConfigUtils.getMaxAuthRetriesOrDefault();
        }

        /**
         * Provides file with configuration parameters to override default configuration options.
         *
         * @param overrideConfig the file with configuration parameters.
         * @return this builder with override configuration parameters.
         */
        public CometApiBuilder withConfig(@NonNull File overrideConfig) {
            Config config = ConfigUtils.getConfigFromFile(overrideConfig);
            this.apiKey = config.getString(COMET_API_KEY);
            this.baseUrl = config.getString(BASE_URL_PLACEHOLDER);
            this.maxAuthRetries = config.getInt(MAX_AUTH_RETRIES_PLACEHOLDER);
            return this;
        }

        /**
         * Supplies builder with Comet API key to.
         *
         * @param apiKey the Comet API key to get access to the server.
         * @return this builder with Comet API key configured.
         */
        public CometApiBuilder withApiKey(@NonNull String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public CometApiImpl build() {
            return new CometApiImpl(apiKey, baseUrl, maxAuthRetries);
        }
    }
}
