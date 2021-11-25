package ml.comet.experiment.impl.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.model.AddExperimentTagsRest;
import ml.comet.experiment.model.AddGraphRest;
import ml.comet.experiment.model.ExperimentTimeRequest;
import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.model.LogOtherRest;
import ml.comet.experiment.model.MetricRest;
import ml.comet.experiment.model.OutputLine;
import ml.comet.experiment.model.OutputUpdate;
import ml.comet.experiment.model.ParameterRest;

import java.util.Collections;

/**
 * The common factory methods to create initialized DTO instances.
 */
@UtilityClass
public class DataUtils {
    /**
     * The factory to create {@link MetricRest} instance.
     *
     * @param metricName  the metric name
     * @param metricValue the metric value
     * @param step        the current step of the experiment
     * @param epoch       the current epoch of the experiment
     * @param context     the current context
     * @return the initialized {@link MetricRest} instance.
     */
    public static MetricRest createLogMetricRequest(
            @NonNull String metricName, @NonNull Object metricValue, long step, long epoch, String context) {
        MetricRest request = new MetricRest();
        request.setMetricName(metricName);
        request.setMetricValue(metricValue.toString());
        request.setStep(step);
        request.setEpoch(epoch);
        request.setTimestamp(System.currentTimeMillis());
        request.setContext(context);
        return request;
    }

    /**
     * The factory to create {@link ParameterRest} instance.
     *
     * @param parameterName the name of the parameter
     * @param paramValue    the value of the parameter
     * @param step          the current experiment step
     * @param context       the current context
     * @return the initialized {@link ParameterRest} instance.
     */
    public static ParameterRest createLogParamRequest(
            @NonNull String parameterName, @NonNull Object paramValue, long step, String context) {
        ParameterRest request = new ParameterRest();
        request.setParameterName(parameterName);
        request.setParameterValue(paramValue.toString());
        request.setStep(step);
        request.setTimestamp(System.currentTimeMillis());
        request.setContext(context);
        return request;
    }

    /**
     * The factory to create {@link OutputUpdate} instance.
     *
     * @param line    the log line
     * @param offset  the log line offset
     * @param stderr  the flag to indicate if it's from StdErr
     * @param context the current context
     * @return the initialized {@link OutputUpdate} instance.
     */
    public static OutputUpdate createLogLineRequest(@NonNull String line, long offset, boolean stderr, String context) {
        OutputLine outputLine = new OutputLine();
        outputLine.setOutput(line);
        outputLine.setStderr(stderr);
        outputLine.setLocalTimestamp(System.currentTimeMillis());
        outputLine.setOffset(offset);

        OutputUpdate outputUpdate = new OutputUpdate();
        outputUpdate.setRunContext(context);
        outputUpdate.setOutputLines(Collections.singletonList(outputLine));
        return outputUpdate;
    }

    /**
     * The factory to create {@link HtmlRest} instance.
     *
     * @param html     the HTML code to be logged.
     * @param override the flag to indicate whether it should override already saved version.
     * @return the initialized {@link HtmlRest} instance.
     */
    public static HtmlRest createLogHtmlRequest(@NonNull String html, boolean override) {
        HtmlRest request = new HtmlRest();
        request.setHtml(html);
        request.setOverride(override);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    /**
     * The factory to create {@link LogOtherRest} instance.
     *
     * @param key   the parameter name/key.
     * @param value the parameter value.
     * @return the initialized {@link LogOtherRest} instance.
     */
    public static LogOtherRest createLogOtherRequest(@NonNull String key, @NonNull Object value) {
        LogOtherRest request = new LogOtherRest();
        request.setKey(key);
        request.setValue(value.toString());
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    /**
     * The factory to create {@link AddExperimentTagsRest} instance.
     *
     * @param tag the tag value
     * @return the initialized {@link AddExperimentTagsRest} instance
     */
    public static AddExperimentTagsRest createTagRequest(@NonNull String tag) {
        AddExperimentTagsRest request = new AddExperimentTagsRest();
        request.setAddedTags(Collections.singletonList(tag));
        return request;
    }

    /**
     * The factory to create {@link AddGraphRest} instance.
     *
     * @param graph the NN graph representation.
     * @return the initialized {@link AddGraphRest} instance.
     */
    public static AddGraphRest createGraphRequest(@NonNull String graph) {
        AddGraphRest request = new AddGraphRest();
        request.setGraph(graph);
        return request;
    }

    /**
     * The factory to create {@link ExperimentTimeRequest} instance.
     *
     * @param startTimeMillis the experiment's start time in milliseconds.
     * @return the initialized {@link ExperimentTimeRequest} instance.
     */
    public static ExperimentTimeRequest createLogStartTimeRequest(long startTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setStartTimeMillis(startTimeMillis);
        return request;
    }

    /**
     * The factory to create {@link ExperimentTimeRequest} instance.
     *
     * @param endTimeMillis the experiment's end time in milliseconds.
     * @return the initialized {@link ExperimentTimeRequest} instance.
     */
    public static ExperimentTimeRequest createLogEndTimeRequest(long endTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setEndTimeMillis(endTimeMillis);
        return request;
    }
}
