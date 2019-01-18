package com.comet.experiment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.HashMap;
import java.util.Optional;

public class Experiment {
    private Connection connection;
    private Optional<String> experimentKey = Optional.empty();

    private String projectName;
    private String workspace;
    private Optional<String> experimentName = Optional.empty();
    private Optional<StdOutLogger> stdOutLogger = Optional.empty();
    private Optional<StdOutLogger> stdErrLogger = Optional.empty();
    private boolean interceptStdout = false;
    private boolean debug = false;

    private long step = 0;
    private String context = "";

    private Experiment(String restApiKey, String projectName, String workspace, boolean debug) {
        this.projectName = projectName;
        this.workspace = workspace;
        this.debug = debug;
        this.connection = new Connection(restApiKey);
    }

    private Experiment() {}

    public static Experiment of(String apiKey, String projectName, String workspace) {
        return of(apiKey, projectName, workspace, false);
    }

    public static Experiment of(String apiKey, String projectName, String workspace, boolean debug) {
        Experiment experiment = new Experiment(apiKey, projectName, workspace, debug);
        boolean success = experiment.registerExperiment();
        if (!success) {
            throw new RuntimeException();
        }
        return experiment;
    }

    public static ExperimentBuilder builder(String projectName, String workspace) {
        return new ExperimentBuilder(projectName, workspace);
    }

    public static class ExperimentBuilder {
        Experiment experiment;

        public ExperimentBuilder(String projectName, String workspace) {
            this.experiment = new Experiment();
            this.experiment.projectName = projectName;
            this.experiment.workspace = workspace;
        }

        public ExperimentBuilder withRestApiKey(String restApiKey) {
            this.experiment.connection = new Connection(restApiKey);
            return this;
        }

        public ExperimentBuilder withExperimentName(String experimentName) {
            this.experiment.experimentName = Optional.of(experimentName);
            return this;
        }

        public ExperimentBuilder withDebug() {
            this.experiment.debug = true;
            return this;
        }

        public ExperimentBuilder withDebug(boolean debug) {
            this.experiment.debug = debug;
            return this;
        }

        public ExperimentBuilder interceptStdout() {
            this.experiment.interceptStdout = true;
            return this;
        }

        public Experiment build() throws IOException {
            if (this.experiment.interceptStdout) {
                this.experiment.captureStdout();
            }
            boolean success = this.experiment.registerExperiment();
            if (!success) {
                throw new RuntimeException();
            }
            return this.experiment;
        }
    }

    private boolean registerExperiment() {
        JSONObject obj = new JSONObject();
        obj.put("project_name", projectName);
        obj.put("workspace", workspace);
        this.experimentName.ifPresent(
                experiment -> obj.put("experiment_name", experiment));
        Optional<String> responseOptional = connection.sendPost(obj.toString(), "/new-experiment");

        responseOptional.ifPresent(response -> {
            JSONObject result = new JSONObject(response);
            if (result.has("experimentKey")) {
                this.experimentKey = Optional.ofNullable(result.getString("experimentKey"));
            }
        });
        return this.experimentKey.isPresent();
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

    public void logMetric(String metricName, String metricValue) {
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
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("html", html);
            obj.put("override", override);
            connection.sendPostAsync(obj.toString(), "/html");
        });
    }

    public void logOther(String key, String value) {
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
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("start_time_millis", startTimeMillis);
            connection.sendPostAsync(obj.toString(), "/experiment-start-end-time");
        });
    }

    public void logEndTime(long endTimeMillis) {
        this.experimentKey.ifPresent(key -> {
            JSONObject obj = new JSONObject();
            obj.put("experimentKey", key);
            obj.put("end_time_millis", endTimeMillis);
            connection.sendPostAsync(obj.toString(), "/experiment-start-end-time");
        });
    }

    public void uploadAsset(File asset, String fileName, boolean overwrite) {
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

    // Internal Experiment Logic Methods

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
}
