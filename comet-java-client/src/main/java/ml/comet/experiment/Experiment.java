package ml.comet.experiment;

import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.model.Curve;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.GitMetaData;
import ml.comet.experiment.model.Value;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link Experiment} is a unit of measurable research that defines a single run with
 * some data/parameters/code/results.
 *
 * <p>Creating an {@link Experiment} object in your code will report a new experiment to
 * your Comet.ml project.
 *
 * <p>Your {@link Experiment} will automatically track and collect many things and will also
 * allow you to manually report anything.
 */
public interface Experiment extends AutoCloseable {

    /**
     * Get the experiment key returned by Comet.
     *
     * @return the experiment key assigned by Comet
     */
    String getExperimentKey();

    /**
     * Get the experiment link returned by Comet.
     *
     * @return the experiment link assigned by Comet.
     */
    Optional<String> getExperimentLink();

    /**
     * Get project name of the experiment.
     *
     * @return the project name of the experiment.
     */
    String getProjectName();

    /**
     * Get workspace name of the experiment.
     *
     * @return the workspace name of the experiment.
     */
    String getWorkspaceName();

    /**
     * Get experiment name.
     *
     * @return the name of the experiment.
     */
    String getExperimentName();


    /**
     * Get experiment metadata.
     *
     * @return the metadata associated with experiment.
     */
    ExperimentMetadata getMetadata();

    /**
     * Get experiment git metadata.
     *
     * @return the GIT metadata for this experiment.
     */
    GitMetaData getGitMetadata();

    /**
     * Get experiment html.
     *
     * @return the HTML of the experiment.
     */
    Optional<String> getHtml();

    /**
     * Get experiment output data.
     *
     * @return the output data of the experiment.
     */
    Optional<String> getOutput();

    /**
     * Get experiment graph data.
     *
     * @return the graph data associated with experiment.
     */
    Optional<String> getGraph();

    /**
     * Get experiment parameters.
     *
     * @return the parameters logged by experiment.
     */
    List<Value> getParameters();

    /**
     * Get experiment metrics.
     *
     * @return the metrics logged by experiment.
     */
    List<Value> getMetrics();

    /**
     * Get experiment log other data.
     *
     * @return the other data logged with experiment.
     */
    List<Value> getLogOther();

    /**
     * Get experiment tags.
     *
     * @return the TAGs associated with experiment
     */
    List<String> getTags();

    /**
     * Get list of the logged experiment assets with particular type.
     *
     * @param type the type of assets to be included.
     * @return the list of assets associated with experiment.
     */
    List<LoggedExperimentAsset> getAssetList(String type);

    /**
     * Get list of all logged experiment assets.
     *
     * @return the list of assets associated with experiment.
     */
    List<LoggedExperimentAsset> getAllAssetList();

    /**
     * Allows looking for logged artifact using provided search parameters.
     *
     * @param name           the short name of the artifact like {@code 'artifact-name'}.
     * @param workspace      the workspace where artifact was saved.
     * @param versionOrAlias the artifact version or alias to be returned.
     * @return the {@link LoggedArtifact} instance.
     * @throws ArtifactException if failed to get comet artifact.
     */
    LoggedArtifact getArtifact(String name, String workspace, String versionOrAlias) throws ArtifactException;

    /**
     * Allows looking for logged artifact using provided search parameters.
     *
     * @param name      the short name of the artifact like {@code 'artifact-name'} or
     *                  the name of artifact with version or alias like {@code 'artifact-name:versionOrAlias'}.
     * @param workspace the workspace where artifact was saved.
     * @return the {@link LoggedArtifact} instance.
     * @throws ArtifactException if failed to get comet artifact.
     */
    LoggedArtifact getArtifact(String name, String workspace) throws ArtifactException;

    /**
     * Allows looking for logged artifact using provided search parameters.
     *
     * @param name the name of the artifact. This could either be a fully
     *             qualified artifact name like {@code 'workspace/artifact-name:versionOrAlias'}
     *             or any short forms like {@code 'artifact-name'}, {@code 'artifact-name:versionOrAlias'}.
     * @return the {@link LoggedArtifact} instance.
     * @throws ArtifactException if failed to get comet artifact.
     */
    LoggedArtifact getArtifact(String name) throws ArtifactException;

    /**
     * Sets the experiment name.
     *
     * @param experimentName The new name for the experiment
     */
    void setExperimentName(String experimentName);

    /**
     * Send logs to Comet.
     *
     * @param line    Text to be logged
     * @param offset  Offset describes the place for current text to be inserted
     * @param stderr  the flag to indicate if this is StdErr message.
     * @param context the context to be associated with the parameter.
     */
    void logLine(String line, long offset, boolean stderr, String context);

    /**
     * Logs a metric with Comet. For running experiment updates current step to one from param!
     * Metrics are generally values that change from step to step.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them.
     * @param context     the context to be associated with the metric.
     */
    void logMetric(String metricName, Object metricValue, ExperimentContext context);

    /**
     * Logs a metric with Comet. For running experiment updates current step to one from param!
     * Metrics are generally values that change from step to step.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step        The current step for this metric, this will set the given step for this experiment
     * @param epoch       The current epoch for this metric, this will set the given epoch for this experiment.
     */
    void logMetric(String metricName, Object metricValue, long step, long epoch);

