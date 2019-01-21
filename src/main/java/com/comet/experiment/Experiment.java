package com.comet.experiment;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public interface Experiment {
    void exit();
    void setInterceptStdout() throws IOException;
    void stopInterceptStdout();
    void setStep(long step);
    void nextStep();
    long getStep();
    void setContext(String context);
    String getContext();
    Optional<String> getExperimentKey();
    Optional<String> getExperimentLink();
    void setName(String name);
    void logMetric(String metricName, String metricValue);
    void logParam(String paramName, String paramValue);
    void logHtml(String html, boolean override);
    void logOther(String key, String value);
    void logGraph();
    void logStartTime(long startTimeMillis);
    void logEndTime(long endTimeMillis);
    void uploadAsset(File asset, String fileName, boolean overwrite);
    void uploadImage(File image, String imageName, boolean overwrite);
}
