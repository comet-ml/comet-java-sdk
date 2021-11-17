package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import ml.comet.experiment.Experiment;
import ml.comet.experiment.impl.constants.AssetType;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.model.AddGraphRest;
import ml.comet.experiment.model.AddTagsToExperimentRest;
import ml.comet.experiment.model.CreateGitMetadata;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.ExperimentTimeRequest;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.model.LogOtherRest;
import ml.comet.experiment.model.MetricRest;
import ml.comet.experiment.model.ParameterRest;
import ml.comet.experiment.model.ValueMinMaxDto;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_ASSET;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_GIT_METADATA;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_GRAPH;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_HTML;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_LOG_OTHER;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_METRIC;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_PARAMETER;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_START_END_TIME;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_TAG;
import static ml.comet.experiment.impl.constants.AssetType.ASSET_TYPE_SOURCE_CODE;
import static ml.comet.experiment.impl.constants.QueryParamName.CONTEXT;
import static ml.comet.experiment.impl.constants.QueryParamName.EPOCH;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.FILE_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.OVERWRITE;
import static ml.comet.experiment.impl.constants.QueryParamName.STEP;
import static ml.comet.experiment.impl.constants.QueryParamName.TYPE;


/**
 * The base class for all experiment implementations providing implementation of common routines.
 */
@RequiredArgsConstructor
public abstract class BaseExperiment implements Experiment {

    /**
     * Gets the current context as recorded in the Experiment object locally.
     *
     * @return the current context which associated with log records of this experiment.
     * TODO: 03.11.2021 this can be made Optional
     */
    protected abstract String getContext();

    /**
     * Returns connection associated with particular experiment. The subclasses must override this method to provide
     * relevant connection instance.
     *
     * @return the initialized connection associated with particular experiment.
     */
    protected abstract Connection getConnection();

    /**
     * Returns Comet REST API client. The subclasses must override this method to provide relevant connection instance.
     *
     * @return the initialized instance of the {@link RestApiClient}
     */
    protected abstract RestApiClient getRestApiClient();

    /**
     * Returns logger instance associated with particular experiment. The subclasses should override this method to
     * provide specific logger instance.
     *
     * @return the logger instance associated with particular experiment.
     */
    protected abstract Logger getLogger();

