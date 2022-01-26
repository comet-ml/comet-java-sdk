package ml.comet.experiment.impl.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.ArtifactImpl;
import ml.comet.experiment.impl.RegistryModelImpl;
import ml.comet.experiment.impl.rest.AddExperimentTagsRest;
import ml.comet.experiment.impl.rest.AddGraphRest;
import ml.comet.experiment.impl.rest.ArtifactRequest;
import ml.comet.experiment.impl.rest.ArtifactVersionState;
import ml.comet.experiment.impl.rest.ExperimentTimeRequest;
import ml.comet.experiment.impl.rest.GitMetadataRest;
import ml.comet.experiment.impl.rest.HtmlRest;
import ml.comet.experiment.impl.rest.LogOtherRest;
import ml.comet.experiment.impl.rest.MetricRest;
import ml.comet.experiment.impl.rest.OutputLine;
import ml.comet.experiment.impl.rest.OutputUpdate;
import ml.comet.experiment.impl.rest.ParameterRest;
import ml.comet.experiment.impl.rest.RegistryModelCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelItemCreateRequest;
import ml.comet.experiment.model.GitMetaData;

import java.util.Collections;
import java.util.Map;

/**
 * The common factory methods to create initialized model DTO instances.
 */
@UtilityClass
public class DataModelUtils {
    /**
     * The factory to create {@link MetricRest} instance.
     *
     * @param metricName  the metric name
     * @param metricValue the metric value
     * @param context     the current context
     * @return the initialized {@link MetricRest} instance.
     */
    public static MetricRest createLogMetricRequest(
            @NonNull String metricName, @NonNull Object metricValue, @NonNull ExperimentContext context) {
        MetricRest request = new MetricRest();
        request.setMetricName(metricName);
        request.setMetricValue(metricValue.toString());
        request.setStep(context.getStep());
        request.setEpoch(context.getEpoch());
        request.setTimestamp(System.currentTimeMillis());
        request.setContext(context.getContext());
        return request;
    }

    /**
     * The factory to create {@link ParameterRest} instance.
     *
     * @param parameterName the name of the parameter
     * @param paramValue    the value of the parameter
     * @param context       the current context
     * @return the initialized {@link ParameterRest} instance.
     */
    public static ParameterRest createLogParamRequest(
            @NonNull String parameterName, @NonNull Object paramValue, @NonNull ExperimentContext context) {
        ParameterRest request = new ParameterRest();
        request.setParameterName(parameterName);
        request.setParameterValue(paramValue.toString());
        request.setStep(context.getStep());
        request.setTimestamp(System.currentTimeMillis());
        request.setContext(context.getContext());
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

    /**
     * The factory to create {@link GitMetadataRest} request instance.
     *
     * @param metaData the {@link GitMetaData} model object from public API.
     * @return the initialized {@link GitMetadataRest} instance.
     */
    public static GitMetadataRest createGitMetadataRequest(GitMetaData metaData) {
        GitMetadataRest g = new GitMetadataRest();
        g.setUser(metaData.getUser());
        g.setOrigin(metaData.getOrigin());
        g.setBranch(metaData.getBranch());
        g.setParent(metaData.getParent());
        g.setRoot(metaData.getRoot());
        return g;
    }

    /**
     * The factory to create {@link ArtifactRequest} instance to be used to upsert Comet artifact.
     *
     * @param artifact the {@link ArtifactImpl} instance.
     * @return the initialized {@link ArtifactRequest} instance.
     */
    public static ArtifactRequest createArtifactUpsertRequest(final ArtifactImpl artifact) {
        ArtifactRequest r = new ArtifactRequest();
        r.setArtifactName(artifact.getName());
        r.setArtifactType(artifact.getType());
        if (artifact.getSemanticVersion() != null) {
            r.setVersion(artifact.getSemanticVersion().getValue());
        }
        if (artifact.getAliases() != null && artifact.getAliases().size() > 0) {
            r.setAlias(artifact.getAliases().toArray(new String[0]));
        }
        if (artifact.getVersionTags() != null && artifact.getVersionTags().size() > 0) {
            r.setVersionTags(artifact.getVersionTags().toArray(new String[0]));
        }
        if (artifact.getMetadata() != null) {
            r.setVersionMetadata(JsonUtils.toJson(artifact.getMetadata()));
        }
        return r;
    }

    /**
     * Creates request to signal the state of the artifact version send operation.
     *
     * @param artifactVersionId the identifier of the artifact version.
     * @param state             the {@link ArtifactVersionState} signaling state of the operation.
     * @return the properly initialized {@link ArtifactRequest} instance.
     */
    public static ArtifactRequest createArtifactVersionStateRequest(
            String artifactVersionId, ArtifactVersionState state) {
        ArtifactRequest r = new ArtifactRequest();
        r.setArtifactVersionId(artifactVersionId);
        r.setState(state);
        return r;
    }

    /**
     * Prepares request to create new registry record of the model associated with particular experiment.
     *
     * @param model the {@link RegistryModelImpl} instance holding all required data.
     * @return the properly initialized instance of {@link RegistryModelCreateRequest}.
     */
    public static RegistryModelCreateRequest createRegistryModelCreateRequest(RegistryModelImpl model) {
        RegistryModelCreateRequest request = new RegistryModelCreateRequest();
        request.setExperimentModelId(model.getExperimentModelId());
        request.setRegistryModelName(model.getRegistryName());
        request.setVersion(model.getVersion());
        request.setPublic(model.isPublic());
        request.setComment(model.getComment());
        request.setDescription(model.getDescription());
        request.setStages(model.getStages());
        return request;
    }

    /**
     * Prepares request which can be used to update the existing registry record of the model associated
     * with particular experiment.
     *
     * @param model the {@link RegistryModelImpl} instance holding all required data.
     * @return the properly initialized instance of {@link RegistryModelItemCreateRequest}.
     */
    public static RegistryModelItemCreateRequest createRegistryModelItemCreateRequest(RegistryModelImpl model) {
        RegistryModelItemCreateRequest request = new RegistryModelItemCreateRequest();
        request.setExperimentModelId(model.getExperimentModelId());
        request.setRegistryModelName(model.getRegistryName());
        request.setVersion(model.getVersion());
        request.setComment(model.getComment());
        request.setStages(model.getStages());
        return request;
    }

    /**
     * Converts JSON encoded metadata into {@link Map} object.
     *
     * @param json the JSON encoded metadata string.
     * @return the instance of the {@link Map} object.
     */
    public Map<String, Object> metadataFromJson(String json) {
        return JsonUtils.fromJson(json, new TypeReference<Map<String, Object>>() {
        });
    }
}
