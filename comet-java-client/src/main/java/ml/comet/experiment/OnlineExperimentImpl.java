package ml.comet.experiment;

import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.builder.OnlineExperimentBuilder;
import ml.comet.experiment.constants.Constants;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.http.Connection;
import ml.comet.experiment.http.ConnectionInitializer;
import ml.comet.experiment.log.StdOutLogger;
import ml.comet.experiment.model.CreateExperimentRequest;
import ml.comet.experiment.model.CreateExperimentResponse;
import ml.comet.experiment.model.OutputLine;
import ml.comet.experiment.model.OutputUpdate;
import ml.comet.experiment.utils.CometUtils;
import ml.comet.experiment.utils.ConfigUtils;
import ml.comet.experiment.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static ml.comet.experiment.constants.Constants.ADD_OUTPUT;
import static ml.comet.experiment.constants.Constants.EXPERIMENT_KEY;

/**
 * The implementation of the OnlineExperiment.
 */
@Getter
public class OnlineExperimentImpl extends BaseExperiment implements OnlineExperiment {
    private static final int SCHEDULED_EXECUTOR_TERMINATION_WAIT_SEC = 60;
    private static final int STD_OUT_LOGGER_FLUSH_WAIT_DELAY_MS = 2000;

    private final ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private final String projectName;
    private final String workspaceName;
    private final String apiKey;
    private final String baseUrl;
    private final int maxAuthRetries;

    private Logger logger = LoggerFactory.getLogger(OnlineExperimentImpl.class);
    private Connection connection;
    private String experimentKey;
    private String experimentLink;
    private String experimentName;
    private StdOutLogger stdOutLogger;
    private StdOutLogger stdErrLogger;
    private boolean interceptStdout;
    private ScheduledFuture<?> heartbeatSendFuture;

    private long step = 0;
    private long epoch = 0;
    private String context = "";

    // The flag to indicate if experiment end() was called and experiment shutdown initialized
    private final AtomicBoolean atShutdown = new AtomicBoolean();

    private OnlineExperimentImpl(
            String apiKey,
            String projectName,
            String workspaceName,
            String experimentName,
            String experimentKey,
            Logger logger,
            boolean interceptStdout,
            String baseUrl,
            int maxAuthRetries) {
        this.projectName = projectName;
        this.workspaceName = workspaceName;
        this.apiKey = apiKey;
        this.experimentName = experimentName;
        this.experimentKey = experimentKey;
        this.interceptStdout = interceptStdout;
        if (logger != null) {
            this.logger = logger;
        }
        this.baseUrl = baseUrl;
        this.maxAuthRetries = maxAuthRetries;
        this.initializeExperiment();
    }

    /**
     * Default constructor which reads all configuration parameters of the experiment either from configuration file
     * or environment variables.
     */
    public OnlineExperimentImpl() {
        this.projectName = ConfigUtils.getProjectNameOrThrow();
        this.workspaceName = ConfigUtils.getWorkspaceNameOrThrow();
        this.apiKey = ConfigUtils.getApiKeyOrThrow();
        this.baseUrl = ConfigUtils.getBaseUrlOrDefault();
        this.maxAuthRetries = ConfigUtils.getMaxAuthRetriesOrDefault();
        this.initializeExperiment();
    }

    public String getExperimentName() {
        return experimentName;
    }

