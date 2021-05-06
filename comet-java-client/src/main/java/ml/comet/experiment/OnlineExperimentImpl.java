package ml.comet.experiment;

import com.typesafe.config.Config;
import lombok.Getter;
import ml.comet.experiment.http.Connection;
import ml.comet.experiment.builder.OnlineExperimentBuilder;
import ml.comet.experiment.constants.Constants;
import ml.comet.experiment.http.ConnectionInitializer;
import ml.comet.experiment.model.CreateExperimentRequest;
import ml.comet.experiment.model.CreateExperimentResponse;
import ml.comet.experiment.model.OutputLine;
import ml.comet.experiment.model.OutputUpdate;
import ml.comet.experiment.log.StdOutLogger;
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

import static ml.comet.experiment.constants.Constants.COMET_API_KEY;
import static ml.comet.experiment.constants.Constants.COMET_PROJECT;
import static ml.comet.experiment.constants.Constants.COMET_WORKSPACE;
import static ml.comet.experiment.constants.Constants.EXPERIMENT_KEY;
import static ml.comet.experiment.constants.Constants.ADD_OUTPUT;

@Getter
public class OnlineExperimentImpl extends BaseExperiment implements OnlineExperiment {
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final Config config;
    private final String projectName;
    private final String workspaceName;
    private final String apiKey;

    private Logger logger = LoggerFactory.getLogger(OnlineExperimentImpl.class);
    private Connection connection;
    private String experimentKey;
    private String experimentLink;
    private String experimentName;
    private StdOutLogger stdOutLogger;
    private StdOutLogger stdErrLogger;
    private boolean interceptStdout;
    private ScheduledFuture pingStatusFuture;

    private long step = 0;
    private String context = "";

    private OnlineExperimentImpl(
            String apiKey,
            String projectName,
            String workspaceName,
            String experimentName,
            String experimentKey,
            Logger logger,
            Config config,
            boolean interceptStdout) {
        this.config = config;
        this.projectName = projectName;
        this.workspaceName = workspaceName;
        this.apiKey = apiKey;
        this.experimentName = experimentName;
        this.experimentKey = experimentKey;
        this.interceptStdout = interceptStdout;
        if (logger != null) {
            this.logger = logger;
        }
        this.initializeExperiment();
    }

    public OnlineExperimentImpl() {
        this.config = ConfigUtils.getDefaultConfigFromClassPath();
        this.projectName = config.getString(COMET_PROJECT);
        this.workspaceName = config.getString(COMET_WORKSPACE);
        this.apiKey = config.getString(COMET_API_KEY);
        this.initializeExperiment();
    }

    public String getExperimentName() {
        return experimentName;
    }

    public static OnlineExperimentBuilderImpl builder() {
        return new OnlineExperimentBuilderImpl();
    }

    public static class OnlineExperimentBuilderImpl implements OnlineExperimentBuilder {
        private String projectName;
        private String workspace;
        private String apiKey;
        private String experimentName;
        private String experimentKey;
        private Logger logger;
        private Config config;
        private boolean interceptStdout = false;

        /**
         * Create a builder to construct an Experiment Object
         */
        private OnlineExperimentBuilderImpl() {
            this.config = ConfigUtils.getDefaultConfigFromClassPath();
            this.apiKey = config.getString(COMET_API_KEY);
            this.projectName = config.getString(COMET_PROJECT);
            this.workspace = config.getString(COMET_WORKSPACE);
        }

        @Override
        public OnlineExperimentBuilderImpl withProjectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl withWorkspace(String workspace) {
            this.workspace = workspace;
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl withApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl withExperimentName(String experimentName) {
            this.experimentName = experimentName;
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl withExistingExperimentKey(String experimentKey) {
            this.experimentKey = experimentKey;
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl withConfig(File overrideConfig) {
            this.config = ConfigUtils.getConfigFromFile(overrideConfig)
                    .withFallback(this.config)
                    .resolve();
            return this;
        }

        @Override
        public OnlineExperimentBuilderImpl interceptStdout() {
            this.interceptStdout = true;
            return this;
        }

        @Override
        public OnlineExperimentImpl build() {
            return new OnlineExperimentImpl(apiKey, projectName, workspace, experimentName, experimentKey, logger, config, interceptStdout);
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
        if (pingStatusFuture != null) {
            pingStatusFuture.cancel(true);
            pingStatusFuture = null;
        }
    }

    @Override
    public void setInterceptStdout() throws IOException {
        if (!interceptStdout) {
            interceptStdout = true;
            captureStdout();
        }
    }

    @Override
    public void stopInterceptStdout() {
        if (stdOutLogger != null) {
            stdOutLogger.stop();
            stdOutLogger = null;
            interceptStdout = false;
        }
        if (stdErrLogger != null) {
            stdErrLogger.stop();
            stdErrLogger = null;
        }
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
        step++;
    }

    @Override
    public long getStep() {
        return step;
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
    public void logMetric(String metricName, Object metricValue) {
        logMetric(metricName, metricValue, step);
    }

    @Override
    public void logMetric(String metricName, Object metricValue, long step) {
        this.setStep(step);
        super.logMetric(metricName, metricValue, step);
    }

    @Override
    public void logParameter(String parameterName, Object paramValue) {
        logParameter(parameterName, paramValue, step);
    }

    @Override
    public void logParameter(String parameterName, Object paramValue, long step) {
        this.setStep(step);
        super.logParameter(parameterName, paramValue, step);
    }

    @Override
    public void uploadAsset(File asset, boolean overwrite) {
        uploadAsset(asset, asset.getName(), overwrite);
    }

    @Override
    public void uploadAsset(File asset, String fileName, boolean overwrite) {
        super.uploadAsset(asset, fileName, overwrite, step);
    }

    private void initializeExperiment() {
        this.connection = ConnectionInitializer.initConnection(this.config, this.apiKey, this.logger);
        setupStdOutIntercept();
        registerExperiment();
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

        connection.sendPost(body, Constants.NEW_EXPERIMENT)
                .ifPresent(response -> {
                    CreateExperimentResponse result = JsonUtils.fromJson(response, CreateExperimentResponse.class);
                    this.experimentKey = result.getExperimentKey();
                    this.experimentLink = result.getLink();
                    logger.info("Experiment is live on comet.ml " + getExperimentUrl());
                    pingStatusFuture = scheduledExecutorService.scheduleAtFixedRate(
                            new StatusPing(this), 1, 3, TimeUnit.SECONDS);
                });

        if (this.experimentKey == null) {
            throw new RuntimeException("Failed to register onlineExperiment with Comet ML");
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

    protected void pingStatus() {
        if (experimentKey == null) {
            return;
        }
        logger.debug("pingStatus");
        connection.sendGet(Constants.EXPERIMENT_STATUS, Collections.singletonMap(EXPERIMENT_KEY, experimentKey));
    }

    private OutputUpdate getLogLineRequest(String line, long offset, boolean stderr) {
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

    static class StatusPing implements Runnable {
        OnlineExperimentImpl onlineExperiment;

        StatusPing(OnlineExperimentImpl onlineExperiment) {
            this.onlineExperiment = onlineExperiment;
        }

        @Override
        public void run() {
            onlineExperiment.pingStatus();
        }
    }


}
