package ml.comet.experiment.impl;

import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.Experiment;
import ml.comet.experiment.builder.ApiExperimentBuilder;
import ml.comet.experiment.impl.config.CometConfig;
import ml.comet.experiment.impl.utils.CometUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Duration;
import java.util.Optional;

import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_BASE_URL;
import static ml.comet.experiment.impl.config.CometConfig.COMET_MAX_AUTH_RETRIES;
import static ml.comet.experiment.impl.config.CometConfig.COMET_TIMEOUT_CLEANING_SECONDS;

/**
 * Implementation of the {@link Experiment} that allows to read/update existing experiment synchronously.
 */
public final class ApiExperimentImpl extends BaseExperiment implements ApiExperiment {
    @Getter
    private Logger logger = LoggerFactory.getLogger(ApiExperimentImpl.class);

    private ApiExperimentImpl(
            final String apiKey,
            final String experimentKey,
            final Logger logger,
            final String baseUrl,
            int maxAuthRetries,
            final Duration cleaningTimeout) {
        super(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout);
        if (logger != null) {
            this.logger = logger;
        }

        // initialize this experiment
        this.init();
    }

    @Override
    void init() {
        super.init();
    }

    /**
     * Returns builder to create ApiExperiment instance.
     *
     * @param experimentKey the unique identifier of the existing experiment.
     * @return the initialized ApiExperiment instance.
     */
    public static ApiExperimentImpl.ApiExperimentBuilderImpl builder(@NonNull final String experimentKey) {
        return new ApiExperimentImpl.ApiExperimentBuilderImpl(experimentKey);
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
    public Optional<String> getExperimentLink() {
        if (StringUtils.isEmpty(experimentKey)) {
            return Optional.empty();
        }
        try {
            return Optional.of(CometUtils.createExperimentLink(
                    baseUrl, getWorkspaceName(), getProjectName(), experimentKey));
        } catch (Exception ex) {
            this.logger.error("failed to build experiment link", ex);
            return Optional.empty();
        }
    }

    /**
     * The builder to create properly configured ApiExperiment instance.
     */
    public static final class ApiExperimentBuilderImpl implements ApiExperimentBuilder {
        private final String experimentKey;
        private String apiKey;
        private Logger logger;

        private ApiExperimentBuilderImpl(final String anExperimentKey) {
            this.experimentKey = anExperimentKey;
        }

        @Override
        public ApiExperimentImpl.ApiExperimentBuilderImpl withApiKey(@NonNull final String anApiKey) {
            this.apiKey = anApiKey;
            return this;
        }

        @Override
        public ApiExperimentImpl.ApiExperimentBuilderImpl withLogger(@NonNull final Logger logger) {
            this.logger = logger;
            return this;
        }

        @Override
        public ApiExperimentImpl.ApiExperimentBuilderImpl withConfigOverride(@NonNull final File overrideConfig) {
            CometConfig.applyConfigOverride(overrideConfig);
            return this;
        }

        @Override
        public ApiExperiment build() {
            if (StringUtils.isEmpty(this.apiKey)) {
                this.apiKey = COMET_API_KEY.getString();
            }
            return new ApiExperimentImpl(
                    this.apiKey, this.experimentKey, this.logger,
                    COMET_BASE_URL.getString(),
                    COMET_MAX_AUTH_RETRIES.getInt(),
                    COMET_TIMEOUT_CLEANING_SECONDS.getDuration());
        }
    }
}
