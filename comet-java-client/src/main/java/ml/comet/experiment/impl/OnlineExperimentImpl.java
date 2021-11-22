package ml.comet.experiment.impl;

import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.builder.OnlineExperimentBuilder;
import ml.comet.experiment.exception.ConfigException;
import ml.comet.experiment.impl.config.CometConfig;
import ml.comet.experiment.impl.log.StdOutLogger;
import ml.comet.experiment.model.ExperimentStatusResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_BASE_URL;
import static ml.comet.experiment.impl.config.CometConfig.COMET_MAX_AUTH_RETRIES;
import static ml.comet.experiment.impl.config.CometConfig.COMET_PROJECT_NAME;
import static ml.comet.experiment.impl.config.CometConfig.COMET_TIMEOUT_CLEANING_SECONDS;
import static ml.comet.experiment.impl.config.CometConfig.COMET_WORKSPACE_NAME;

/**
 * The implementation of the {@link OnlineExperiment} to work with Comet API asynchronously.
 */
public final class OnlineExperimentImpl extends BaseExperiment implements OnlineExperiment {
    private static final int SCHEDULED_EXECUTOR_TERMINATION_WAIT_SEC = 60;
    private static final int STD_OUT_LOGGER_FLUSH_WAIT_DELAY_MS = 2000;

    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    @Getter
    private Logger logger = LoggerFactory.getLogger(OnlineExperimentImpl.class);

    private StdOutLogger stdOutLogger;
    private StdOutLogger stdErrLogger;
    private boolean interceptStdout;
    private ScheduledFuture<?> heartbeatSendFuture;

    // The flag to indicate if experiment end() was called and experiment shutdown initialized
    private final AtomicBoolean atShutdown = new AtomicBoolean();

    /**
     * Default constructor which reads all configuration parameters of the experiment either from configuration file
     * or from environment variables.
     *
     * @throws ConfigException If the Comet API key, project name, and workspace are missing from the configuration
     *                         source or wrong types of the values defined.
     */
    public OnlineExperimentImpl() throws ConfigException {
        super(COMET_API_KEY.getString(),
                COMET_BASE_URL.getString(),
                COMET_MAX_AUTH_RETRIES.getInt(),
                StringUtils.EMPTY,
                COMET_TIMEOUT_CLEANING_SECONDS.getDuration(),
                COMET_PROJECT_NAME.getString(),
                COMET_WORKSPACE_NAME.getString()
        );
        this.init();
    }

    private OnlineExperimentImpl(
            String apiKey,
            String projectName,
            String workspaceName,
            String experimentName,
            String experimentKey,
            Logger logger,
            boolean interceptStdout,
            String baseUrl,
            int maxAuthRetries,
            Duration cleaningTimeout) {
        super(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, projectName, workspaceName);

        this.experimentName = experimentName;
        this.interceptStdout = interceptStdout;
        if (logger != null) {
            this.logger = logger;
        }
        this.init();
    }

    /**
     * Returns builder to be used to create properly configured instance of this class.
     *
     * @return the builder to be used to create properly configured instance of this class.
     */
    public static OnlineExperimentBuilderImpl builder() {
        return new OnlineExperimentBuilderImpl();
    }

    @Override
    public void end() {
        // set shutdown flag
        this.atShutdown.set(true);

        // stop pinging server
        if (heartbeatSendFuture != null) {
            if (!heartbeatSendFuture.cancel(true)) {
                this.logger.error("failed to stop experiment's heartbeat sender");
            } else {
                this.logger.info("Experiment's heartbeat sender stopped");
            }
            heartbeatSendFuture = null;
        }
        // release executor
        this.scheduledExecutorService.shutdownNow();
        try {
            if (!this.scheduledExecutorService.awaitTermination(
                    SCHEDULED_EXECUTOR_TERMINATION_WAIT_SEC, TimeUnit.SECONDS)) {
                this.logger.warn("scheduled executor failed to terminate");
            }
        } catch (InterruptedException e) {
            this.logger.error("scheduled executor's wait for termination was interrupted", e);
        }

        // stop intercepting stdout
        if (this.interceptStdout) {
            try {
                this.stopInterceptStdout();
            } catch (IOException e) {
                logger.error("failed to stop StdOut/StdErr intercepting", e);
            }
        }

        // invoke end of the superclass for common cleanup routines with given timeout
        super.end();
    }

    @Override
    public void setInterceptStdout() throws IOException {
        if (!interceptStdout) {
            interceptStdout = true;
            captureStdout();
        }
    }

    @Override
    public void stopInterceptStdout() throws IOException {
        if (this.stdOutLogger != null) {
            this.stopStdOutLogger(this.stdOutLogger, STD_OUT_LOGGER_FLUSH_WAIT_DELAY_MS);
            this.stdOutLogger = null;
            this.interceptStdout = false;
        }
        if (this.stdErrLogger != null) {
            this.stopStdOutLogger(this.stdErrLogger, 0);
            this.stdErrLogger = null;
        }
    }

