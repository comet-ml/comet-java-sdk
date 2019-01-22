package com.comet.experiment;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

public interface Experiment {
    /**
     * Tells Comet that the Experiment is complete.
     */
    void exit();

    /**
     * Turn on intercept of stdout and stderr and the logging of both in Comet
     * @throws IOException
     */
    void setInterceptStdout() throws IOException;

    /**
     * Turn off intercept of stdout and stderr and turns off their logging in Comet
     */
    void stopInterceptStdout();

    /**
     * Sets the current step for the experiment.  This is used when logging metrics and params
     * @param step
     */
    void setStep(long step);

    /**
     * Increments the step that the experiment is on
     */
    void nextStep();

    /**
     * Gets the current step as recorded by the Experiment object locally
     * @return
     */
    long getStep();

    /**
     * Sets the context for any logs and uploaded files
     * @param context
     */
    void setContext(String context);

    /**
     * Gets the current context as recorded in the Experiment object locally
     * @return
     */
    String getContext();

    /**
     * Get the experiment key returned by Comet
     * @return
     */
    String getExperimentKey();

    /**
     * Get the experiment link returned by Comet.  Empty in the case of continuing an existing experiment
     * @return
     */
    Optional<String> getExperimentLink();

    /**
     * Sets the experiment name
     * @param experimentName The new name for the experiment
     */
    void setExperimentName(String experimentName);

    /**
     * Logs a metric with Comet.  Metrics are generally values that change from step to step
     * @param metricName The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     */
    void logMetric(String metricName, String metricValue);

    /**
     * Logs a metric with Comet.  Metrics are generally values that change from step to step
     * @param metricName The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step The current step for this metric, this will set the given step for this experiment
     */
    void logMetric(String metricName, String metricValue, long step);


    /**
     * Logs a param with Comet.  Params should be set at the start of the experiment
     * @param parameterName The name of the param being logged
     * @param paramValue The value for the param being logged
     */
    void logParameter(String parameterName, String paramValue);

    /**
     * Logs a param with Comet.  Params should be set at the start of the experiment
     * @param parameterName The name of the param being logged
     * @param paramValue The value for the param being logged
     * @param step The current step for this metric, this will set the given step for this experiment
     */
    void logParameter(String parameterName, String paramValue, long step);

    /**
     * Let's you create an html report for the experiment
     * @param html A block of html to be sent to Comet
     * @param override Whether previous html sent should be deleted.  If true the old html will be deleted.  If false, it will have the new html appended onto the end.
     */
    void logHtml(String html, boolean override);

    /**
     * Logs a key value pair with Comet.  This can be used for any sort of generic per experiment data you wish to track
     * @param key The key for the data to be stored
     * @param value The value for said key
     */
    void logOther(String key, String value);

    /**
     * Logs a graph to Comet
     * @param graph The graph to be logged
     */
    void logGraph(String graph);

    /**
     * Logs the start time of the experiment
     * @param startTimeMillis When you want to say that the experiment started
     */
    void logStartTime(long startTimeMillis);

    /**
     * Logs the start time of the experiment
     * @param endTimeMillis When you want to say that the experiment ended
     */
    void logEndTime(long endTimeMillis);

    /**
     * Upload an asset to be associated with the experiment, for example the trained weights of a neural net
     * @param asset The asset to be stored
     * @param fileName The file name under which the asset should be stored in Comet
     * @param overwrite Whether to overwrite files of the same name in Comet
     */
    void uploadAsset(File asset, String fileName, boolean overwrite);

    /**
     * Upload an image to be associated with the experiment
     * @param image The image to be stored
     * @param imageName The file name under which the image should be stored in Comet
     * @param overwrite Whether to overwrite files of the same name in Comet
     */
    void uploadImage(File image, String imageName, boolean overwrite);
}
