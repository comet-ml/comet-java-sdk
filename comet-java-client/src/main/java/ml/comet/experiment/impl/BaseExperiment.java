package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ml.comet.experiment.Experiment;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.constants.ApiEndpoints;
import ml.comet.experiment.impl.constants.AssetType;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.http.ConnectionInitializer;
import ml.comet.experiment.impl.utils.CometUtils;
import ml.comet.experiment.impl.utils.JsonUtils;
import ml.comet.experiment.model.AddGraphRest;
import ml.comet.experiment.model.AddTagsToExperimentRest;
import ml.comet.experiment.model.CreateExperimentRequest;
import ml.comet.experiment.model.CreateExperimentResponse;
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
import org.apache.commons.lang3.StringUtils;
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
public abstract class BaseExperiment implements Experiment {
    final String apiKey;
    final String baseUrl;
    final int maxAuthRetries;
    final Duration cleaningTimeout;

    RestApiClient restApiClient;
    private Connection connection;

    final String projectName;
    final String workspaceName;
    String experimentKey;
    String experimentLink;
    String experimentName;
    boolean initialized;

    @Getter
    @Setter
    String context = StringUtils.EMPTY;
    @Setter
    @Getter
    long step;
    @Setter
    @Getter
    long epoch;

    /**
     * Returns logger instance associated with particular experiment. The subclasses should override this method to
     * provide specific logger instance.
     *
     * @return the logger instance associated with particular experiment.
     */
    protected abstract Logger getLogger();

