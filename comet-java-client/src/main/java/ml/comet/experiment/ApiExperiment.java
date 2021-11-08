package ml.comet.experiment;

import com.typesafe.config.Config;
import lombok.NonNull;
import ml.comet.experiment.builder.ApiExperimentBuilder;
import ml.comet.experiment.http.Connection;
import ml.comet.experiment.http.ConnectionInitializer;
import ml.comet.experiment.utils.ConfigUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.Optional;

import static ml.comet.experiment.constants.Constants.BASE_URL_PLACEHOLDER;
import static ml.comet.experiment.constants.Constants.COMET_API_KEY;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_PLACEHOLDER;

/**
 * Implementation of the Experiment which provides methods to access meta-data of particular
 * experiment through the REST API exposed by the Comet.ml server.
 *
 * <p>The ApiExperiment should be used to directly read experiment data from the Comet.ml server.
 */
public final class ApiExperiment extends BaseExperiment {
    /**
     * The base URL of the experiment.
     */
    private final String baseUrl;
    /**
     * The unique key of the experiment.
     */
    private final String experimentKey;
    /**
     * The connection to the Comet server.
     */
    private final Connection connection;
    /**
     * The logger instance.
     */
    private Logger logger = LoggerFactory.getLogger(ApiExperiment.class);

    private ApiExperiment(
            final String apiKey,
            final String anExperimentKey,
            final Logger logger,
            final String baseUrl,
            final int maxAuthRetries) {
        this.experimentKey = anExperimentKey;
        if (logger != null) {
            this.logger = logger;
        }
        this.baseUrl = baseUrl;
        this.connection = ConnectionInitializer.initConnection(
                apiKey, this.baseUrl, maxAuthRetries, this.logger);
    }

    /**
     * Returns builder to create ApiExperiment instance.
     *
     * @param experimentKey the unique identifier of the existing experiment.
     * @return the initialized ApiExperiment instance.
     */
    public static ApiExperiment.ApiExperimentBuilderImpl builder(@NonNull final String experimentKey) {
        return new ApiExperiment.ApiExperimentBuilderImpl(experimentKey);
    }

    /**
     * The builder to create properly configured ApiExperiment instance.
     */
    public static final class ApiExperimentBuilderImpl implements ApiExperimentBuilder {
        /**
         * The unique key of the experiment.
         */
        private final String experimentKey;
        /**
         * The Comet API key to get access to the server.
         */
        private String apiKey;
        /**
         * The logger instance.
         */
        private Logger logger;
        /**
         * The base URL of the experiment.
         */
        private String baseUrl;
        /**
         * The maximal number of authentication retries against Comet server.
         */
        private int maxAuthRetries;

        private ApiExperimentBuilderImpl(final String anExperimentKey) {
            this.experimentKey = anExperimentKey;
            this.apiKey = ConfigUtils.getApiKey().orElse(null);
            this.baseUrl = ConfigUtils.getBaseUrlOrDefault();
            this.maxAuthRetries = ConfigUtils.getMaxAuthRetriesOrDefault();
        }

        @Override
        public ApiExperiment.ApiExperimentBuilderImpl withApiKey(@NonNull final String anApiKey) {
            this.apiKey = anApiKey;
            return this;
        }

        @Override
        public ApiExperiment.ApiExperimentBuilderImpl withLogger(@NonNull final Logger logger) {
            this.logger = logger;
            return this;
        }

        @Override
        public ApiExperiment.ApiExperimentBuilderImpl withConfig(@NonNull final File overrideConfig) {
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
        String url = String.format("%s/%s/%s/%s",
                baseUrl, getWorkspaceName(), getProjectName(), experimentKey);
        try {
            // check URI syntax and return
            URI uri = URI.create(url);
            return Optional.of(uri.toString());
        } catch (Exception ex) {
            this.logger.error("failed to build experiment link", ex);
            return Optional.empty();
        }
    }
}