    @Override
    public void setExperimentName(@NonNull String experimentName) {
        logOther("Name", experimentName);
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, long step, long epoch) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetric {} = {}, step: {}, epoch: {}", metricName, metricValue, step, epoch);
        }
        validateExperimentKeyPresent();

        MetricRest request = getLogMetricRequest(metricName, metricValue, step, epoch);
        getConnection().sendPostAsync(request, ADD_METRIC);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameter {} = {}, step: {}", parameterName, paramValue, step);
        }
        validateExperimentKeyPresent();

        ParameterRest request = getLogParameterRequest(parameterName, paramValue, step);
        getConnection().sendPostAsync(request, ADD_PARAMETER);
    }

    @Override
    public void logHtml(@NonNull String html, boolean override) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logHtml {}, override: {}", html, override);
        }
        validateExperimentKeyPresent();

        HtmlRest request = getLogHtmlRequest(html, override);
        getConnection().sendPostAsync(request, ADD_HTML);
    }

    @Override
    public void logCode(@NonNull String code, @NonNull String fileName) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("log raw source code, file name: {}", fileName);
        }

        validateExperimentKeyPresent();

        Map<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put(FILE_NAME, fileName);
            put(CONTEXT, getContext());
            put(TYPE, ASSET_TYPE_SOURCE_CODE.type());
            put(OVERWRITE, Boolean.toString(false));
        }};

        getConnection().sendPostAsync(code.getBytes(StandardCharsets.UTF_8), ADD_ASSET, params)
                .toCompletableFuture()
                .exceptionally(t -> {
                    getLogger().error("failed to log raw source code with file name {}", fileName, t);
                    return null;
                });
    }

    @Override
    public void logCode(@NonNull File asset) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("log source code from file {}", asset.getName());
        }
        validateExperimentKeyPresent();

        Map<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put(FILE_NAME, asset.getName());
            put(CONTEXT, getContext());
            put(TYPE, ASSET_TYPE_SOURCE_CODE.type());
            put(OVERWRITE, Boolean.toString(false));
        }};

        getConnection().sendPostAsync(asset, ADD_ASSET, params)
                .toCompletableFuture()
                .exceptionally(t -> {
                    getLogger().error("failed to log source code from file {}", asset, t);
                    return null;
                });
    }

    @Override
    public void logOther(@NonNull String key, @NonNull Object value) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logOther {} {}", key, value);
        }
        validateExperimentKeyPresent();

        LogOtherRest request = getLogOtherRequest(key, value);
        getConnection().sendPostAsync(request, ADD_LOG_OTHER);
    }

    @Override
    public void addTag(@NonNull String tag) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logTag {}", tag);
        }
        validateExperimentKeyPresent();

        AddTagsToExperimentRest request = getTagRequest(tag);
        getConnection().sendPostAsync(request, ADD_TAG);
    }

    @Override
    public void logGraph(@NonNull String graph) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logOther {}", graph);
        }
        validateExperimentKeyPresent();

        AddGraphRest request = getGraphRequest(graph);
        getConnection().sendPostAsync(request, ADD_GRAPH);
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logStartTime {}", startTimeMillis);
        }
        validateExperimentKeyPresent();

        ExperimentTimeRequest request = getLogStartTimeRequest(startTimeMillis);
        getConnection().sendPostAsync(request, ADD_START_END_TIME);
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logEndTime {}", endTimeMillis);
        }
        validateExperimentKeyPresent();

        ExperimentTimeRequest request = getLogEndTimeRequest(endTimeMillis);
        getConnection().sendPostAsync(request, ADD_START_END_TIME);
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite, long step, long epoch) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("uploadAsset from file {}, name {}, override {}, step {}, epoch {}",
                    asset.getName(), fileName, overwrite, step, epoch);
        }
        validateExperimentKeyPresent();

        getConnection()
                .sendPostAsync(asset, ADD_ASSET, new HashMap<QueryParamName, String>() {{
                    put(EXPERIMENT_KEY, getExperimentKey());
                    put(FILE_NAME, fileName);
                    put(STEP, Long.toString(step));
                    put(EPOCH, Long.toString(epoch));
                    put(CONTEXT, getContext());
                    put(OVERWRITE, Boolean.toString(overwrite));
                }})
                .toCompletableFuture()
                .exceptionally(t -> {
                    getLogger().error("failed to upload asset from file {} with name {}", asset, fileName, t);
                    return null;
                });
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite, long step, long epoch) {
        uploadAsset(asset, asset.getName(), overwrite, step, epoch);
    }

    @Override
    public void logGitMetadata(CreateGitMetadata gitMetadata) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("gitMetadata {}", gitMetadata);
        }
        validateExperimentKeyPresent();

        getConnection().sendPostAsync(gitMetadata, ADD_GIT_METADATA);
    }

    @Override
    public ExperimentMetadataRest getMetadata() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get metadata for experiment {}", getExperimentKey());
        }
        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> getRestApiClient().getMetadata(experimentKey))
                    .blockingGet();
        } catch (Exception ex) {
            getLogger().error("Failed to read experiment's metadata, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public GitMetadataRest getGitMetadata() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get git metadata for experiment {}", getExperimentKey());
        }

        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> getRestApiClient().getGitMetadata(experimentKey))
                    .blockingGet();
        } catch (Exception ex) {
            getLogger().error("Failed to read experiment's Git metadata, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public Optional<String> getHtml() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get html for experiment {}", getExperimentKey());
        }
        try {
            return Optional.ofNullable(
                    validateAndGetExperimentKey()
                            .concatMap(experimentKey -> getRestApiClient().getHtml(experimentKey))
                            .blockingGet()
                            .getHtml());
        } catch (Exception ex) {
            getLogger().error("Failed to read HTML for the experiment, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public Optional<String> getOutput() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get output for experiment {}", getExperimentKey());
        }

        try {
            return Optional.ofNullable(
                    validateAndGetExperimentKey()
                            .concatMap(experimentKey -> getRestApiClient().getOutput(experimentKey))
                            .blockingGet()
                            .getOutput());
        } catch (Exception ex) {
            getLogger().error("Failed to read StdOut for the experiment, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public Optional<String> getGraph() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get graph for experiment {}", getExperimentKey());
        }

        try {
            return Optional.ofNullable(
                    validateAndGetExperimentKey()
                            .concatMap(experimentKey -> getRestApiClient().getGraph(experimentKey))
                            .blockingGet()
                            .getGraph());
        } catch (Exception ex) {
            getLogger().error("Failed to read Graph for the experiment, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public List<ValueMinMaxDto> getParameters() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get params for experiment {}", getExperimentKey());
        }

        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> getRestApiClient().getParameters(experimentKey))
                    .blockingGet()
                    .getValues();
        } catch (Exception ex) {
            getLogger().error("Failed to read parameters for the experiment, experiment key: {}",
                    getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public List<ValueMinMaxDto> getMetrics() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get metrics summary for experiment {}", getExperimentKey());
        }

        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> getRestApiClient().getMetrics(experimentKey))
                    .blockingGet()
                    .getValues();
        } catch (Exception ex) {
            getLogger().error("Failed to read metrics for the experiment, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public List<ValueMinMaxDto> getLogOther() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get log other for experiment {}", getExperimentKey());
        }

        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> getRestApiClient().getLogOther(experimentKey))
                    .blockingGet()
                    .getValues();
        } catch (Exception ex) {
            getLogger().error("Failed to read other parameters for the experiment, experiment key: {}",
                    getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public List<String> getTags() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get tags for experiment {}", getExperimentKey());
        }

        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> getRestApiClient().getTags(experimentKey))
                    .blockingGet()
                    .getTags();
        } catch (Exception ex) {
            getLogger().error("Failed to read TAGs for the experiment, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public List<ExperimentAssetLink> getAssetList(@NonNull AssetType type) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get assets with type {} for experiment {}", type, getExperimentKey());
        }

        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> getRestApiClient().getAssetList(experimentKey, type))
                    .blockingGet()
                    .getAssets();
        } catch (Exception ex) {
            getLogger().error("Failed to read ASSETS for the experiment, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    void end(Duration cleaningTimeout) {
        getLogger().info("Waiting for all scheduled uploads to complete. It can take up to {} seconds.",
                cleaningTimeout.getSeconds());

        // close connection
        Connection connection = this.getConnection();
        if (connection != null) {
            try {
                connection.waitAndClose(cleaningTimeout);
            } catch (Exception e) {
                getLogger().error("failed to close connection", e);
            }
        }
    }

    private String getObjectValue(Object val) {
        return val.toString();
    }

    private void validateExperimentKeyPresent() {
        if (getExperimentKey() == null) {
            throw new IllegalStateException("Experiment key must be present!");
        }
    }

    private Single<String> validateAndGetExperimentKey() {
        if (getExperimentKey() == null) {
            return Single.error(new IllegalStateException("Experiment key must be present!"));
        }
        return Single.just(getExperimentKey());
    }

    private MetricRest getLogMetricRequest(@NonNull String metricName, @NonNull Object metricValue,
                                           long step, long epoch) {
        MetricRest request = new MetricRest();
        request.setExperimentKey(getExperimentKey());
        request.setMetricName(metricName);
        request.setMetricValue(getObjectValue(metricValue));
        request.setStep(step);
        request.setEpoch(epoch);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private ParameterRest getLogParameterRequest(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        ParameterRest request = new ParameterRest();
        request.setExperimentKey(getExperimentKey());
        request.setParameterName(parameterName);
        request.setParameterValue(getObjectValue(paramValue));
        request.setStep(step);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private HtmlRest getLogHtmlRequest(@NonNull String html, boolean override) {
        HtmlRest request = new HtmlRest();
        request.setExperimentKey(getExperimentKey());
        request.setHtml(html);
        request.setOverride(override);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private LogOtherRest getLogOtherRequest(@NonNull String key, @NonNull Object value) {
        LogOtherRest request = new LogOtherRest();
        request.setExperimentKey(getExperimentKey());
        request.setKey(key);
        request.setValue(getObjectValue(value));
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private AddTagsToExperimentRest getTagRequest(@NonNull String tag) {
        AddTagsToExperimentRest request = new AddTagsToExperimentRest();
        request.setExperimentKey(getExperimentKey());
        request.setAddedTags(Collections.singletonList(tag));
        return request;
    }

    private AddGraphRest getGraphRequest(@NonNull String graph) {
        AddGraphRest request = new AddGraphRest();
        request.setExperimentKey(getExperimentKey());
        request.setGraph(graph);
        return request;
    }

    private ExperimentTimeRequest getLogStartTimeRequest(long startTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setExperimentKey(getExperimentKey());
        request.setStartTimeMillis(startTimeMillis);
        return request;
    }

    private ExperimentTimeRequest getLogEndTimeRequest(long endTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setExperimentKey(getExperimentKey());
        request.setEndTimeMillis(endTimeMillis);
        return request;
    }
}
