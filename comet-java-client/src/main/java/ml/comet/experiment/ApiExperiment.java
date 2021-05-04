package ml.comet.experiment;

import com.typesafe.config.Config;
import ml.comet.experiment.http.Connection;
import ml.comet.experiment.builder.ApiExperimentBuilder;
import ml.comet.experiment.constants.Constants;
import ml.comet.experiment.http.ConnectionInitializer;
import ml.comet.experiment.utils.ConfigUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

import static ml.comet.experiment.constants.Constants.COMET_API_KEY;

public class ApiExperiment extends BaseExperiment {
    private final Config config;
    private final String apiKey;
    private final String experimentKey;
    private Logger logger = LoggerFactory.getLogger(ApiExperiment.class);
    private Connection connection;

    private ApiExperiment(
            String apiKey,
            String experimentKey,
            Logger logger,
            Config config) {
        this.config = config;
        this.apiKey = apiKey;
        this.experimentKey = experimentKey;
        if (logger != null) {
            this.logger = logger;
        }
        this.connection = ConnectionInitializer.initConnection(this.config, this.apiKey, this.logger);
    }

    public ApiExperiment(String experimentKey) {
        this.experimentKey = experimentKey;
        this.config = ConfigUtils.getDefaultConfigFromClassPath();
        this.apiKey = config.getString(COMET_API_KEY);
        this.connection = ConnectionInitializer.initConnection(this.config, this.apiKey, this.logger);
    }

    public static ApiExperiment.ApiExperimentBuilderImpl builder(String experimentKey) {
        return new ApiExperiment.ApiExperimentBuilderImpl(experimentKey);
    }

    public static class ApiExperimentBuilderImpl implements ApiExperimentBuilder {
        private final String experimentKey;
        private String apiKey;
        private Logger logger;
        private Config config;

        private ApiExperimentBuilderImpl(String experimentKey) {
            this.config = ConfigUtils.getDefaultConfigFromClassPath();
            this.apiKey = config.getString(COMET_API_KEY);
            this.experimentKey = experimentKey;
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
            this.config = ConfigUtils.getConfigFromFile(overrideConfig)
                    .withFallback(this.config)
                    .resolve();
            return this;
        }

        @Override
        public ApiExperiment build() {
            return new ApiExperiment(apiKey, experimentKey, logger, config);
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
        String link = config.getString(Constants.BASE_URL_PLACEHOLDER)
                + "/" + getWorkspaceName() + "/" + getProjectName() + "/" + experimentKey;
        return Optional.of(link);
    }

    private void setupConnection() {
        this.connection = ConnectionInitializer.initConnection(this.config, this.apiKey, this.logger);
    }

}
