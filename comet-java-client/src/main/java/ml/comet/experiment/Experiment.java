package ml.comet.experiment;

import ml.comet.experiment.model.CreateGitMetadata;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.ValueMinMaxDto;

import java.io.File;
import java.util.List;
import java.util.Optional;

public interface Experiment {

    /**
     * Get the experiment key returned by Comet
     * @return
     */
    String getExperimentKey();

    /**
     * Get the experiment link returned by Comet.
     * @return
     */
    Optional<String> getExperimentLink();

    /**
     * Get project name of the experiment.
     * @return
     */
    String getProjectName();

    /**
     * Get workspace name of the experiment.
     * @return
     */
    String getWorkspaceName();

    /**
     * Get experiment name.
     * @return
     */
    String getExperimentName();


    /**
     * Get experiment metadata.
     * @return
     */
    ExperimentMetadataRest getMetadata();

    /**
     * Get experiment git metadata.
     * @return
     */
    GitMetadataRest getGitMetadata();

    /**
     * Get experiment html.
     * @return
     */
    Optional<String> getHtml();

    /**
     * Get experiment output data.
     * @return
     */
    Optional<String> getOutput();

    /**
     * Get experiment graph data.
     * @return
     */
    Optional<String> getGraph();

    /**
     * Get experiment parameters.
     * @return
     */
    List<ValueMinMaxDto> getParameters();

    /**
     * Get experiment metrics.
     * @return
     */
    List<ValueMinMaxDto> getMetrics();

    /**
     * Get experiment log other data.
     * @return
     */
    List<ValueMinMaxDto> getLogOther();

    /**
     * Get experiment tags.
     * @return
     */
    List<String> getTags();

    /**
     * Get experiment asset list.
     * @return
     */
    List<ExperimentAssetLink> getAssetList(String type);

    /**
     * Sets the experiment name
     * @param experimentName The new name for the experiment
     */
    void setExperimentName(String experimentName);

    /**
     * Logs a metric with Comet. For running experiment updates current step to one from param!  Metrics are generally values that change from step to step
     * @param metricName The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step The current step for this metric, this will set the given step for this experiment
     * @param epoch The current epoch for this metric, this will set the given epoch for this experiment
     */
    void logMetric(String metricName, Object metricValue, long step, long epoch);


    /**
     * Logs a param with Comet. For running experiment updates current step to one from param! Params should be set at the start of the experiment
     * @param parameterName The name of the param being logged
     * @param paramValue The value for the param being logged
     * @param step The current step for this metric, this will set the given step for this experiment
     */
    void logParameter(String parameterName, Object paramValue, long step);

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
    void logOther(String key, Object value);

    /**
     * Adds a tag to this experiment.
     * @param tag The tag to be added
     */
    void addTag(String tag);

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
     * Let's you report code for the experiment.
     * @param code Code to be sent to Comet
     * @param fileName Name of source file to be displayed on UI 'code' tab
     */
    void logCode(String code, String fileName);

    /**
     * Let's you report code for the experiment.
     * @param file Asset with source code to be sent
     */
    void logCode(File file);

    /**
     * Upload an asset to be associated with the experiment, for example the trained weights of a neural net.
     * For running experiment updates current step to one from param!
     * @param asset The asset to be stored
     * @param fileName The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite Whether to overwrite files of the same name in Comet
     * @param step the step to be associated with asset
     * @param epoch the epoch to be associated with asset
     */
    void uploadAsset(File asset, String fileName, boolean overwrite, long step, long epoch);
    void uploadAsset(File asset, boolean overwrite, long step, long epoch);

    /**
     * Log Git Metadata for the experiment.
     * @param gitMetadata The Git Metadata for the experiment
     */
    void logGitMetadata(CreateGitMetadata gitMetadata);

}
