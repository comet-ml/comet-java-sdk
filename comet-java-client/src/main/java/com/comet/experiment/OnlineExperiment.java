package com.comet.experiment;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.comet.experiment.Contstants.EXPERIMENT_KEY;
import static com.comet.experiment.Contstants.UPLOAD_IMAGE;

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
    private Optional<String> apiKey = Optional.empty();
    private Optional<ScheduledFuture> pingStatusFuture = Optional.empty();

    private long step = 0;
    private String context = "";

    private OnlineExperiment(String apiKey, String projectName, String workspace) {
        this.config = ConfigFactory.parseFile(
                new File(getClass().getClassLoader().getResource(Contstants.DEFAULTS_CONF).getFile()));

        this.projectName = projectName;
        this.workspace = workspace;
        this.apiKey = Optional.of(apiKey);
        this.initializeExperiment();
    }

    public OnlineExperiment() {
        this.config = ConfigFactory.parseFile(
                new File(getClass().getClassLoader().getResource(Contstants.DEFAULTS_CONF).getFile()));

        String apiKey = config.getString("comet.apiKey");
        String project = config.getString("comet.project");
        String workspace = config.getString("comet.workspace");
        this.projectName = project;
        this.workspace = workspace;
        this.apiKey = Optional.of(apiKey);
        this.initializeExperiment();
    }

    public static OnlineExperiment of(String apiKey, String projectName, String workspace) {
        OnlineExperiment onlineExperiment = new OnlineExperiment(apiKey, projectName, workspace);
        onlineExperiment.initializeExperiment();
        return onlineExperiment;
    }

    public static OnlineExperimentBuilder builder(String projectName, String workspace, String apiKey) {
        return new OnlineExperimentBuilder(projectName, workspace, apiKey);
    }

    public static class OnlineExperimentBuilder implements ExperimentBuilder {
        OnlineExperiment onlineExperiment;

        /**
         * Create a builder to construct an Experiment Object
         * @param projectName The project under which the experiment should run
         * @param workspace   The workspace under which the experiment should be run
         */
        private OnlineExperimentBuilder(String projectName, String workspace, String apiKey) {
            this.onlineExperiment = new OnlineExperiment();
            this.onlineExperiment.projectName = projectName;
            this.onlineExperiment.workspace = workspace;
            this.onlineExperiment.apiKey = Optional.of(apiKey);
        }

        @Override
        public OnlineExperimentBuilder withProjectName(String projectName) {
            this.onlineExperiment.projectName = projectName;
            return this;
        }

        @Override
        public OnlineExperimentBuilder withWorkspace(String workspace) {
            this.onlineExperiment.workspace = workspace;
            return this;
        }

        @Override
        public OnlineExperimentBuilder withApiKey(String apiKey) {
            this.onlineExperiment.apiKey = Optional.of(apiKey);
            return this;
        }

        @Override
        public OnlineExperimentBuilder withExperimentName(String experimentName) {
            this.onlineExperiment.experimentName = Optional.of(experimentName);
            return this;
        }

        @Override
        public OnlineExperimentBuilder withExistingExperimentKey(String experimentKey) {
            this.onlineExperiment.experimentKey = Optional.of(experimentKey);
            return this;
        }

        @Override
        public OnlineExperimentBuilder withLogger(Logger logger) {
            this.onlineExperiment.logger = logger;
            return this;
        }

        @Override
        public OnlineExperimentBuilder withConfig(File overrideConfig) {
            this.onlineExperiment.config = ConfigFactory.parseFile(overrideConfig)
                    .withFallback(this.onlineExperiment.config)
                    .resolve();
            return this;
        }

        @Override
        public OnlineExperimentBuilder interceptStdout() {
            this.onlineExperiment.interceptStdout = true;
            return this;
        }

        @Override
        public OnlineExperiment build() {
            this.onlineExperiment.initializeExperiment();
            return this.onlineExperiment;
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
                        this.config.getString(Contstants.COMET_URL),
                        this.apiKey.get(),
                        this.logger);
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
                experiment -> obj.put(Contstants.EXPERIMENT_NAME, experiment));
        Optional<String> responseOptional = connection.sendPost(obj.toString(), Contstants.NEW_EXPERIMENT);
        responseOptional.ifPresent(response -> {
            JSONObject result = new JSONObject(response);
            if (result.has(EXPERIMENT_KEY)) {
                this.experimentKey = Optional.ofNullable(result.getString(EXPERIMENT_KEY));
                this.experimentLink = Optional.ofNullable(result.getString(Contstants.LINK));
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
            connection.sendPostAsync(obj.toString(), Contstants.METRIC);
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
            connection.sendPostAsync(obj.toString(), Contstants.PARAMETER);
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
            connection.sendPostAsync(obj.toString(), Contstants.HTML);
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
            connection.sendPostAsync(obj.toString(), Contstants.LOG_OTHER);
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
            connection.sendPostAsync(obj.toString(), Contstants.GRAPH);
        });
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        logger.debug("logStartTime {}", startTimeMillis);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("start_time_millis", startTimeMillis);
            connection.sendPostAsync(obj.toString(), Contstants.EXPERIMENT_START_END_TIME);
        });
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        logger.debug("logEndTime {}", endTimeMillis);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put(EXPERIMENT_KEY, key);
            obj.put("end_time_millis", endTimeMillis);
            connection.sendPostAsync(obj.toString(), Contstants.EXPERIMENT_START_END_TIME);
        });
    }

    @Override
    public void uploadAsset(File asset, String fileName, boolean overwrite) {
        logger.debug(String.format("uploadAsset {} {} {}", asset.getName(), fileName, overwrite));
        this.experimentKey.ifPresent(key ->
                connection.sendPost(asset, Contstants.UPLOAD_ASSET, new HashMap<String, String>() {{
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
            connection.sendPostAsync(obj.toString(), Contstants.EXPERIMENT_STATUS);
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
