package ml.comet.experiment;

import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.context.ExperimentContext;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * The {@code OnlineExperiment} should be used to asynchronously update data of your Comet.ml experiment.
 *
 * <p>This experiment type allows you to automatically intercept {@code StdOut} and {@code StdErr} streams and send
 * them to the Comet.ml. Use the {@link #setInterceptStdout()} to start automatic interception of {@code StdOut} and
 * the {@link #stopInterceptStdout()} to stop.
 *
 * <p>Also, it is possible to use {@link #setStep(long)}, {@link #setEpoch(long)},
 * and {@link #setContext(String)} which will bbe automatically associated with related logged data records.
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
     * Sets the context identifier for any logs and uploaded files.
     *
     * @param context the context identifier to be associated with any log records, files, and assets.
     */
    void setContext(String context);

    /**
     * Gets the current context identifier as recorded in the {@link OnlineExperiment} object locally.
     *
     * @return the current context which associated with log records of this experiment.
     */
    String getContext();

    /**
     * Logs a metric with Comet. For running experiment updates current step to one from param!
     * Metrics are generally values that change from step to step.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step        The current step for this metric, this will set the given step for this experiment
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
     * Send output logs to Comet.
     *
     * @param line   Text to be logged
     * @param offset Offset describes the place for current text to be inserted
     * @param stderr the flag to indicate if this is StdErr message.
     */
    void logLine(String line, long offset, boolean stderr);

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

    /**
     * Logs all the files located in the given folder as assets.
     *
     * @param folder      the folder you want to log.
     * @param logFilePath if {@code true}, log the file path with each file.
     * @param recursive   if {@code true}, recurse the folder.
     * @param context     the experiment context to be associated with the logged assets.
     */
    void logAssetFolder(File folder, boolean logFilePath, boolean recursive, ExperimentContext context);

    void logAssetFolder(File folder, boolean logFilePath, boolean recursive);

    void logAssetFolder(File folder, boolean logFilePath);

    /**
     * Logs a Remote Asset identified by a {@link URI}. A Remote Asset is an asset but its content is not uploaded
     * and stored on Comet. Rather a link for its location is stored, so you can identify and distinguish
     * between two experiment using different version of a dataset stored somewhere else.
     *
     * @param uri       the {@link URI} pointing to the remote asset location. There is no imposed format,
     *                  and it could be a private link.
     * @param fileName  the optional "name" of the remote asset, could be a dataset name, a model file name.
     * @param overwrite if {@code true} will overwrite all existing assets with the same name.
     * @param metadata  Some additional data to attach to the remote asset.
     *                  The dictionary values must be JSON compatible.
     * @param context   the experiment context to be associated with the logged assets.
     */
    void logRemoteAsset(URI uri, String fileName, boolean overwrite,
                        Map<String, Object> metadata, ExperimentContext context);

    void logRemoteAsset(URI uri, String fileName, boolean overwrite, Map<String, Object> metadata);

    void logRemoteAsset(URI uri, String fileName, boolean overwrite);

    void logRemoteAsset(URI uri, boolean overwrite);

    /**
     * Logs an {@link Artifact} object. First, it creates a new version of the artifact. After that, it uploads
     * asynchronously all the local and remote assets attached to the artifact object.
     *
     * @param artifact the {@link Artifact} instance.
     */
    void logArtifact(Artifact artifact);
}
