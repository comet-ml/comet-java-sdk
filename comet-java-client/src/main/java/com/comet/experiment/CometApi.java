package com.comet.experiment;

import com.comet.response.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.comet.experiment.Constants.*;

public class CometApi {
    public final static ObjectMapper objectMapper = new ObjectMapper();
    private final Connection connection;
    private Logger logger = LoggerFactory.getLogger(OnlineExperiment.class);
    private final int maxAuthRetries = 4;

    public CometApi(Config config, String restApiKey) {
        this.connection = new Connection(
                config.getString(Constants.COMET_URL),
                Optional.empty(),
                Optional.of(restApiKey),
                this.logger,
                this.maxAuthRetries);
    }

    public List<String> getAllWorkspaces() {
        Optional<String> body = connection.sendGet(WORKSPACES, Collections.emptyMap());
        if (!body.isPresent()) {
            return Collections.emptyList();
        }

        System.out.println(body.get());

        try {
            WorkspaceResponse workspaceResponse = objectMapper.readValue(body.get(), WorkspaceResponse.class);
            return workspaceResponse.getWorkspaces();
        } catch (IOException ex) {
            logger.debug("failed to parse workspace endpoint response", ex);
            return Collections.emptyList();
        }
    }

    public List<ProjectRest> getAllProjects(String workspace) {
        Optional<String> body = connection.sendGet(PROJECTS, Collections.singletonMap("workspace", workspace));
        if (!body.isPresent()) {
            return Collections.emptyList();
        }

        System.out.println(body.get());

        try {
            ProjectResponse projectResponse = objectMapper.readValue(body.get(), ProjectResponse.class);
            return projectResponse.getProjects();
        } catch (IOException ex) {
            System.out.println(ex);
            logger.debug("failed to parse project endpoint response", ex);
            return Collections.emptyList();
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

        System.out.println("body:");
        System.out.println(body.get());

        try {
            T decodedResponse = objectMapper.readValue(body.get(), clazz);
            return Optional.ofNullable(decodedResponse);
        } catch (IOException ex) {
            System.out.println(ex);
            logger.debug("failed to parse endpoint response", ex);
            return Optional.empty();
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