    @Override
    public void nextStep() {
        this.step++;
    }

    @Override
    public void nextEpoch() {
        this.epoch++;
    }

    @Override
    public Optional<String> getExperimentLink() {
        return Optional.ofNullable(this.experimentLink);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue) {
        this.logMetric(metricName, metricValue, this.step, this.epoch);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, long step) {
        this.logMetric(metricName, metricValue, step, this.epoch);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, long step, long epoch) {
        this.setStep(step);
        this.setEpoch(epoch);
        this.logMetricAsync(metricName, metricValue, step, epoch, null);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue) {
        this.logParameter(parameterName, paramValue, this.step);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        this.setStep(step);
        this.logParameterAsync(parameterName, paramValue, step, null);
    }

    @Override
    public void logHtml(@NonNull String html, boolean override) {
        this.logHtmlAsync(html, override, null);
    }

    @Override
    public void logOther(@NonNull String key, @NonNull Object value) {
        this.logOtherAsync(key, value, null);
    }

    @Override
    public void addTag(@NonNull String tag) {
        this.addTagAsync(tag, null);
    }

    @Override
    public void logGraph(@NonNull String graph) {
        this.logGraphAsync(graph, null);
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        this.logStartTimeAsync(startTimeMillis, null);
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        this.logEndTimeAsync(endTimeMillis, null);
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite, long step) {
        super.uploadAsset(asset, fileName, overwrite, step, this.epoch);
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite) {
        uploadAsset(asset, asset.getName(), overwrite);
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite) {
        super.uploadAsset(asset, fileName, overwrite, this.step, this.epoch);
    }

    @Override
    void init() {
        super.init();

        setupStdOutIntercept();
        registerExperiment();

        this.heartbeatSendFuture = this.scheduledExecutorService.scheduleAtFixedRate(
                new OnlineExperimentImpl.HeartbeatPing(this), 1, 3, TimeUnit.SECONDS);
    }

    private void setupStdOutIntercept() {
        if (this.interceptStdout) {
            try {
                this.captureStdout();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void stopStdOutLogger(@NonNull StdOutLogger stdOutLogger, long delay) throws IOException {
        // flush first
        stdOutLogger.flush();

        // wait a bit for changes to propagate
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            this.logger.warn("interrupted while waiting for StdLogger to flush", e);
        }

        // close after that
        stdOutLogger.close();
    }

    // Internal OnlineExperiment Logic Methods
    private void captureStdout() throws IOException {
        stdOutLogger = StdOutLogger.createStdoutLogger(this);
        stdErrLogger = StdOutLogger.createStderrLogger(this);
    }

    private void sendHeartbeat() {
        if (!this.alive || this.atShutdown.get()) {
            return;
        }
        logger.debug("sendHeartbeat");
        Optional<ExperimentStatusResponse> status = this.sendExperimentStatus();
        if (status.isPresent()) {
            long interval = status.get().getIsAliveBeatDurationMillis();
            if (logger.isDebugEnabled()) {
                logger.debug("received heartbeat interval {}", interval);
            }
            // TODO: implement logic to change heartbeat interval
        }
    }

    /**
     * The runnable to be invoked to send periodic heartbeat ping to mark this experiment as still running.
     */
    static class HeartbeatPing implements Runnable {
        OnlineExperimentImpl onlineExperiment;

        HeartbeatPing(OnlineExperimentImpl onlineExperiment) {
            this.onlineExperiment = onlineExperiment;
        }

        @Override
        public void run() {
            onlineExperiment.sendHeartbeat();
        }
    }

    /**
     * The builder to create properly configured instance of the OnlineExperimentImpl.
     */
    public static final class OnlineExperimentBuilderImpl implements OnlineExperimentBuilder {
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
        private OnlineExperimentBuilderImpl() {
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

            if (StringUtils.isEmpty(this.apiKey)) {
                this.apiKey = COMET_API_KEY.getString();
            }
            if (StringUtils.isEmpty(this.projectName)) {
                this.projectName = COMET_PROJECT_NAME.getOptionalString().orElse(null);
            }
            if (StringUtils.isEmpty(this.workspace)) {
                this.workspace = COMET_WORKSPACE_NAME.getOptionalString().orElse(null);
            }
            if (StringUtils.isEmpty(this.baseUrl)) {
                this.baseUrl = COMET_BASE_URL.getString();
            }
            if (this.maxAuthRetries == -1) {
                this.maxAuthRetries = COMET_MAX_AUTH_RETRIES.getInt();
            }
            Duration cleaningTimeout = COMET_TIMEOUT_CLEANING_SECONDS.getDuration();

            return new OnlineExperimentImpl(
                    this.apiKey, this.projectName, this.workspace, this.experimentName, this.experimentKey,
                    this.logger, this.interceptStdout, this.baseUrl, this.maxAuthRetries, cleaningTimeout);
        }
    }
}
