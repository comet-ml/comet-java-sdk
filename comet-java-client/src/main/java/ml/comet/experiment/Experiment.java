package ml.comet.experiment;

import ml.comet.experiment.impl.constants.AssetType;
import ml.comet.experiment.model.GitMetadata;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.ValueMinMaxDto;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Describes the public contract of the Experiment.
 */
public interface Experiment {

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
    ExperimentMetadataRest getMetadata();

    /**
     * Get experiment git metadata.
     *
     * @return the GIT metadata for this experiment.
     */
    GitMetadataRest getGitMetadata();

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
    List<ValueMinMaxDto> getParameters();

    /**
     * Get experiment metrics.
     *
     * @return the metrics logged by experiment.
     */
    List<ValueMinMaxDto> getMetrics();

    /**
     * Get experiment log other data.
     *
     * @return the other data logged with experiment.
     */
    List<ValueMinMaxDto> getLogOther();

    /**
     * Get experiment tags.
     *
     * @return the TAGs associated with experiment
     */
    List<String> getTags();

    /**
     * Get experiment asset list.
     *
     * @param type the type of assets to be included.
     * @return the list of assets associated with experiment.
     */
    List<ExperimentAssetLink> getAssetList(AssetType type);

    /**
     * Sets the experiment name.
     *
     * @param experimentName The new name for the experiment
     */
    void setExperimentName(String experimentName);

    /**
     * Send logs to Comet.
     *
     * @param line   Text to be logged
     * @param offset Offset describes the place for current text to be inserted
     * @param stderr the flag to indicate if this is StdErr message.
     */
    void logLine(String line, long offset, boolean stderr);

    /**
     * Logs a metric with Comet. For running experiment updates current step to one from param!
     * Metrics are generally values that change from step to step.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step        The current step for this metric, this will set the given step for this experiment
     * @param epoch       The current epoch for this metric, this will set the given epoch for this experiment
     */
    void logMetric(String metricName, Object metricValue, long step, long epoch);


    /**
     * Logs a param with Comet. For running experiment updates current step to one from param!
     * Params should be set at the start of the experiment.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param step          The current step for this metric, this will set the given step for this experiment
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
     * @param code     Code to be sent to Comet
     * @param fileName Name of source file to be displayed on UI 'code' tab
     */
    void logCode(String code, String fileName);

    /**
     * Allows you to report code for the experiment.
     *
     * @param file Asset with source code to be sent
     */
    void logCode(File file);

    /**
     * Upload an asset to be associated with the experiment, for example the trained weights of a neural net.
     * For running experiment updates current step to one from param!
     *
     * @param asset     The asset to be stored
     * @param fileName  The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite Whether to overwrite files of the same name in Comet
     * @param step      the step to be associated with asset
     * @param epoch     the epoch to be associated with asset
     */
    void uploadAsset(File asset, String fileName, boolean overwrite, long step, long epoch);

    void uploadAsset(File asset, boolean overwrite, long step, long epoch);

    /**
     * Logs Git Metadata for the experiment.
     *
     * @param gitMetadata The Git Metadata for the experiment
     */
    void logGitMetadata(GitMetadata gitMetadata);

    /**
     * Tells Comet that the Experiment is complete and release all associated resources.
     */
    void end();
}