    /**
     * Logs a param with Comet. For running experiment updates current step to one from param!
     * Params should be set at the start of the experiment.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param context       the context to be associated with the parameter.
     */
    void logParameter(String parameterName, Object paramValue, ExperimentContext context);

    /**
     * Logs a param with Comet. For running experiment updates current step to one from param!
     * Params should be set at the start of the experiment.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param step          The current step for this metric, this will set the given step for this experiment.
     */
    void logParameter(String parameterName, Object paramValue, long step);

    /**
     * Allows you to create html report for the experiment.
     *
     * @param html     A block of html to be sent to Comet
     * @param override Whether previous html sent should be deleted.  If <code>true</code> the old html will be deleted.
     *                 If <code>false</code>, it will have the new html appended onto the end.
     */
    void logHtml(String html, boolean override);

    /**
     * Logs a key value pair with Comet.
     * This can be used for any sort of generic per experiment data you wish to track.
     *
     * @param key   The key for the data to be stored
     * @param value The value for said key
     */
    void logOther(String key, Object value);

    /**
     * Adds a tag to this experiment.
     *
     * @param tag The tag to be added
     */
    void addTag(String tag);

    /**
     * Logs a graph to Comet.
     *
     * @param graph The graph to be logged
     */
    void logGraph(String graph);

    /**
     * Logs the start time of the experiment.
     *
     * @param startTimeMillis When you want to say that the experiment started
     */
    void logStartTime(long startTimeMillis);

    /**
     * Logs the start time of the experiment.
     *
     * @param endTimeMillis When you want to say that the experiment ended
     */
    void logEndTime(long endTimeMillis);

    /**
     * Allows you to report code for the experiment.
     *
     * @param code        Code to be sent to Comet
     * @param logicalPath Name of source file to be displayed on UI 'code' tab
     * @param context     the context to be associated with the asset.
     */
    void logCode(String code, String logicalPath, ExperimentContext context);

    /**
     * Allows you to report code for the experiment.
     *
     * @param code        Code to be sent to Comet
     * @param logicalPath Name of source file to be displayed on UI 'code' tab
     */
    void logCode(String code, String logicalPath);

    /**
     * Allows you to report code for the experiment.
     *
     * @param file    Asset with source code to be sent
     * @param context the context to be associated with the asset.
     */
    void logCode(File file, ExperimentContext context);

    /**
     * Allows you to report code for the experiment.
     *
     * @param file Asset with source code to be sent
     */
    void logCode(File file);

    /**
     * Allows to log additional textual data associated with Comet Experiment.
     * These strings appear on the Text Tab in the Comet UI.
     *
     * @param text     the text data to be logged.
     * @param context  the current experiment context to be associated.
     * @param metadata the additional metadata for the text.
     */
    void logText(String text, ExperimentContext context, Map<String, Object> metadata);

    void logText(String text, ExperimentContext context);

    void logText(String text);

    /**
     * Allows to log x/y curve into your Comet experiment. This can be, for example, the time-series data, etc.
     *
     * @param curve     the {@code Curve} object holding the data points.
     * @param overwrite allows to override the previously logged curve with the same name.
     * @param context   the experiment context to be associated with data record (step, epoch, context ID).
     */
    void logCurve(Curve curve, boolean overwrite, ExperimentContext context);

    void logCurve(Curve curve, boolean overwrite);

    void logCurve(Curve curve);

    /**
     * Upload an asset to be associated with the experiment, for example the trained weights of a neural net.
     * For running experiment updates current step to one from param!
     *
     * @param asset       The asset to be stored
     * @param logicalPath The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite   Whether to overwrite files of the same name in Comet
     * @param context     the context to be associated with the asset.
     */
    void uploadAsset(File asset, String logicalPath, boolean overwrite, ExperimentContext context);

    /**
     * Upload an asset to be associated with the experiment, for example the trained weights of a neural net.
     * For running experiment updates current step to one from param!
     *
     * @param asset       The asset to be stored
     * @param logicalPath The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite   Whether to overwrite files of the same name in Comet
     * @param step        the step to be associated with the asset
     * @param epoch       the epoch to be associated with the asset
     */
    void uploadAsset(File asset, String logicalPath, boolean overwrite, long step, long epoch);

    /**
     * Upload an asset to be associated with the experiment, for example the trained weights of a neural net.
     * For running experiment updates current step to one from param!
     *
     * @param asset     The file asset to be stored. The name of the file will be used as assets identifier on Comet.
     * @param overwrite Whether to overwrite files of the same name in Comet
     * @param context   the context to be associated with the asset.
     */
    void uploadAsset(File asset, boolean overwrite, ExperimentContext context);

    /**
     * Upload an asset to be associated with the experiment, for example the trained weights of a neural net.
     * For running experiment updates current step to one from param!
     *
     * @param asset     The file asset to be stored. The name of the file will be used as assets identifier on Comet.
     * @param overwrite Whether to overwrite files of the same name in Comet
     * @param step      the step to be associated with the asset
     * @param epoch     the epoch to be associated with the asset
     */
    void uploadAsset(File asset, boolean overwrite, long step, long epoch);

    /**
     * Logs Git Metadata for the experiment.
     *
     * @param gitMetadata The Git Metadata for the experiment
     */
    void logGitMetadata(GitMetaData gitMetadata);

    /**
     * Tells Comet that the Experiment is complete and release all associated resources.
     */
    void end();
}
