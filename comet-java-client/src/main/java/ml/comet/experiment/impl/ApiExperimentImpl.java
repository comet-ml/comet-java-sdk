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
import static ml.comet.experiment.impl.config.CometConfig.COMET_PROJECT_NAME;
import static ml.comet.experiment.impl.config.CometConfig.COMET_TIMEOUT_CLEANING_SECONDS;
import static ml.comet.experiment.impl.config.CometConfig.COMET_WORKSPACE_NAME;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_CLEANUP_PROMPT;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

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
            final Duration cleaningTimeout,
            final String projectName,
            final String workspaceName) {
        super(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, projectName, workspaceName);
        if (logger != null) {
            this.logger = logger;
        }
    }

    @Override
    void init() {
        super.init();

        registerExperiment();
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
        if (StringUtils.isBlank(experimentKey)) {
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

    @Override
    public void end() {
        getLogger().info(getString(EXPERIMENT_CLEANUP_PROMPT, cleaningTimeout.getSeconds()));

        super.end();
    }

    @Override
    public void close() {
        this.end();
    }

    /**
     * Returns builder to create {@link Experiment} instance.
     *
     * @param experimentKey the unique identifier of the existing experiment.
     * @return the initialized {@link ApiExperimentBuilder} instance.
     */
    public static ApiExperimentImpl.ApiExperimentBuilderImpl builder(@NonNull final String experimentKey) {
        return builder().withExistingExperimentKey(experimentKey);
    }

    /**
     * Returns builder to create {@link Experiment} instance.
     *
     * @return the initialized {@link ApiExperimentBuilder} instance.
     */
    public static ApiExperimentImpl.ApiExperimentBuilderImpl builder() {
        return new ApiExperimentImpl.ApiExperimentBuilderImpl();
    }

    /**
     * The builder to create properly configured ApiExperiment instance.
     */
    public static final class ApiExperimentBuilderImpl implements ApiExperimentBuilder {
        private String experimentKey;
        private String apiKey;
        private Logger logger;
        private String projectName;
        private String workspace;

        private ApiExperimentBuilderImpl() {
        }

        @Override
        public ApiExperimentImpl.ApiExperimentBuilderImpl withExistingExperimentKey(@NonNull String experimentKey) {
            this.experimentKey = experimentKey;
            return this;
        }

        @Override
        public ApiExperimentImpl.ApiExperimentBuilderImpl withApiKey(@NonNull final String anApiKey) {
            this.apiKey = anApiKey;
            return this;
        }

        @Override
        public ApiExperimentImpl.ApiExperimentBuilderImpl withProjectName(@NonNull String projectName) {
            this.projectName = projectName;
            return this;
        }

        @Override
        public ApiExperimentImpl.ApiExperimentBuilderImpl withWorkspace(@NonNull String workspace) {
            this.workspace = workspace;
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
            if (StringUtils.isBlank(this.apiKey)) {
                this.apiKey = COMET_API_KEY.getString();
            }
            if (StringUtils.isBlank(this.projectName)) {
                this.projectName = COMET_PROJECT_NAME.getOptionalString().orElse(null);
            }
            if (StringUtils.isBlank(this.workspace)) {
                this.workspace = COMET_WORKSPACE_NAME.getOptionalString().orElse(null);
            }
            ApiExperimentImpl experiment = new ApiExperimentImpl(
                    this.apiKey, this.experimentKey, this.logger,
                    COMET_BASE_URL.getString(),
                    COMET_MAX_AUTH_RETRIES.getInt(),
                    COMET_TIMEOUT_CLEANING_SECONDS.getDuration(),
                    this.projectName, this.workspace);
            try {
                // initialize experiment
                experiment.init();
            } catch (Throwable ex) {
                // release hold resources and signal to user about failure
                experiment.end();
                throw ex;
            }
            return experiment;
        }
    }
}
