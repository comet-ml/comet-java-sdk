package ml.comet.experiment.impl;

import io.reactivex.rxjava3.functions.Action;
import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.log.StdOutLogger;
import ml.comet.experiment.impl.rest.ExperimentStatusResponse;
import ml.comet.experiment.model.GitMetaData;
import org.awaitility.Awaitility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_ALREADY_CLOSED_STATUS_ERROR;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_CLEANUP_PROMPT;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_HEARTBEAT_STOPPED_PROMPT;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_INVENTORY_STATUS_PROMPT;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_CLEAN_EXPERIMENT_INVENTORY;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_ASSET;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_ASSET_FOLDER;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_CODE_ASSET;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_REMOTE_ASSET;
import static ml.comet.experiment.impl.resources.LogMessages.TIMEOUT_FOR_EXPERIMENT_INVENTORY_CLEANUP;
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
    // The flag to indicate if experiment end() was called and experiment cleaning its inventory
    private final AtomicBoolean atCleanup = new AtomicBoolean();

    // The counter to maintain current inventory of the artifacts being in progress
    private final AtomicInteger artifactsInProgress = new AtomicInteger();
    // The counter to maintain current inventory of the assets being in progress
    private final AtomicInteger assetsInProgress = new AtomicInteger();

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
        if (this.hasShutdownStarted()) {
            // already shutting down
            return;
        }
        getLogger().info(getString(EXPERIMENT_CLEANUP_PROMPT, cleaningTimeout.getSeconds()));

        // mark as shutting down
        //
        this.atShutdown.set(true);

        // wait for inventory to complete all pending actions
        //
        this.atCleanup.set(true);
        try {
            this.waitForInventoryCleanup();
        } catch (Throwable t) {
            this.logger.error(getString(FAILED_TO_CLEAN_EXPERIMENT_INVENTORY), t);
        }
        this.atCleanup.set(false);

        // stop pinging server
        //
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
        //
        if (this.interceptStdout) {
            try {
                this.stopInterceptStdout();
            } catch (IOException e) {
                logger.error("failed to stop StdOut/StdErr intercepting", e);
            }
        }

        // invoke end of the superclass for common cleanup routines
        //
        super.end();
    }

    /**
     * Allows using {@link OnlineExperiment} with try-with-resources statement with automatic closing after usage.
     *
     * @throws Exception if an exception occurs.
     */
    @Override
    public void close() throws Exception {
        this.end();
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
        this.setStep(this.getStep() + 1);
    }

    @Override
    public long getStep() {
        if (this.baseContext.getStep() != null) {
            return this.baseContext.getStep();
        } else {
            return 0;
        }
    }

    @Override
    public void setStep(long step) {
        this.baseContext.setStep(step);
    }

    @Override
    public void nextEpoch() {
        this.setEpoch(this.getEpoch() + 1);
    }

    @Override
    public long getEpoch() {
        if (this.baseContext.getEpoch() != null) {
            return this.baseContext.getEpoch();
        } else {
            return 0;
        }
    }

    @Override
    public void setEpoch(long epoch) {
        this.baseContext.setEpoch(epoch);
    }

    @Override
    public void setContext(String context) {
        this.baseContext.setContext(context);
    }

    @Override
    public String getContext() {
        return this.baseContext.getContext();
    }

    @Override
    public Optional<String> getExperimentLink() {
        return ofNullable(this.experimentLink);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, @NonNull ExperimentContext context) {
        this.checkExperimentActiveState();
        this.logMetric(metricName, metricValue, context, empty());
    }

    @Override
    public void logMetric(String metricName, Object metricValue, long step, long epoch) {
        this.logMetric(metricName, metricValue,
                new ExperimentContext(step, epoch, this.getContext()));
    }

    @Override
    public void logMetric(String metricName, Object metricValue, long step) {
        this.logMetric(metricName, metricValue,
                new ExperimentContext(step, this.getEpoch(), this.getContext()));
    }

    @Override
    public void logMetric(String metricName, Object metricValue) {
        this.logMetric(metricName, metricValue, this.baseContext);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue) {
        this.logParameter(parameterName, paramValue, this.baseContext);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        this.logParameter(parameterName, paramValue,
                new ExperimentContext(step, this.getEpoch(), this.getContext()));
    }

    @Override
    public void logParameter(String parameterName, Object paramValue, @NonNull ExperimentContext context) {
        this.checkExperimentActiveState();
        this.logParameter(parameterName, paramValue, context, empty());
    }

    @Override
    public void logHtml(@NonNull String html, boolean override) {
        this.checkExperimentActiveState();
        this.logHtml(html, override, empty());
    }

    @Override
    public void logOther(@NonNull String key, @NonNull Object value) {
        this.checkExperimentActiveState();
        this.logOther(key, value, empty());
    }

    @Override
    public void addTag(@NonNull String tag) {
        this.checkExperimentActiveState();
        this.addTag(tag, empty());
    }

    @Override
    public void logGraph(@NonNull String graph) {
        this.checkExperimentActiveState();
        this.logGraph(graph, empty());
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        this.checkExperimentActiveState();
        this.logStartTime(startTimeMillis, empty());
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        this.checkExperimentActiveState();
        this.logEndTime(endTimeMillis, empty());
    }

    @Override
    public void logGitMetadata(GitMetaData gitMetadata) {
        this.checkExperimentActiveState();
        this.logGitMetadataAsync(gitMetadata, empty());
    }

    @Override
    public void logLine(String line, long offset, boolean stderr) {
        this.logLine(line, offset, stderr, this.getContext());
    }

    @Override
    public void logLine(String line, long offset, boolean stderr, String context) {
        this.setContext(context);
        this.logLine(line, offset, stderr, context, empty());
    }

    @Override
    public void logAssetFolder(@NonNull File folder, boolean logFilePath,
                               boolean recursive, @NonNull ExperimentContext context) {
        this.checkExperimentActiveState();
        this.executeLogAction(() ->
                        this.logAssetFolder(folder, logFilePath, recursive, true, context,
                                this.logAssetActionOnComplete()),
                this.assetsInProgress, getString(FAILED_TO_LOG_ASSET_FOLDER, folder));
    }

    @Override
    public void logAssetFolder(@NonNull File folder, boolean logFilePath, boolean recursive) {
        this.logAssetFolder(folder, logFilePath, recursive, this.baseContext);
    }

    @Override
    public void logAssetFolder(@NonNull File folder, boolean logFilePath) {
        this.logAssetFolder(folder, logFilePath, false);
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName,
                            boolean overwrite, @NonNull ExperimentContext context) {
        this.checkExperimentActiveState();
        this.executeLogAction(() ->
                        this.uploadAsset(asset, fileName, overwrite, context, this.logAssetActionOnComplete()),
                this.assetsInProgress, getString(FAILED_TO_LOG_ASSET, fileName));
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite, @NonNull ExperimentContext context) {
        this.uploadAsset(asset, asset.getName(), overwrite, context);
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite, long step, long epoch) {
        this.uploadAsset(asset, asset.getName(), overwrite,
                new ExperimentContext(step, epoch, getContext()));
    }

    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite, long step) {
        this.uploadAsset(asset, fileName, overwrite,
                new ExperimentContext(step, this.getEpoch(), this.getContext()));
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite) {
        this.uploadAsset(asset, fileName, overwrite, this.baseContext);
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite) {
        this.uploadAsset(asset, asset.getName(), overwrite, this.baseContext);
    }

    @Override
    public void logRemoteAsset(@NonNull URI uri, String fileName, boolean overwrite,
                               Map<String, Object> metadata, @NonNull ExperimentContext context) {
        this.checkExperimentActiveState();
        this.executeLogAction(() -> this.logRemoteAsset(uri, ofNullable(fileName), overwrite,
                        ofNullable(metadata), context, this.logAssetActionOnComplete()),
                this.assetsInProgress, getString(FAILED_TO_LOG_REMOTE_ASSET, uri));
    }

    @Override
    public void logRemoteAsset(@NonNull URI uri, String fileName, boolean overwrite, Map<String, Object> metadata) {
        this.logRemoteAsset(uri, fileName, overwrite, metadata, this.baseContext);
    }

    @Override
    public void logRemoteAsset(@NonNull URI uri, @NonNull String fileName, boolean overwrite) {
        this.logRemoteAsset(uri, fileName, overwrite, null);
    }

    @Override
    public void logRemoteAsset(@NonNull URI uri, boolean overwrite) {
        this.logRemoteAsset(uri, null, overwrite, null, this.baseContext);
    }

    @Override
    public void logCode(@NonNull String code, @NonNull String fileName, @NonNull ExperimentContext context) {
        this.checkExperimentActiveState();
        this.executeLogAction(() -> this.logCode(code, fileName, context, this.logAssetActionOnComplete()),
                this.assetsInProgress, getString(FAILED_TO_LOG_CODE_ASSET, fileName));
    }

    @Override
    public void logCode(@NonNull File file, @NonNull ExperimentContext context) {
        this.checkExperimentActiveState();
        this.executeLogAction(() -> this.logCode(file, context, this.logAssetActionOnComplete()),
                this.assetsInProgress, getString(FAILED_TO_LOG_CODE_ASSET, file));
    }

    @Override
    public void logCode(@NonNull String code, @NonNull String fileName) {
        this.logCode(code, fileName, this.baseContext);
    }

    @Override
    public void logCode(@NonNull File file) {
        this.logCode(file, this.baseContext);
    }

    @Override
    public CompletableFuture<LoggedArtifact> logArtifact(Artifact artifact) throws ArtifactException {
        this.checkExperimentActiveState();
        try {
            this.artifactsInProgress.incrementAndGet();
            return this.logArtifact(artifact, Optional.of(this.artifactsInProgress::decrementAndGet));
        } catch (Throwable t) {
            this.artifactsInProgress.decrementAndGet();
            throw t;
        }
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
        if (!this.alive || (this.hasShutdownStarted() && !this.atCleanup.get())) {
            // not alive or already finished cleanup while shutting down
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
     * Allows waiting for scheduled log actions to be completed.
     */
    private void waitForInventoryCleanup() {
        if (!this.alive || this.hasEmptyInventory()) {
            // already closed or has empty inventory
            return;
        }
        this.logger.info(getString(
                EXPERIMENT_INVENTORY_STATUS_PROMPT, this.assetsInProgress.get(), this.artifactsInProgress.get()));

        // wait for the inventory to be processed
        Awaitility
                .await(getString(TIMEOUT_FOR_EXPERIMENT_INVENTORY_CLEANUP,
                        this.assetsInProgress.get(), this.artifactsInProgress.get()))
                .atMost(this.cleaningTimeout)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(this::hasEmptyInventory);
    }

    /**
     * Allows checking if experiment inventory is fully processed and nothing is still awaiting.
     *
     * @return {@code true} if experiment inventory is fully processed.
     */
    private boolean hasEmptyInventory() {
        return this.artifactsInProgress.get() == 0 && this.assetsInProgress.get() == 0;
    }

    /**
     * Allows checking if Experiment shutdown already started.
     *
     * @return {@code true} if Experiment shutdown already started.
     */
    private boolean hasShutdownStarted() {
        return this.atShutdown.get();
    }

    /**
     * Allows checking if the experiment is in active state.
     *
     * @throws IllegalStateException is experiment was already closed by calling {@link #end()}.
     */
    private void checkExperimentActiveState() throws IllegalStateException {
        if (hasShutdownStarted()) {
            throw new IllegalStateException(getString(EXPERIMENT_ALREADY_CLOSED_STATUS_ERROR));
        }
    }

    /**
     * Executes provided log action wrapping it into the inventory tracking.
     *
     * @param action    the {@link Action} to be executed.
     * @param inventory the {@link AtomicInteger} to track inventory associated with action.
     */
    private void executeLogAction(final Action action, final AtomicInteger inventory, final String errMessage) {
        try {
            inventory.incrementAndGet();
            action.run();
        } catch (Throwable t) {
            inventory.decrementAndGet();
            logger.error(errMessage, t);
        }
    }

    private Optional<Action> logAssetActionOnComplete() {
        return Optional.of(this.assetsInProgress::decrementAndGet);
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
