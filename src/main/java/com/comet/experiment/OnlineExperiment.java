package com.comet.experiment;

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

public class OnlineExperiment implements Experiment {
    private static ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private Connection connection;
    private Optional<String> experimentKey = Optional.empty();
    private Optional<String> experimentLink = Optional.empty();

    private String projectName;
    private String workspace;
    private Optional<String> experimentName = Optional.empty();
    private Optional<StdOutLogger> stdOutLogger = Optional.empty();
    private Optional<StdOutLogger> stdErrLogger = Optional.empty();
    private boolean interceptStdout = false;

    private Logger logger = LoggerFactory.getLogger(OnlineExperiment.class);
    private Optional<String> restApiKey = Optional.empty();
    private Optional<ScheduledFuture> pingStatusFuture = Optional.empty();

    private long step = 0;
    private String context = "";

    private OnlineExperiment(String restApiKey, String projectName, String workspace) {
        this.projectName = projectName;
        this.workspace = workspace;
        this.restApiKey = Optional.of(restApiKey);
        this.connection = new Connection(restApiKey, logger);
    }

    private OnlineExperiment() {}

    public static OnlineExperiment of(String restApiKey, String projectName, String workspace) {
        OnlineExperiment onlineExperiment = new OnlineExperiment(restApiKey, projectName, workspace);
        boolean success = onlineExperiment.registerExperiment();
        if (!success) {
            throw new RuntimeException();
        }
        return onlineExperiment;
    }

    public static OnlineExperimentBuilder builder(String projectName, String workspace) {
        return new OnlineExperimentBuilder(projectName, workspace);
    }

    public static class OnlineExperimentBuilder implements ExperimentBuilder {
        OnlineExperiment onlineExperiment;

        private OnlineExperimentBuilder(String projectName, String workspace) {
            this.onlineExperiment = new OnlineExperiment();
            this.onlineExperiment.projectName = projectName;
            this.onlineExperiment.workspace = workspace;
        }

        public OnlineExperimentBuilder withProjectName(String projectName) {
            this.onlineExperiment.projectName = projectName;
            return this;
        }

        public OnlineExperimentBuilder withWorkspace(String workspace) {
            this.onlineExperiment.workspace = workspace;
            return this;
        }

        public OnlineExperimentBuilder withRestApiKey(String restApiKey) {
            this.onlineExperiment.restApiKey = Optional.of(restApiKey);
            return this;
        }

        public OnlineExperimentBuilder withExperimentName(String experimentName) {
            this.onlineExperiment.experimentName = Optional.of(experimentName);
            return this;
        }

        public OnlineExperimentBuilder withLogger(Logger logger) {
            this.onlineExperiment.logger = logger;
            return this;
        }

        public OnlineExperimentBuilder interceptStdout() {
            this.onlineExperiment.interceptStdout = true;
            return this;
        }

        public OnlineExperiment build() {
            setupConnection();
            setupStdOutIntercept();
            registerExperiment();

            return this.onlineExperiment;
        }

        private void setupConnection() {
            if (!this.onlineExperiment.restApiKey.isPresent()) {
                throw new RuntimeException("Rest Api Key required");
            }
            this.onlineExperiment.connection =
                    new Connection(
                            this.onlineExperiment.restApiKey.get(),
                            this.onlineExperiment.logger);
        }

