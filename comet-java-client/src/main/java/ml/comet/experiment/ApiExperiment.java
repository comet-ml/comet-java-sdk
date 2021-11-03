package ml.comet.experiment;

import com.typesafe.config.Config;
import ml.comet.experiment.builder.ApiExperimentBuilder;
import ml.comet.experiment.http.Connection;
import ml.comet.experiment.http.ConnectionInitializer;
import ml.comet.experiment.utils.ConfigUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import static ml.comet.experiment.constants.Constants.*;

/**
 * Implementation of the Experiment which provides methods to access meta-data of particular experiment through the
 * REST API exposed by the Comet.ml server.
 * <p>
 * The ApiExperiment should be used to directly read experiment data from the Comet.ml server.
 */
public class ApiExperiment extends BaseExperiment {
    private final String baseUrl;
    private final String experimentKey;
    private final Connection connection;
    private Logger logger = LoggerFactory.getLogger(ApiExperiment.class);

    private ApiExperiment(
            String apiKey,
            String experimentKey,
            Logger logger,
            String baseUrl,
            int maxAuthRetries) {
        this.experimentKey = experimentKey;
        if (logger != null) {
            this.logger = logger;
        }
        this.baseUrl = baseUrl;
        this.connection = ConnectionInitializer.initConnection(apiKey, this.baseUrl, maxAuthRetries, this.logger);
    }

    public static ApiExperiment.ApiExperimentBuilderImpl builder(String experimentKey) {
        return new ApiExperiment.ApiExperimentBuilderImpl(experimentKey);
    }

    public static class ApiExperimentBuilderImpl implements ApiExperimentBuilder {
        private final String experimentKey;
        private String apiKey;
        private Logger logger;
        private String baseUrl;
        private int maxAuthRetries;

        private ApiExperimentBuilderImpl(String experimentKey) {
            this.experimentKey = experimentKey;
            this.apiKey = ConfigUtils.getApiKey().orElse(null);
            this.baseUrl = ConfigUtils.getBaseUrlOrDefault();
            this.maxAuthRetries = ConfigUtils.getMaxAuthRetriesOrDefault();
        }

        @Override
        public ApiExperiment.ApiExperimentBuilderImpl withApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        @Override
        public ApiExperiment.ApiExperimentBuilderImpl withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        @Override
        public ApiExperiment.ApiExperimentBuilderImpl withConfig(File overrideConfig) {
            Config config = ConfigUtils.getConfigFromFile(overrideConfig);
            this.apiKey = config.getString(COMET_API_KEY);
            this.baseUrl = config.getString(BASE_URL_PLACEHOLDER);
            this.maxAuthRetries = config.getInt(MAX_AUTH_RETRIES_PLACEHOLDER);
            return this;
        }

        @Override
        public ApiExperiment build() {
            return new ApiExperiment(apiKey, experimentKey, logger, baseUrl, maxAuthRetries);
        }
    }

    @Override
    protected Connection getConnection() {
        return this.connection;
    }

    @Override
    protected Logger getLogger() {
        return this.logger;
    }

    @Override
    public String getContext() {
        return StringUtils.EMPTY;
    }

    @Override
    public String getWorkspaceName() {
        return getMetadata().getWorkspaceName();
    }

    @Override
    public String getProjectName() {
        return getMetadata().getProjectName();
    }

    @Override
    public String getExperimentName() {
        return getMetadata().getExperimentName();
    }

    @Override
    public String getExperimentKey() {
        return this.experimentKey;
    }

    @Override
    public Optional<String> getExperimentLink() {
        if (StringUtils.isEmpty(experimentKey)) {
            return Optional.empty();
        }
        String url = String.format("%s/%s/%s/%s", baseUrl, getWorkspaceName(), getProjectName(), experimentKey);
        try {
            // check URI syntax and return
            URI uri = URI.create(url);
            return Optional.of(uri.toString());
        } catch (Exception ex) {
            this.logger.error("failed to build experiment link", ex);
            return Optional.empty();
        }
    }

    @Override
    public void end() {
        // close connection
        if (this.connection != null) {
            try {
                this.connection.close();
            } catch (IOException e) {
                this.logger.error("failed to close connection", e);
            }
        }
    }
}
