package ml.comet.experiment.impl;

import lombok.NonNull;
import ml.comet.experiment.builder.OnlineExperimentBuilder;
import ml.comet.experiment.impl.config.CometConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.time.Duration;

import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_BASE_URL;
import static ml.comet.experiment.impl.config.CometConfig.COMET_EXPERIMENT_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_MAX_AUTH_RETRIES;
import static ml.comet.experiment.impl.config.CometConfig.COMET_PROJECT_NAME;
import static ml.comet.experiment.impl.config.CometConfig.COMET_TIMEOUT_CLEANING_SECONDS;
import static ml.comet.experiment.impl.config.CometConfig.COMET_WORKSPACE_NAME;

/**
 * The builder to create properly configured instance of the OnlineExperimentImpl.
 */
final class OnlineExperimentBuilderImpl implements OnlineExperimentBuilder {
    private String projectName;
    private String workspace;
    private String apiKey;
    private String baseUrl;
    private int maxAuthRetries = -1;
    private String experimentName;
    private String experimentKey;
    private Logger logger;
    private boolean interceptStdout = false;

    /**
     * Default constructor to avoid direct initialization from the outside.
     */
    OnlineExperimentBuilderImpl() {
    }

    @Override
    public OnlineExperimentBuilderImpl withProjectName(@NonNull String projectName) {
        this.projectName = projectName;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withWorkspace(@NonNull String workspace) {
        this.workspace = workspace;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withApiKey(@NonNull String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withMaxAuthRetries(int maxAuthRetries) {
        this.maxAuthRetries = maxAuthRetries;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withUrlOverride(@NonNull String urlOverride) {
        this.baseUrl = urlOverride;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withExperimentName(@NonNull String experimentName) {
        this.experimentName = experimentName;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withExistingExperimentKey(@NonNull String experimentKey) {
        this.experimentKey = experimentKey;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withLogger(@NonNull Logger logger) {
        this.logger = logger;
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl withConfigOverride(@NonNull File overrideConfig) {
        CometConfig.applyConfigOverride(overrideConfig);
        return this;
    }

    @Override
    public OnlineExperimentBuilderImpl interceptStdout() {
        this.interceptStdout = true;
        return this;
    }

    @Override
    public OnlineExperimentImpl build() {

        if (StringUtils.isBlank(this.apiKey)) {
            this.apiKey = COMET_API_KEY.getString();
        }
        if (StringUtils.isBlank(this.projectName)) {
            this.projectName = COMET_PROJECT_NAME.getOptionalString().orElse(null);
        }
        if (StringUtils.isBlank(this.workspace)) {
            this.workspace = COMET_WORKSPACE_NAME.getOptionalString().orElse(null);
        }
        if (StringUtils.isBlank(this.baseUrl)) {
            this.baseUrl = COMET_BASE_URL.getString();
        }
        if (StringUtils.isBlank(this.experimentKey)) {
            this.experimentKey = COMET_EXPERIMENT_KEY.getOptionalString().orElse(null);
        }
        if (this.maxAuthRetries == -1) {
            this.maxAuthRetries = COMET_MAX_AUTH_RETRIES.getInt();
        }
        Duration cleaningTimeout = COMET_TIMEOUT_CLEANING_SECONDS.getDuration();

        OnlineExperimentImpl experiment = new OnlineExperimentImpl(
                this.apiKey, this.projectName, this.workspace, this.experimentName, this.experimentKey,
                this.logger, this.interceptStdout, this.baseUrl, this.maxAuthRetries, cleaningTimeout);
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
