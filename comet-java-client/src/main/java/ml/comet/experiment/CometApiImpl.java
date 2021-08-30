package ml.comet.experiment;

import com.typesafe.config.Config;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static ml.comet.experiment.constants.Constants.BASE_URL_PLACEHOLDER;
import static ml.comet.experiment.constants.Constants.COMET_API_KEY;
import static ml.comet.experiment.constants.Constants.EXPERIMENTS;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_PLACEHOLDER;
import static ml.comet.experiment.constants.Constants.PROJECTS;
import static ml.comet.experiment.constants.Constants.WORKSPACES;

public class CometApiImpl implements CometApi {
    private final Connection connection;

    public CometApiImpl(String apiKey, String baseUrl, int maxAuthRetries) {
        Logger logger = LoggerFactory.getLogger(CometApiImpl.class);
        this.connection = ConnectionInitializer.initConnection(apiKey, baseUrl, maxAuthRetries, logger);
    }

    public List<String> getAllWorkspaces() {
        GetWorkspacesResponse response = getObject(WORKSPACES, Collections.emptyMap(), GetWorkspacesResponse.class);
        return response.getWorkspaceNames();
    }

    public List<RestProject> getAllProjects(String workspaceName) {
        Map<String, String> params = Collections.singletonMap("workspaceName", workspaceName);
        GetProjectsResponse response = getObject(PROJECTS, params, GetProjectsResponse.class);
        return response.getProjects();
    }

    public List<ExperimentMetadataRest> getAllExperiments(String projectId) {
        Map<String, String> params = Collections.singletonMap("projectId", projectId);
        GetExperimentsResponse response = getObject(EXPERIMENTS, params, GetExperimentsResponse.class);
        return response.getExperiments();
    }

    private <T> T getObject(String endpoint, Map<String, String> params, Class<T> clazz) {
        return connection.sendGet(endpoint, params)
                .map(body -> JsonUtils.fromJson(body, clazz))
                .orElseThrow(() -> new IllegalArgumentException("Failed to parse endpoint response " + endpoint));
    }

    public static CometApiBuilder builder() {
        return new CometApiBuilder();
    }

    public static class CometApiBuilder {
        private String apiKey;
        private String baseUrl;
        private int maxAuthRetries;

        public CometApiBuilder() {
            this.apiKey = ConfigUtils.getApiKey().orElse(null);
            this.baseUrl = ConfigUtils.getBaseUrlOrDefault();
            this.maxAuthRetries = ConfigUtils.getMaxAuthRetriesOrDefault();
        }

        public CometApiBuilder withConfig(File overrideConfig) {
            Config config = ConfigUtils.getConfigFromFile(overrideConfig);
            this.apiKey = config.getString(COMET_API_KEY);
            this.baseUrl = config.getString(BASE_URL_PLACEHOLDER);
            this.maxAuthRetries = config.getInt(MAX_AUTH_RETRIES_PLACEHOLDER);
            return this;
        }

        public CometApiBuilder withApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public CometApiImpl build() {
            return new CometApiImpl(apiKey, baseUrl, maxAuthRetries);
        }
    }
}
