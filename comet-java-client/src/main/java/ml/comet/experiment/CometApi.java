package ml.comet.experiment;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import ml.comet.response.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ml.comet.experiment.Constants.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CometApi {
    public final static ObjectMapper objectMapper = new ObjectMapper();
    private final Connection connection;
    private Logger logger = LoggerFactory.getLogger(OnlineExperiment.class);

    public CometApi(Config config, String restApiKey) {
        this.connection = new Connection(
                config.getString(Constants.COMET_URL),
                Optional.empty(),
                Optional.of(restApiKey),
                this.logger);
    }

    public List<String> getAllWorkspaces() {
        Optional<String> body = connection.sendGet(WORKSPACES, Collections.emptyMap());
        if (!body.isPresent()) {
            return Collections.emptyList();
        }

        try {
            WorkspaceResponse workspaceResponse = objectMapper.readValue(body.get(), WorkspaceResponse.class);
            return workspaceResponse.getWorkspaces();
        } catch (IOException ex) {
            logger.error("failed to parse workspace endpoint response", ex);
            throw new RuntimeException("failed to parse workspace endpoint response", ex);
        }
    }

    public List<ProjectRest> getAllProjects(String workspace) {
        Optional<String> body = connection.sendGet(PROJECTS, Collections.singletonMap("workspace", workspace));
        if (!body.isPresent()) {
            return Collections.emptyList();
        }

        try {
            ProjectResponse projectResponse = objectMapper.readValue(body.get(), ProjectResponse.class);
            return projectResponse.getProjects();
        } catch (IOException ex) {
            logger.error("failed to parse project endpoint response", ex);
            throw new RuntimeException("failed to parse project endpoint response", ex);
        }
    }

    public List<ExperimentRest> getAllExperiments(String projectId) {
        Optional<ExperimentResponse> response = getObjectForProject(projectId, EXPERIMENTS, ExperimentResponse.class);
        if (!response.isPresent()) {
            return Collections.emptyList();
        } else {
            return response.get().getExperiments();
        }
    }

    public Optional<GitMetadata> getGitMetadata(String experimentKey) {
        return getObjectForExperiment(experimentKey, GIT_METADATA, GitMetadata.class);
    }

    public Optional<String> getHtml(String experimentKey) {
        Optional<HtmlResponse> response = getObjectForExperiment(experimentKey, GET_HTML, HtmlResponse.class);
        return response.map(HtmlResponse::getHtml);
    }

    public Optional<String> getCode(String experimentKey) {
        Optional<CodeResponse> response = getObjectForExperiment(experimentKey, GET_CODE, CodeResponse.class);
        return response.map(CodeResponse::getCode);
    }

    public Optional<String> getOutput(String experimentKey) {
        Optional<OutputResponse> response = getObjectForExperiment(experimentKey, GET_OUTPUT, OutputResponse.class);
        return response.map(OutputResponse::getOutput);
    }

    public Optional<String> getGraph(String experimentKey) {
        Optional<GraphResponse> response = getObjectForExperiment(experimentKey, GET_GRAPH, GraphResponse.class);
        return response.map(GraphResponse::getGraph);
    }

    public Optional<ParametersResponse> getParameters(String experimentKey) {
        return getObjectForExperiment(experimentKey, GET_PARAMETERS, ParametersResponse.class);
    }

    public Optional<MetricsResponse> getMetrics(String experimentKey) {
        return getObjectForExperiment(experimentKey, GET_METRICS, MetricsResponse.class);
    }

    public Optional<LogOtherResponse> getLogOther(String experimentKey) {
        return getObjectForExperiment(experimentKey, GET_LOG_OTHER, LogOtherResponse.class);
    }

    public Optional<TagsResponse> getTags(String experimentKey) {
        return getObjectForExperiment(experimentKey, GET_TAGS, TagsResponse.class);
    }

    public Optional<AssetListResponse> getAssetList(String experimentKey, String type) {
        Optional<List<AssetInfo>> assets =
                getObject(
                        GET_ASSET_INFO,
                        new HashMap<String, String>() {{
                            put("experimentKey", experimentKey);
                            put("type", type);
                        }},
                        new TypeReference<List<AssetInfo>>(){{}});
        return assets.map(x -> new AssetListResponse(x));
    }

    public <T> Optional<T> getObjectForExperiment(String experimentKey, String endpoint, Class<T> clazz) {
        return getObject(endpoint, Collections.singletonMap("experimentKey", experimentKey), clazz);
    }

    public <T> Optional<T> getObjectForProject(String experimentKey, String endpoint, Class<T> clazz) {
        return getObject(endpoint, Collections.singletonMap("projectId", experimentKey), clazz);
    }

    public <T> Optional<T> getObject(String endpoint, Map<String, String> parameters, Class<T> clazz) {
        Optional<String> body = connection.sendGet(endpoint, parameters);
        if (!body.isPresent()) {
            return Optional.empty();
        }

        try {
            T decodedResponse = objectMapper.readValue(body.get(), clazz);
            return Optional.ofNullable(decodedResponse);
        } catch (IOException ex) {
            logger.error("failed to parse endpoint response", ex);
            throw new RuntimeException("failed to parse endpoint response", ex);
        }
    }

    public <T> Optional<T> getObject(String endpoint, Map<String, String> parameters, TypeReference<T> typeReference) {
        Optional<String> body = connection.sendGet(endpoint, parameters);
        if (!body.isPresent()) {
            return Optional.empty();
        }

        try {
            T decodedResponse = objectMapper.readValue(body.get(), typeReference);
            return Optional.ofNullable(decodedResponse);
        } catch (IOException ex) {
            logger.error("failed to parse endpoint response", ex);
            throw new RuntimeException("failed to parse endpoint response", ex);
        }
    }

    public static CometApiBuilder builder(String restApiKey) {
        return new CometApiBuilder(restApiKey);
    }

    public static class CometApiBuilder {
        private Config config;
        private String restApiKey;

        public CometApiBuilder(String restApiKey) {
            this.restApiKey = restApiKey;
            this.config = ConfigFactory.parseFile(
                    new File(getClass().getClassLoader().getResource(Constants.DEFAULTS_CONF).getFile()));
        }

        public CometApiBuilder withConfig(File overrideConfig) {
            this.config = ConfigFactory.parseFile(overrideConfig)
                    .withFallback(this.config)
                    .resolve();
            return this;
        }

        public CometApi build() {
            return new CometApi(config, restApiKey);
        }
    }
}
