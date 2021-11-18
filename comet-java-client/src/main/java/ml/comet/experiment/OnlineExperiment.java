package ml.comet.experiment;

import java.io.File;
import java.io.IOException;

/**
 * Describes the public contract of the online experiment which extends functionality of the Experiment by providing
 * additional methods to log various parameters in real time.
 */
public interface OnlineExperiment extends Experiment {

    /**
     * Turn on intercept of stdout and stderr and the logging of both in Comet.
     *
     * @throws IOException if an I/O exception occurs.
     */
    void setInterceptStdout() throws IOException;

    /**
     * Turn off intercept of stdout and stderr and turns off their logging in Comet.
     *
     * @throws IOException if an I/O exception occurs.
     */
    void stopInterceptStdout() throws IOException;

    /**
     * Sets the current step for the experiment. This is used when logging metrics and params.
     *
     * @param step the current step of the experiment.
     */
    void setStep(long step);

    /**
     * Increments the step that the experiment is on.
     */
    void nextStep();

    /**
     * Gets the current step as recorded by the Experiment object locally.
     *
     * @return the current step of the experiment
     */
    long getStep();

    /**
     * Sets the current epoch for the experiment.
     *
     * @param epoch the current epoch for the experiment.
     */
    void setEpoch(long epoch);

    /**
     * Increments the epoch that the experiment is on.
     */
    void nextEpoch();

    /**
     * Gets the current epoch as recorded by the Experiment object locally.
     *
     * @return the current epoch of the experiment.
     */
    long getEpoch();

    /**
     * Sets the context for any logs and uploaded files.
     *
     * @param context the context to be associated with any log records, files, and assets.
     */
    void setContext(String context);

    /**
     * Gets the current context as recorded in the Experiment object locally.
     *
     * @return the current context which associated with log records of this experiment.
     */
    String getContext();

    /**
     * Logs a metric with Comet under the current experiment step.
     * Metrics are generally values that change from step to step.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them.
     * @param step        The step to be associated with this metric
     */
    void logMetric(String metricName, Object metricValue, long step);

    void logMetric(String metricName, Object metricValue);


    /**
     * Logs a param with Comet under the current experiment step.
     * Params should be set at the start of the experiment.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     */
    void logParameter(String parameterName, Object paramValue);

    /**
     * Upload an asset under the current experiment step to be associated with the experiment,
     * for example the trained weights of a neural net.
     *
     * @param asset     The asset to be stored
     * @param fileName  The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite Whether to overwrite files of the same name in Comet
     * @param step      The step to be associated with asset
     */
    void uploadAsset(File asset, String fileName, boolean overwrite, long step);

    void uploadAsset(File asset, String fileName, boolean overwrite);

    void uploadAsset(File asset, boolean overwrite);
}