    BaseExperiment(@NonNull final String apiKey,
                   @NonNull final String baseUrl,
                   int maxAuthRetries,
                   @NonNull final String experimentKey,
                   @NonNull final Duration cleaningTimeout) {
        this(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    BaseExperiment(@NonNull final String apiKey,
                   @NonNull final String baseUrl,
                   int maxAuthRetries,
                   @NonNull final String experimentKey,
                   @NonNull final Duration cleaningTimeout,
                   @NonNull final String projectName,
                   @NonNull final String workspaceName) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.maxAuthRetries = maxAuthRetries;
        this.experimentKey = experimentKey;
        this.cleaningTimeout = cleaningTimeout;
        this.projectName = projectName;
        this.workspaceName = workspaceName;
    }

    /**
     * Invoked to validate and initialize common fields used by all subclasses.
     */
    void init() {
        CometUtils.printCometSdkVersion();
        validateInitialParams();
        this.connection = ConnectionInitializer.initConnection(
                this.apiKey, this.baseUrl, this.maxAuthRetries, this.getLogger());
        this.restApiClient = new RestApiClient(this.connection);
        // mark as initialized
        this.initialized = true;
    }

    /**
     * Validates initial parameters and throws exception if validation failed.
     *
     * @throws IllegalArgumentException if validation failed.
     */
    private void validateInitialParams() throws IllegalArgumentException {
        if (StringUtils.isEmpty(apiKey)) {
            throw new IllegalArgumentException("API key is not specified!");
        }
        if (StringUtils.isNotEmpty(experimentKey)) {
            return;
        }
        if (StringUtils.isEmpty(projectName)) {
            throw new IllegalArgumentException("ProjectName is not specified!");
        }
        if (StringUtils.isEmpty(workspaceName)) {
            throw new IllegalArgumentException("Workspace name is not specified!");
        }
    }

    /**
     * Registers experiment at the Comet server.
     *
     * @throws CometGeneralException if failed to register experiment.
     */
    void registerExperiment() throws CometGeneralException {
        if (experimentKey != null) {
            getLogger().debug("Not registering a new experiment. Using previous experiment key {}", experimentKey);
            return;
        }

        CreateExperimentRequest request = new CreateExperimentRequest(workspaceName, projectName, getExperimentName());
        String body = JsonUtils.toJson(request);

        this.connection.sendPost(body, ApiEndpoints.NEW_EXPERIMENT, true)
                .ifPresent(response -> {
                    CreateExperimentResponse result = JsonUtils.fromJson(response, CreateExperimentResponse.class);
                    this.experimentKey = result.getExperimentKey();
                    this.experimentLink = result.getLink();

                    getLogger().info("Experiment is live on comet.ml " + this.experimentLink);
                });

        if (this.experimentKey == null) {
            throw new CometGeneralException("Failed to register onlineExperiment with Comet ML");
        }
    }

    @Override
    public String getExperimentKey() {
        return this.experimentKey;
    }

    @Override
    public String getProjectName() {
        return this.projectName;
    }

    @Override
    public String getWorkspaceName() {
        return this.workspaceName;
    }

    @Override
    public String getExperimentName() {
        return this.experimentName;
    }

    @Override
    public void setExperimentName(@NonNull String experimentName) {
        logOther("Name", experimentName);
        this.experimentName = experimentName;
    }

    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, long step, long epoch) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetric {} = {}, step: {}, epoch: {}", metricName, metricValue, step, epoch);
        }
        validate();

        MetricRest request = getLogMetricRequest(metricName, metricValue, step, epoch);
        this.connection.sendPostAsync(request, ADD_METRIC);
    }

    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameter {} = {}, step: {}", parameterName, paramValue, step);
        }
        validate();

        ParameterRest request = getLogParameterRequest(parameterName, paramValue, step);
        this.connection.sendPostAsync(request, ADD_PARAMETER);
    }

    @Override
    public void logHtml(@NonNull String html, boolean override) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logHtml {}, override: {}", html, override);
        }
        validate();

        HtmlRest request = getLogHtmlRequest(html, override);
        this.connection.sendPostAsync(request, ADD_HTML);
    }

    @Override
    public void logCode(@NonNull String code, @NonNull String fileName) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("log raw source code, file name: {}", fileName);
        }

        validate();

        Map<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put(FILE_NAME, fileName);
            put(CONTEXT, getContext());
            put(TYPE, ASSET_TYPE_SOURCE_CODE.type());
            put(OVERWRITE, Boolean.toString(false));
        }};

        this.connection.sendPostAsync(code.getBytes(StandardCharsets.UTF_8), ADD_ASSET, params)
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
        validate();

        Map<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put(FILE_NAME, asset.getName());
            put(CONTEXT, getContext());
            put(TYPE, ASSET_TYPE_SOURCE_CODE.type());
            put(OVERWRITE, Boolean.toString(false));
        }};

        this.connection.sendPostAsync(asset, ADD_ASSET, params)
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
        validate();

        LogOtherRest request = getLogOtherRequest(key, value);
        this.connection.sendPostAsync(request, ADD_LOG_OTHER);
    }

    @Override
    public void addTag(@NonNull String tag) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logTag {}", tag);
        }
        validate();

        AddTagsToExperimentRest request = getTagRequest(tag);
        this.connection.sendPostAsync(request, ADD_TAG);
    }

    @Override
    public void logGraph(@NonNull String graph) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logOther {}", graph);
        }
        validate();

        AddGraphRest request = getGraphRequest(graph);
        this.connection.sendPostAsync(request, ADD_GRAPH);
    }

    @Override
    public void logStartTime(long startTimeMillis) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logStartTime {}", startTimeMillis);
        }
        validate();

        ExperimentTimeRequest request = getLogStartTimeRequest(startTimeMillis);
        this.connection.sendPostAsync(request, ADD_START_END_TIME);
    }

    @Override
    public void logEndTime(long endTimeMillis) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logEndTime {}", endTimeMillis);
        }
        validate();

        ExperimentTimeRequest request = getLogEndTimeRequest(endTimeMillis);
        this.connection.sendPostAsync(request, ADD_START_END_TIME);
    }

    @Override
    public void uploadAsset(@NonNull File asset, @NonNull String fileName, boolean overwrite, long step, long epoch) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("uploadAsset from file {}, name {}, override {}, step {}, epoch {}",
                    asset.getName(), fileName, overwrite, step, epoch);
        }
        validate();

        this.connection
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
        validate();

        this.connection.sendPostAsync(gitMetadata, ADD_GIT_METADATA);
    }

    @Override
    public ExperimentMetadataRest getMetadata() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get metadata for experiment {}", getExperimentKey());
        }
        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> restApiClient.getMetadata(experimentKey))
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
                    .concatMap(experimentKey -> restApiClient.getGitMetadata(experimentKey))
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
                            .concatMap(experimentKey -> restApiClient.getHtml(experimentKey))
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
                            .concatMap(experimentKey -> restApiClient.getOutput(experimentKey))
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
                            .concatMap(experimentKey -> restApiClient.getGraph(experimentKey))
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
                    .concatMap(experimentKey -> restApiClient.getParameters(experimentKey))
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
                    .concatMap(experimentKey -> restApiClient.getMetrics(experimentKey))
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
                    .concatMap(experimentKey -> restApiClient.getLogOther(experimentKey))
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
                    .concatMap(experimentKey -> restApiClient.getTags(experimentKey))
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
                    .concatMap(experimentKey -> restApiClient.getAssetList(experimentKey, type))
                    .blockingGet()
                    .getAssets();
        } catch (Exception ex) {
            getLogger().error("Failed to read ASSETS for the experiment, experiment key: {}", getExperimentKey(), ex);
            throw ex;
        }
    }

    @Override
    public void end() {
        if (!this.initialized) {
            return;
        }
        getLogger().info("Waiting for all scheduled uploads to complete. It can take up to {} seconds.",
                cleaningTimeout.getSeconds());

        // close REST API
        this.restApiClient.dispose();

        // close connection
        if (this.connection != null) {
            try {
                this.connection.waitAndClose(this.cleaningTimeout);
                this.connection = null;
            } catch (Exception e) {
                getLogger().error("failed to close connection", e);
            }
        }

        // mark as not initialized
        this.initialized = false;
    }

    private String getObjectValue(Object val) {
        return val.toString();
    }

    private void validate() {
        if (getExperimentKey() == null) {
            throw new IllegalStateException("Experiment key must be present!");
        }
        if (!this.initialized) {
            throw new IllegalStateException("Experiment was not initialized. You need to call init().");
        }
    }

    private Single<String> validateAndGetExperimentKey() {
        if (getExperimentKey() == null) {
            return Single.error(new IllegalStateException("Experiment key must be present!"));
        }
        if (!this.initialized) {
            return Single.error(new IllegalStateException("Experiment was not initialized. You need to call init()."));
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
