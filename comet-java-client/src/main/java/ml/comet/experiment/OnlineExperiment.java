package ml.comet.experiment;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import ml.comet.response.GitMetadata;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ml.comet.experiment.Constants.*;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class OnlineExperiment implements Experiment {
    public static final String OUTPUT = "/output";
    private static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private Connection connection;
    private Config config;

    private Optional<String> experimentKey = Optional.empty();
    private Optional<String> experimentLink = Optional.empty();

    private String projectName;
    private String workspace;
    private Optional<String> experimentName = Optional.empty();
    private Optional<StdOutLogger> stdOutLogger = Optional.empty();
    private Optional<StdOutLogger> stdErrLogger = Optional.empty();
    private boolean interceptStdout = false;

    private Logger logger = LoggerFactory.getLogger(OnlineExperiment.class);
    private Optional<String> apiKey;
    private Optional<ScheduledFuture> pingStatusFuture = Optional.empty();

    private long step = 0;
    private String context = "";
    private final int maxAuthRetries = 4;

    private OnlineExperiment(
            String apiKey,
            String projectName,
            String workspace,
            Optional<String> experimentName,
            Optional<String> experimentKey,
            Optional<Logger> logger,
            Config config,
            boolean interceptStdout) {
        this.config = config;

        this.projectName = projectName;
        this.workspace = workspace;
        this.apiKey = Optional.of(apiKey);
        this.experimentName = experimentName;
        this.experimentKey = experimentKey;
        this.interceptStdout = interceptStdout;

        if (logger.isPresent()) {
            this.logger = logger.get();
        }

        this.initializeExperiment();
    }

    public OnlineExperiment() {
        this.config = ConfigFactory.parseFile(
                new File(getClass().getClassLoader().getResource(Constants.DEFAULTS_CONF).getFile()));

        String apiKey = config.getString("comet.apiKey");
        String project = config.getString("comet.project");
        String workspace = config.getString("comet.workspace");
        this.projectName = project;
        this.workspace = workspace;
        this.apiKey = Optional.of(apiKey);
        this.initializeExperiment();
    }

    public static OnlineExperiment of(String apiKey, String projectName, String workspace) {
        Config config = ConfigFactory.parseFile(
            new File(OnlineExperiment.class.getClassLoader().getResource(Constants.DEFAULTS_CONF).getFile()));
        OnlineExperiment onlineExperiment = new OnlineExperiment(apiKey, projectName, workspace, Optional.empty(), Optional.empty(), Optional.empty(), config, true);
        return onlineExperiment;
    }

    public static OnlineExperimentBuilder builder(String apiKey, String projectName, String workspace) {
        return new OnlineExperimentBuilder(apiKey, projectName, workspace);
    }

    public static class OnlineExperimentBuilder implements ExperimentBuilder {
        private String projectName;
        private String workspace;
        private String apiKey;
        private Optional<String> experimentName = Optional.empty();
        private Optional<String> experimentKey = Optional.empty();
        private Optional<Logger> logger = Optional.empty();
        private Config config;
        private boolean interceptStdout = false;

        /**
         * Create a builder to construct an Experiment Object
         * @param projectName The project under which the experiment should run
         * @param workspace   The workspace under which the experiment should be run
         */
        private OnlineExperimentBuilder(String apiKey, String projectName, String workspace) {
            this.config = ConfigFactory.parseFile(
                    new File(getClass().getClassLoader().getResource(Constants.DEFAULTS_CONF).getFile()));
            this.projectName = projectName;
            this.workspace = workspace;
            this.apiKey = apiKey;
        }

        @Override
        public OnlineExperimentBuilder withProjectName(String projectName) {
            this.projectName = projectName;
            return this;
        }

        @Override
        public OnlineExperimentBuilder withWorkspace(String workspace) {
            this.workspace = workspace;
            return this;
        }

        @Override
        public OnlineExperimentBuilder withApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        @Override
        public OnlineExperimentBuilder withExperimentName(String experimentName) {
            this.experimentName = Optional.of(experimentName);
            return this;
        }

        @Override
        public OnlineExperimentBuilder withExistingExperimentKey(String experimentKey) {
            this.experimentKey = Optional.of(experimentKey);
            return this;
        }

        @Override
        public OnlineExperimentBuilder withLogger(Logger logger) {
            this.logger = Optional.ofNullable(logger);
            return this;
        }

        @Override
        public OnlineExperimentBuilder withConfig(File overrideConfig) {
            this.config = ConfigFactory.parseFile(overrideConfig)
                    .withFallback(this.config)
                    .resolve();
            return this;
        }

        @Override
        public OnlineExperimentBuilder interceptStdout() {
            this.interceptStdout = true;
            return this;
        }

        @Override
        public OnlineExperiment build() {
            return new OnlineExperiment(apiKey, projectName, workspace, experimentName, experimentKey, logger, config, interceptStdout);
        }
    }

    private void initializeExperiment() {
        setupConnection();
        setupStdOutIntercept();
        registerExperiment();
    }

    private void setupConnection() {
        if (!this.apiKey.isPresent()) {
            throw new RuntimeException("Api Key required");
        }
        this.connection =
                new Connection(
                        this.config.getString(Constants.COMET_URL),
                        this.apiKey,
                        Optional.empty(),
                        this.logger,
                        this.maxAuthRetries);
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
        if (experimentKey.isPresent()) {
            logger.debug("Not registering a new experiment.  Using experiment key {}", experimentKey.get());
            return;
        }

        JSONObject obj = new JSONObject();
        obj.put("project_name", projectName);
        obj.put("workspace", workspace);
        this.experimentName.ifPresent(
                experiment -> obj.put(Constants.EXPERIMENT_NAME, experiment));
        Optional<String> responseOptional = connection.sendPost(obj.toString(), Constants.NEW_EXPERIMENT);
        responseOptional.ifPresent(response -> {
            JSONObject result = new JSONObject(response);
            if (result.has(EXPERIMENT_KEY)) {
                this.experimentKey = Optional.ofNullable(result.getString(EXPERIMENT_KEY));
                this.experimentLink = Optional.ofNullable(result.getString(Constants.LINK));
                logger.info("Experiment is live on comet.ml " + this.experimentLink.orElse(""));
                pingStatusFuture = Optional.of(scheduledExecutorService.scheduleAtFixedRate(
                        new StatusPing(this), 1, 3, TimeUnit.SECONDS));
            }
        });

        if (!this.experimentKey.isPresent()) {
            throw new RuntimeException("Failed to register onlineExperiment with Comet ML");
        }
    }

    @Override
    public void exit() {
        if (pingStatusFuture.isPresent()) {
            pingStatusFuture.get().cancel(true);
            pingStatusFuture = Optional.empty();
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
        if (stdOutLogger.isPresent()) {
            stdOutLogger.get().stop();
            stdOutLogger = Optional.empty();
            interceptStdout = false;
        }
        if (stdErrLogger.isPresent()) {
            stdErrLogger.get().stop();
            stdErrLogger = Optional.empty();
        }
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
        return this.experimentKey.get();
    }

    @Override
    public Optional<String> getExperimentLink() {
        return this.experimentLink;
    }

    @Override
    public void setExperimentName(String experimentName) {
        logOther("Name", experimentName);
    }

    @Override
    public void logMetric(String metricName, Object metricValue) {
        logMetric(metricName, metricValue, step);
    }

    @Override
    public void logMetric(String metricName, Object metricValue, long step) {
        this.setStep(step);
        logger.debug("logMetric {} {}", metricName, metricValue);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("metricName", metricName);
            obj.put("metricValue", getObjectValue(metricValue));
            obj.put("step", step);
            obj.put("timestamp", System.currentTimeMillis());
            connection.sendPostAsync(obj.toString(), METRIC);
        });
    }

    @Override
    public void logParameter(String parameterName, Object paramValue) {
        logParameter(parameterName, paramValue, step);
    }

    @Override
    public void logParameter(String parameterName, Object paramValue, long step) {
        this.setStep(step);
        logger.debug("logParameter {} {}", parameterName, paramValue);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("paramName", parameterName);
            obj.put("paramValue", getObjectValue(paramValue));
            obj.put("step", step);
            connection.sendPostAsync(obj.toString(), PARAMETER);
        });
    }

    @Override
    public void logHtml(String html, boolean override) {
        logger.debug("logHtml {} {}", html, override);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("html", html);
            obj.put("override", override);
            connection.sendPostAsync(obj.toString(), SET_HTML);
        });
    }

    @Override
    public void logCode(String code) {
        logger.debug("logCode {}", code);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("code", code);
            connection.sendPostAsync(obj.toString(), SET_CODE);
        });
    }

    @Override
    public void logOther(String key, Object value) {
        logger.debug("logOther {} {}", key, value);
        this.experimentKey.ifPresent(expKey -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, expKey);
            obj.put("key", key);
            obj.put("val", getObjectValue(value));
            connection.sendPostAsync(obj.toString(), LOG_OTHER);
        });
    }

    @Override
    public void addTag(String tag) {
        logger.debug("logTag {}", tag);
        this.experimentKey.ifPresent(expKey -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, expKey);
            obj.put("addedTags", Collections.singletonList(tag));
            connection.sendPostAsync(obj.toString(), ADD_TAG);
        });
    }

    @Override
    public void logGraph(String graph) {
        logger.debug("logOther {}", graph);

        this.experimentKey.ifPresent(expKey -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, expKey);
            obj.put("graph", graph);
            obj.put("offset", step);
            connection.sendPostAsync(obj.toString(), GRAPH);
        });
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        logger.debug("logStartTime {}", startTimeMillis);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("start_time_millis", startTimeMillis);
            connection.sendPostAsync(obj.toString(), EXPERIMENT_START_END_TIME);
        });
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        logger.debug("logEndTime {}", endTimeMillis);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("end_time_millis", endTimeMillis);
            connection.sendPostAsync(obj.toString(), EXPERIMENT_START_END_TIME);
        });
    }

    @Override
    public void uploadAsset(File asset, String fileName, boolean overwrite) {
        logger.debug(String.format("uploadAsset {} {} {}", asset.getName(), fileName, overwrite));
        this.experimentKey.ifPresent(key ->
                connection.sendPost(asset, UPLOAD_ASSET, new HashMap<String, String>() {{
                    put(EXPERIMENT_KEY, key);
                    put("fileName", fileName);
                    put("step", Long.toString(step));
                    put("context", context);
                    put("overwrite", Boolean.toString(overwrite));
                }}));
    }

    @Override
    public void uploadAsset(File asset, boolean overwrite){
        uploadAsset(asset, asset.getName(), overwrite);
    }

    @Override
    public void uploadImage(File image, String imageName, boolean overwrite) {
        logger.debug(String.format("uploadImage {} {} {}", image.getName(), imageName, overwrite));
        this.experimentKey.ifPresent(key -> {
            connection.sendPost(image, UPLOAD_IMAGE, new HashMap<String, String>() {{
                put(EXPERIMENT_KEY, key);
                put("figureName", imageName);
                put("step", Long.toString(step));
                put("context", context);
                put("overwrite", Boolean.toString(overwrite));
            }});
        });
    }

    @Override
    public void logGitMetadata(GitMetadata gitMetadata) {
        logger.debug(String.format("gitMetadata {}", gitMetadata));
        this.experimentKey.ifPresent(key -> {
            JSONObject outer = new JSONObject();
            outer.put(EXPERIMENT_KEY, key);
            JSONObject inner = new JSONObject();
            inner.put("runContext", context);
            JSONObject metadata = new JSONObject();
            metadata.put("user", gitMetadata.getUser());
            metadata.put("origin", gitMetadata.getOrigin());
            metadata.put("branch", gitMetadata.getBranch());
            metadata.put("root", gitMetadata.getRoot());
            metadata.put("parent", gitMetadata.getParent());
            inner.put("sdkGitMetadata", metadata);
            outer.put("sdkGitMetadataWithContext", inner);
            connection.sendPostAsync(outer.toString(), SET_GIT_METADATA);
        });
    }

    @Override
    public void uploadImage(File image, boolean overwrite){
        uploadImage(image, image.getName(), overwrite);
    }

    // Internal OnlineExperiment Logic Methods
    private void captureStdout() throws IOException {
        StdOutLogger logger = StdOutLogger.createStdoutLogger(this);
        stdOutLogger = Optional.of(logger);

        StdOutLogger errorLogger = StdOutLogger.createStderrLogger(this);
        stdErrLogger = Optional.of(errorLogger);
    }

    protected void logLine(String line, int offset, boolean stderr) {
        this.experimentKey.ifPresent(key -> {
            JSONObject stdoutLine = new JSONObject();
            stdoutLine.put("stdout", line);
            stdoutLine.put("stderr", stderr);
            stdoutLine.put("local_timestamp", System.currentTimeMillis());
            stdoutLine.put("offset", offset);

            JSONArray stdoutArray = new JSONArray();
            stdoutArray.put(0, stdoutLine);

            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("stdoutLines", stdoutArray);
            obj.put("runContext", context);

            connection.sendPostAsync(obj.toString(), OUTPUT);
        });
    }

    protected void pingStatus() {
        this.experimentKey.ifPresent(key -> {
            logger.debug("pingStatus");
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            connection.sendPostAsync(obj.toString(), Constants.EXPERIMENT_STATUS);
        });
    }

    static class StatusPing implements Runnable {
        OnlineExperiment onlineExperiment;

        StatusPing(OnlineExperiment onlineExperiment) {
            this.onlineExperiment = onlineExperiment;
        }

        @Override
        public void run() {
            onlineExperiment.pingStatus();
        }
    }

    private String getObjectValue(Object val) {
        return val.toString();
    }
}