        private void setupStdOutIntercept() {
            if (this.onlineExperiment.interceptStdout) {
                try {
                    this.onlineExperiment.captureStdout();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void registerExperiment() {
            boolean success = this.onlineExperiment.registerExperiment();
            if (!success) {
                throw new RuntimeException("Failed to register onlineExperiment with Comet ML");
            }
        }
    }

    private boolean registerExperiment() {
        JSONObject obj = new JSONObject();
        obj.put("project_name", projectName);
        obj.put("workspace", workspace);
        this.experimentName.ifPresent(
                experiment -> obj.put("experiment_name", experiment));
        Optional<String> responseOptional = connection.sendPost(obj.toString(), "/new-experiment");
        logger.debug(responseOptional.toString());

        responseOptional.ifPresent(response -> {
            JSONObject result = new JSONObject(response);
            if (result.has("experimentKey")) {
                this.experimentKey = Optional.ofNullable(result.getString("experimentKey"));
                this.experimentLink = Optional.ofNullable(result.getString("link"));

                pingStatusFuture = Optional.of(scheduledExecutorService.scheduleAtFixedRate(
                        new StatusPing(this), 1, 3, TimeUnit.SECONDS));
            }
        });
        return this.experimentKey.isPresent();
    }

    public void exit() {
        if (pingStatusFuture.isPresent()) {
            pingStatusFuture.get().cancel(true);
            pingStatusFuture = Optional.empty();
        }
    }

    public void setInterceptStdout() throws IOException {
        if (!interceptStdout) {
            interceptStdout = true;
            captureStdout();
        }
    }

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

    public void setStep(long step) {
        this.step = step;
    }

    public void nextStep() {
        step++;
    }

    public long getStep() {
        return step;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public String getContext() {
        return this.context;
    }

    public Optional<String> getExperimentKey() {
        return this.experimentKey;
    }

    public Optional<String> getExperimentLink() {
        return this.experimentLink;
    }

    public void setName(String name) {
        logOther("Name", name);
    }

    public void logMetric(String metricName, String metricValue) {
        logger.debug("logMetric {} {}", metricName, metricValue);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("metricName", metricName);
            obj.put("metricValue", metricValue);
            obj.put("step", step);
            connection.sendPostAsync(obj.toString(), "/metric");
        });
    }

    public void logParam(String paramName, String paramValue) {
        logger.debug("logParam {} {}", paramName, paramValue);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("paramName", paramName);
            obj.put("paramValue", paramValue);
            obj.put("step", step);
            connection.sendPostAsync(obj.toString(), "/parameter");
        });
    }

    public void logHtml(String html, boolean override) {
        logger.debug("logHtml {} {}", html, override);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("html", html);
            obj.put("override", override);
            connection.sendPostAsync(obj.toString(), "/html");
        });
    }

    public void logOther(String key, String value) {
        logger.debug("logOther {} {}", key, value);
        this.experimentKey.ifPresent(expKey -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", expKey);
            obj.put("key", key);
            obj.put("val", value);
            connection.sendPostAsync(obj.toString(), "/log-other");
        });
    }

    public void logGraph() {

    }

    public void logStartTime(long startTimeMillis) {
        logger.debug("logStartTime {}", startTimeMillis);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("start_time_millis", startTimeMillis);
            connection.sendPostAsync(obj.toString(), "/experiment-start-end-time");
        });
    }

    public void logEndTime(long endTimeMillis) {
        logger.debug("logEndTime {}", endTimeMillis);
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("end_time_millis", endTimeMillis);
            connection.sendPostAsync(obj.toString(), "/experiment-start-end-time");
        });
    }

    public void uploadAsset(File asset, String fileName, boolean overwrite) {
        logger.debug("uploadAsset {} {} {}", asset.getName(), fileName, overwrite);
        this.experimentKey.ifPresent(key ->
            connection.sendPost(asset, "/upload-asset", new HashMap<String, String>() {{
                put("experimentKey", key);
                put("fileName", fileName);
                put("step", Long.toString(step));
                put("context", context);
                put("overwrite", Boolean.toString(overwrite));
            }}));
    }

    public void uploadImage(File image, String imageName, boolean overwrite) {
        logger.debug("uploadAsset {} {} {}", image.getName(), imageName, overwrite);
        this.experimentKey.ifPresent(key -> {
            connection.sendPost(image, "/upload-image", new HashMap<String, String>() {{
                put("experimentKey", key);
                put("fileName", imageName);
                put("step", Long.toString(step));
                put("context", context);
                put("overwrite", Boolean.toString(overwrite));
            }});
        });
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
            obj.put("experimentKey", key);
            obj.put("stdoutLines", stdoutArray);
            obj.put("runContext", context);

            connection.sendPostAsync(obj.toString(), "/output");
        });
    }

    protected void pingStatus() {
        this.experimentKey.ifPresent(key -> {
            logger.debug("pingStatus");
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            connection.sendPostAsync(obj.toString(), "/experiment-status");
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
}
