package ml.comet.experiment.impl;

import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.impl.log.StdOutLogger;
import ml.comet.experiment.model.ExperimentStatusResponse;
import ml.comet.experiment.model.GitMetadata;
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

import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_HEARTBEAT_STOPPED_PROMPT;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

/**
 * The implementation of the {@link OnlineExperiment} to work with Comet API asynchronously.
 */
public final class OnlineExperimentImpl extends BaseExperimentAsync implements OnlineExperiment {
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
     * Creates new instance with given parameters.
     *
     * @param apiKey          the Comet API key.
     * @param projectName     the project name (optional).
     * @param workspaceName   the workspace name (optional).
     * @param experimentName  the experiment name (optional).
     * @param experimentKey   the experiment key to continue existing experiment (optional).
     * @param logger          the logger to be used instead (optional).
     * @param interceptStdout the flag to indicate if StdOut should be intercepted.
     * @param baseUrl         the base URL of the Comet backend.
     * @param maxAuthRetries  the maximal number of authentication retries.
     * @param cleaningTimeout the cleaning timeout after experiment end.
     * @throws IllegalArgumentException if illegal argument is provided or mandatory argument is missing.
     */
    OnlineExperimentImpl(
            String apiKey,
            String projectName,
            String workspaceName,
            String experimentName,
            String experimentKey,
            Logger logger,
            boolean interceptStdout,
            String baseUrl,
            int maxAuthRetries,
            Duration cleaningTimeout) throws IllegalArgumentException {
        super(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, projectName, workspaceName);

        this.experimentName = experimentName;
        this.interceptStdout = interceptStdout;
        if (logger != null) {
            this.logger = logger;
        }
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
                this.logger.info(getString(EXPERIMENT_HEARTBEAT_STOPPED_PROMPT));
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
        this.logMetric(metricName, metricValue, step, epoch, this.context);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue,
                          long step, long epoch, String context) {
        this.setStep(step);
        this.setEpoch(epoch);
        this.setContext(context);
        this.logMetric(metricName, metricValue, step, epoch, context, null);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue) {
        this.logParameter(parameterName, paramValue, this.step);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        this.logParameter(parameterName, paramValue, step, this.context);
    }

    @Override
    public void logParameter(String parameterName, Object paramValue, long step, String context) {
        this.setStep(step);
        this.setContext(context);
        this.logParameter(parameterName, paramValue, step, context, null);
    }

    @Override
    public void logHtml(@NonNull String html, boolean override) {
        this.logHtml(html, override, null);
    }

    @Override
    public void logOther(@NonNull String key, @NonNull Object value) {
        this.logOther(key, value, null);
    }

    @Override
    public void addTag(@NonNull String tag) {
        this.addTag(tag, null);
    }

    @Override
    public void logGraph(@NonNull String graph) {
        this.logGraph(graph, null);
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        this.logStartTime(startTimeMillis, null);
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        this.logEndTime(endTimeMillis, null);
    }

    @Override
    public void logGitMetadata(GitMetadata gitMetadata) {
        this.logGitMetadataAsync(gitMetadata, null);
    }

    @Override
    public void logLine(String line, long offset, boolean stderr) {
        this.logLine(line, offset, stderr, this.context);
    }

    @Override
    public void logLine(String line, long offset, boolean stderr, String context) {
        this.setContext(context);
        this.logLine(line, offset, stderr, context, null);
    }

    @Override
    public void logAssetFolder(File folder, boolean useFileNames, boolean recursive, long step, long epoch) {
        this.setStep(step);
        this.setEpoch(epoch);
        this.logAssetFolder(folder, useFileNames, recursive, step, epoch, null);
    }

    @Override
    public void logAssetFolder(File folder, boolean useFileNames, boolean recursive, long step) {
        this.logAssetFolder(folder, useFileNames, recursive, step, this.epoch);
    }

    @Override
    public void logAssetFolder(File folder, boolean useFileNames, boolean recursive) {
        this.logAssetFolder(folder, useFileNames, recursive, this.step);
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName,
                            boolean overwrite, long step, long epoch, String context) {
        this.setStep(step);
        this.setEpoch(epoch);
        this.setContext(context);
        this.uploadAsset(asset, fileName, overwrite, step, epoch, context, null);
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite, long step, long epoch, String context) {
        this.uploadAsset(asset, asset.getName(), overwrite, step, epoch, context);
    }

    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite, long step) {
        this.uploadAsset(asset, fileName, overwrite, step, this.epoch, this.context);
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite) {
        this.uploadAsset(asset, fileName, overwrite, this.step, this.epoch, this.context);
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite) {
        this.uploadAsset(asset, overwrite, this.step, this.epoch, this.context);
    }

    @Override
    public void logCode(@NonNull String code, @NonNull String fileName, String context) {
        this.logCode(code, fileName, context, null);
    }

    @Override
    public void logCode(@NonNull File file, String context) {
        this.logCode(file, context, null);
    }

    @Override
    public void logCode(@NonNull String code, @NonNull String fileName) {
        this.logCode(code, fileName, this.context);
    }

    @Override
    public void logCode(@NonNull File file) {
        this.logCode(file, this.context);
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
     * Returns builder to be used to create properly configured instance of this class.
     *
     * @return the builder to be used to create properly configured instance of this class.
     */
    public static OnlineExperimentBuilderImpl builder() {
        return new OnlineExperimentBuilderImpl();
    }
}