    /**
     * Returns builder to be used to create properly configured instance of this class.
     *
     * @return the builder to be used to create properly configured instance of this class.
     */
    public static OnlineExperimentBuilderImpl builder() {
        return new OnlineExperimentBuilderImpl();
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
        public OnlineExperimentBuilderImpl withConfig(@NonNull File overrideConfig) {
            ConfigUtils.setOverrideConfig(overrideConfig);
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl interceptStdout() {
            this.interceptStdout = true;
            return this;
        }

        @Override
        public OnlineExperimentImpl build() {

            if (StringUtils.isEmpty(apiKey)) {
                this.apiKey = ConfigUtils.getApiKey().orElse(null);
            }
            if (StringUtils.isEmpty(projectName)) {
                projectName = ConfigUtils.getProjectName().orElse(null);
            }
            if (StringUtils.isEmpty(workspace)) {
                workspace = ConfigUtils.getWorkspaceName().orElse(null);
            }
            if (StringUtils.isEmpty(baseUrl)) {
                baseUrl = ConfigUtils.getBaseUrlOrDefault();
            }
            if (maxAuthRetries == -1) {
                maxAuthRetries = ConfigUtils.getMaxAuthRetriesOrDefault();
            }

            return new OnlineExperimentImpl(apiKey, projectName, workspace, experimentName, experimentKey,
                    logger, interceptStdout, baseUrl, maxAuthRetries);
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

        // invoke end of the superclass for common cleanup routines
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

    private void stopStdOutLogger(@NonNull StdOutLogger stdOutLogger, long delay) throws IOException {
        // flush first
        stdOutLogger.flush();

        // wait a bit for changes to propagate
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            this.logger.warn("interrupted while waiting for stdlogger to flush", e);
        }

        // close after that
        stdOutLogger.close();
    }

    @Override
    public void logLine(String line, long offset, boolean stderr) {
        if (getExperimentKey() == null) {
            return;
        }
        OutputUpdate outputUpdate = getLogLineRequest(line, offset, stderr);
        getConnection().sendPostAsync(outputUpdate, ADD_OUTPUT);
    }

    @Override
    public void setStep(long step) {
        this.step = step;
    }

    @Override
    public void nextStep() {
        this.step++;
    }

    @Override
    public long getStep() {
        return this.step;
    }

    @Override
    public void setEpoch(long epoch) {
        this.epoch = epoch;
    }

    @Override
    public void nextEpoch() {
        this.epoch++;
    }

    @Override
    public long getEpoch() {
        return this.epoch;
    }

    @Override
    public void setContext(String context) {
        this.context = context;
    }

    @Override
    public String getContext() {
        return this.context;
    }

    @Override
    public String getExperimentKey() {
        return this.experimentKey;
    }

    @Override
    public Optional<String> getExperimentLink() {
        return Optional.ofNullable(this.experimentLink);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, long step) {
        logMetric(metricName, metricValue, step, this.epoch);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue) {
        logMetric(metricName, metricValue, this.step, this.epoch);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, long step, long epoch) {
        this.setStep(step);
        this.setEpoch(epoch);
        super.logMetric(metricName, metricValue, step, epoch);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue) {
        logParameter(parameterName, paramValue, this.step);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        this.setStep(step);
        super.logParameter(parameterName, paramValue, step);
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

    private void initializeExperiment() {
        CometUtils.printCometSdkVersion();
        validateInitialParams();
        this.connection = ConnectionInitializer.initConnection(this.apiKey, this.baseUrl,
                this.maxAuthRetries, this.logger);
        setupStdOutIntercept();
        registerExperiment();
    }

    private void validateInitialParams() {
        if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("Apikey is not specified!");
        }
        if (StringUtils.isNotEmpty(experimentKey)) {
            return;
        }
        if (StringUtils.isEmpty(projectName)) {
            throw new IllegalArgumentException("ProjectName is not specified!");
        }
        if (StringUtils.isEmpty(workspaceName)) {
            throw new IllegalArgumentException("Workspace name is not specified!");
        }
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

    private void registerExperiment() {
        if (experimentKey != null) {
            logger.debug("Not registering a new experiment.  Using experiment key {}", experimentKey);
            return;
        }

        CreateExperimentRequest request = new CreateExperimentRequest(workspaceName, projectName, experimentName);
        String body = JsonUtils.toJson(request);

        connection.sendPost(body, Constants.NEW_EXPERIMENT, true)
                .ifPresent(response -> {
                    CreateExperimentResponse result = JsonUtils.fromJson(response, CreateExperimentResponse.class);
                    this.experimentKey = result.getExperimentKey();
                    this.experimentLink = result.getLink();
                    this.logger.info("Experiment is live on comet.ml " + getExperimentUrl());
                    this.heartbeatSendFuture = this.scheduledExecutorService.scheduleAtFixedRate(
                            new HeartbeatPing(this), 1, 3, TimeUnit.SECONDS);
                });

        if (this.experimentKey == null) {
            throw new CometGeneralException("Failed to register onlineExperiment with Comet ML");
        }
    }

    private String getExperimentUrl() {
        return experimentLink != null ? experimentLink : StringUtils.EMPTY;
    }

    // Internal OnlineExperiment Logic Methods
    private void captureStdout() throws IOException {
        stdOutLogger = StdOutLogger.createStdoutLogger(this);
        stdErrLogger = StdOutLogger.createStderrLogger(this);
    }

    protected void sendHeartbeat() {
        if (experimentKey == null || this.atShutdown.get()) {
            return;
        }
        logger.debug("sendHeartbeat");
        connection.sendGet(Constants.EXPERIMENT_STATUS, Collections.singletonMap(EXPERIMENT_KEY, experimentKey));
    }

    private OutputUpdate getLogLineRequest(@NonNull String line, long offset, boolean stderr) {
        OutputLine outputLine = new OutputLine();
        outputLine.setOutput(line);
        outputLine.setStderr(stderr);
        outputLine.setLocalTimestamp(System.currentTimeMillis());
        outputLine.setOffset(offset);

        OutputUpdate outputUpdate = new OutputUpdate();
        outputUpdate.setExperimentKey(getExperimentKey());
        outputUpdate.setRunContext(this.context);
        outputUpdate.setOutputLines(Collections.singletonList(outputLine));
        return outputUpdate;
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


}
