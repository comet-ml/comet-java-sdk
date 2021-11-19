package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ml.comet.experiment.Experiment;
import ml.comet.experiment.exception.CometApiException;
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
import ml.comet.experiment.model.ExperimentStatusResponse;
import ml.comet.experiment.model.ExperimentTimeRequest;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.model.LogDataResponse;
import ml.comet.experiment.model.LogOtherRest;
import ml.comet.experiment.model.MetricRest;
import ml.comet.experiment.model.OutputLine;
import ml.comet.experiment.model.OutputUpdate;
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
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_OUTPUT;
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

    String projectName;
    String workspaceName;
    String experimentKey;
    String experimentLink;
    String experimentName;
    boolean alive;

    @Setter
    @Getter
    long step;
    @Setter
    @Getter
    long epoch;
    @Getter
    @Setter
    private String context = StringUtils.EMPTY;

    private RestApiClient restApiClient;
    private Connection connection;
    private final CompositeDisposable disposables = new CompositeDisposable();

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
                   final String experimentKey,
                   @NonNull final Duration cleaningTimeout) {
        this(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, StringUtils.EMPTY, StringUtils.EMPTY);
    }

    BaseExperiment(@NonNull final String apiKey,
                   @NonNull final String baseUrl,
                   int maxAuthRetries,
                   final String experimentKey,
                   @NonNull final Duration cleaningTimeout,
                   final String projectName,
                   final String workspaceName) {
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
        this.alive = true;
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

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step        The current step for this metric, this will set the given step for this experiment
     * @param epoch       The current epoch for this metric, this will set the given epoch for this experiment
     * @throws CometApiException if received response with failure code.
     */
    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue, long step, long epoch) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetric {} = {}, step: {}, epoch: {}", metricName, metricValue, step, epoch);
        }
        LogDataResponse response = validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.logMetric(
                        withLogMetricRequest(metricName, metricValue, step, epoch, experimentKey)))
                .blockingGet();

        if (response.hasFailed()) {
            throw new CometApiException("Failed to save metric, reason: %s", response.getMsg());
        }
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step        The current step for this metric, this will set the given step for this experiment
     * @param epoch       The current epoch for this metric, this will set the given epoch for this experiment
     */
    void logMetricAsync(@NonNull String metricName, @NonNull Object metricValue, long step, long epoch) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetricAsync {} = {}, step: {}, epoch: {}", metricName, metricValue, step, epoch);
        }
        validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> restApiClient.logMetric(
                        withLogMetricRequest(metricName, metricValue, step, epoch, experimentKey)))
                .observeOn(Schedulers.single())
                .subscribe(
                        (logDataResponse) -> DataResponseLogger.checkAndLog(
                                logDataResponse, getLogger(), "failed to save metric {} = {}, step: {}, epoch: {}",
                                metricName, metricValue, step, epoch),
                        (throwable) -> getLogger().error("failed to save metric {} = {}, step: {}, epoch: {}",
                                metricName, metricValue, step, epoch, throwable),
                        disposables);
    }

    /**
     * Synchronous version that only logs any received exceptions or failures.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param step          The current step for this metric, this will set the given step for this experiment
     */
    @Override
    public void logParameter(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameter {} = {}, step: {}", parameterName, paramValue, step);
        }
        LogDataResponse response = validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.logParameter(
                        withLogParamRequest(parameterName, paramValue, step, experimentKey)))
                .blockingGet();

        if (response.hasFailed()) {
            throw new CometApiException("Failed to save parameter, reason: %s", response.getMsg());
        }
    }

    /**
     * Asynchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param step          The current step for this metric, this will set the given step for this experiment
     */
    void logParameterAsync(@NonNull String parameterName, @NonNull Object paramValue, long step) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameterAsync {} = {}, step: {}", parameterName, paramValue, step);
        }
        validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> restApiClient.logParameter(
                        withLogParamRequest(parameterName, paramValue, step, experimentKey)))
                .observeOn(Schedulers.single())
                .subscribe(
                        (logDataResponse) -> DataResponseLogger.checkAndLog(
                                logDataResponse, getLogger(), "failed to save parameter {} = {}, step: {}",
                                parameterName, paramValue, step),
                        (throwable) -> getLogger().error("failed to save parameter {} = {}, step: {}",
                                parameterName, paramValue, step, throwable),
                        disposables);
    }

    @Override
    public void logLine(String line, long offset, boolean stderr) {
        validate();

        OutputUpdate outputUpdate = getLogLineRequest(line, offset, stderr);
        this.connection.sendPostAsync(outputUpdate, ADD_OUTPUT);
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
            getLogger().debug("get metadata for experiment {}", this.experimentKey);
        }
        try {
            return validateAndGetExperimentKey()
                    .concatMap(experimentKey -> restApiClient.getMetadata(experimentKey))
                    .blockingGet();
        } catch (Exception ex) {
            getLogger().error("Failed to read experiment's metadata, experiment key: {}", this.experimentKey, ex);
            throw ex;
        }
    }

    @Override
    public GitMetadataRest getGitMetadata() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get git metadata for experiment {}", this.experimentKey);
        }

        return validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getGitMetadata(experimentKey))
                .doOnError(ex -> getLogger().error("Failed to read experiment's Git metadata, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet();
    }

    @Override
    public Optional<String> getHtml() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get html for experiment {}", this.experimentKey);
        }
        return Optional.ofNullable(validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getHtml(experimentKey))
                .doOnError(ex -> getLogger().error("Failed to read HTML for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getHtml());
    }

    @Override
    public Optional<String> getOutput() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get output for experiment {}", this.experimentKey);
        }

        return Optional.ofNullable(validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getOutput(experimentKey))
                .doOnError(ex -> getLogger().error("Failed to read StdOut for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getOutput());
    }

    @Override
    public Optional<String> getGraph() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get graph for experiment {}", this.experimentKey);
        }

        return Optional.ofNullable(validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getGraph(experimentKey))
                .doOnError(ex -> getLogger().error("Failed to read Graph for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getGraph());
    }

    @Override
    public List<ValueMinMaxDto> getParameters() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get params for experiment {}", this.experimentKey);
        }

        return validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getParameters(experimentKey))
                .doOnError(ex -> getLogger().error("Failed to read parameters for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getValues();
    }

    @Override
    public List<ValueMinMaxDto> getMetrics() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get metrics summary for experiment {}", this.experimentKey);
        }

        return validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getMetrics(experimentKey))
                .doOnError(ex -> getLogger().error("Failed to read metrics for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getValues();
    }

    @Override
    public List<ValueMinMaxDto> getLogOther() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get log other for experiment {}", this.experimentKey);
        }

        return validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getLogOther(experimentKey))
                .doOnError(ex -> getLogger().error(
                        "Failed to read other parameters for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getValues();
    }

    @Override
    public List<String> getTags() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get tags for experiment {}", this.experimentKey);
        }

        return validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getTags(experimentKey))
                .doOnError(ex -> getLogger().error("Failed to read TAGs for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getTags();
    }

    @Override
    public List<ExperimentAssetLink> getAssetList(@NonNull AssetType type) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get assets with type {} for experiment {}", type, this.experimentKey);
        }

        return validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getAssetList(experimentKey, type))
                .doOnError(ex -> getLogger().error("Failed to read ASSETS for the experiment, experiment key: {}",
                        this.experimentKey, ex))
                .blockingGet()
                .getAssets();
    }

    @Override
    public void end() {
        if (!this.alive) {
            return;
        }
        getLogger().info("Waiting for all scheduled uploads to complete. It can take up to {} seconds.",
                cleaningTimeout.getSeconds());

        // mark as not alive
        this.alive = false;

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

        // dispose all pending calls
        if (disposables.size() > 0) {
            getLogger().warn("{} calls still has not been processed, disposing", disposables.size());
        }
        this.disposables.dispose();
    }

    Optional<ExperimentStatusResponse> sendExperimentStatus() {
        return Optional.ofNullable(validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.sendExperimentStatus(experimentKey))
                .onErrorComplete()
                .blockingGet());
    }

    private static String getObjectValue(Object val) {
        return val.toString();
    }

    private void validate() {
        if (StringUtils.isEmpty(this.experimentKey)) {
            throw new IllegalStateException("Experiment key must be present!");
        }
        if (!this.alive) {
            throw new IllegalStateException("Experiment was not initialized. You need to call init().");
        }
    }

    private Single<String> validateAndGetExperimentKey() {
        if (StringUtils.isEmpty(this.experimentKey)) {
            return Single.error(new IllegalStateException("Experiment key must be present!"));
        }
        if (!this.alive) {
            return Single.error(new IllegalStateException("Experiment is not alive or already closed."));
        }
        return Single.just(getExperimentKey());
    }

    static MetricRest withLogMetricRequest(
            @NonNull String metricName, @NonNull Object metricValue,
            long step, long epoch, @NonNull String experimentKey) {
        MetricRest request = new MetricRest();
        request.setExperimentKey(experimentKey);
        request.setMetricName(metricName);
        request.setMetricValue(getObjectValue(metricValue));
        request.setStep(step);
        request.setEpoch(epoch);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    static ParameterRest withLogParamRequest(
            @NonNull String parameterName, @NonNull Object paramValue,
            long step, @NonNull String experimentKey) {
        ParameterRest request = new ParameterRest();
        request.setExperimentKey(experimentKey);
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

    private OutputUpdate getLogLineRequest(@NonNull String line, long offset, boolean stderr) {
        OutputLine outputLine = new OutputLine();
        outputLine.setOutput(line);
        outputLine.setStderr(stderr);
        outputLine.setLocalTimestamp(System.currentTimeMillis());
        outputLine.setOffset(offset);

        OutputUpdate outputUpdate = new OutputUpdate();
        outputUpdate.setExperimentKey(getExperimentKey());
        outputUpdate.setRunContext(this.context);
        outputUpdate.setOutputLines(Collections.singletonList(outputLine));
        return outputUpdate;
    }

    /**
     * Utility class to log asynchronously received data responses.
     */
    static final class DataResponseLogger {
        static void checkAndLog(LogDataResponse logDataResponse, Logger logger, String format, Object... args) {
            if (logDataResponse.hasFailed()) {
                logger.error("{}, reason: {}", String.format(format, args), logDataResponse.getMsg());
            } else if (logger.isDebugEnabled()) {
                logger.debug("success {}", logDataResponse);
            }
        }
    }
}
