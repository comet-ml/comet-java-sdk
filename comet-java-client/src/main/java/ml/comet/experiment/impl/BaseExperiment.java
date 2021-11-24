package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ml.comet.experiment.Experiment;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.constants.AssetType;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.http.ConnectionInitializer;
import ml.comet.experiment.impl.utils.CometUtils;
import ml.comet.experiment.model.AddExperimentTagsRest;
import ml.comet.experiment.model.AddGraphRest;
import ml.comet.experiment.model.CreateExperimentRequest;
import ml.comet.experiment.model.CreateExperimentResponse;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.ExperimentStatusResponse;
import ml.comet.experiment.model.ExperimentTimeRequest;
import ml.comet.experiment.model.GitMetadata;
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
        if (StringUtils.isEmpty(this.apiKey)) {
            throw new IllegalArgumentException("API key is not specified!");
        }
        if (StringUtils.isEmpty(this.baseUrl)) {
            throw new IllegalArgumentException("The Comet base URL is not specified!");
        }
    }

    /**
     * Synchronously registers experiment at the Comet server.
     *
     * @throws CometGeneralException if failed to register experiment.
     */
    void registerExperiment() throws CometGeneralException {
        if (!StringUtils.isEmpty(this.experimentKey)) {
            getLogger().debug("Not registering a new experiment. Using previous experiment key {}", this.experimentKey);
            return;
        }

        // do synchronous call to register experiment
        CreateExperimentResponse result = this.restApiClient.registerExperiment(
                        new CreateExperimentRequest(this.workspaceName, this.projectName, this.experimentName))
                .blockingGet();
        this.experimentKey = result.getExperimentKey();
        this.experimentLink = result.getLink();

        getLogger().info("Experiment is live on comet.ml " + this.experimentLink);

        if (StringUtils.isEmpty(this.experimentKey)) {
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

        sendSynchronously(restApiClient::logMetric,
                createLogMetricRequest(metricName, metricValue, step, epoch, this.context));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step        The current step for this metric, this will set the given step for this experiment
     * @param epoch       The current epoch for this metric, this will set the given epoch for this experiment
     * @param onComplete  The optional action to be invoked when this operation asynchronously completes.
     *                    Can be {@code null} if not interested in completion signal.
     */
    void logMetricAsync(@NonNull String metricName, @NonNull Object metricValue,
                        long step, long epoch, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetricAsync {} = {}, step: {}, epoch: {}", metricName, metricValue, step, epoch);
        }

        MetricRest metricRequest = createLogMetricRequest(metricName, metricValue, step, epoch, this.context);
        this.sendAsynchronously(restApiClient::logMetric, metricRequest, onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
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

        sendSynchronously(restApiClient::logParameter,
                createLogParamRequest(parameterName, paramValue, step, this.context));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param step          The current step for this metric, this will set the given step for this experiment
     * @param onComplete    The optional action to be invoked when this operation asynchronously completes.
     *                      Can be {@code null} if not interested in completion signal.
     */
    void logParameterAsync(@NonNull String parameterName, @NonNull Object paramValue, long step, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameterAsync {} = {}, step: {}", parameterName, paramValue, step);
        }

        ParameterRest paramRequest = createLogParamRequest(parameterName, paramValue, step, this.context);
        this.sendAsynchronously(restApiClient::logParameter, paramRequest, onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param line   Text to be logged
     * @param offset Offset describes the place for current text to be inserted
     * @param stderr the flag to indicate if this is StdErr message.
     */
    @Override
    public void logLine(String line, long offset, boolean stderr) {
        validate();

        sendSynchronously(restApiClient::logOutputLine,
                createLogLineRequest(line, offset, stderr, this.context));
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param html     A block of html to be sent to Comet
     * @param override Whether previous html sent should be deleted.
     *                 If <code>true</code> the old html will be deleted.
     */
    @Override
    public void logHtml(@NonNull String html, boolean override) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logHtml {}, override: {}", html, override);
        }

        sendSynchronously(restApiClient::logHtml,
                createLogHtmlRequest(html, override));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param html       A block of html to be sent to Comet
     * @param override   Whether previous html sent should be deleted.
     *                   If <code>true</code> the old html will be deleted.
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logHtmlAsync(@NonNull String html, boolean override, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logHtmlAsync {}, override: {}", html, override);
        }

        HtmlRest htmlRequest = createLogHtmlRequest(html, override);
        this.sendAsynchronously(restApiClient::logHtml, htmlRequest, onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param key   The key for the data to be stored
     * @param value The value for said key
     */
    @Override
    public void logOther(@NonNull String key, @NonNull Object value) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logOther {} {}", key, value);
        }

        sendSynchronously(restApiClient::logOther, createLogOtherRequest(key, value));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param key        The key for the data to be stored
     * @param value      The value for said key
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logOtherAsync(@NonNull String key, @NonNull Object value, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logOtherAsync {} {}", key, value);
        }

        LogOtherRest request = createLogOtherRequest(key, value);
        sendAsynchronously(restApiClient::logOther, request, onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param tag The tag to be added
     */
    @Override
    public void addTag(@NonNull String tag) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("addTag {}", tag);
        }

        sendSynchronously(restApiClient::addTag, createTagRequest(tag));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param tag        The tag to be added
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    public void addTagAsync(@NonNull String tag, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("addTagAsync {}", tag);
        }

        sendAsynchronously(restApiClient::addTag, createTagRequest(tag), onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param graph The graph to be logged.
     */
    @Override
    public void logGraph(@NonNull String graph) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logGraph {}", graph);
        }

        sendSynchronously(restApiClient::logGraph, createGraphRequest(graph));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param graph      The graph to be logged
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logGraphAsync(@NonNull String graph, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logGraphAsync {}", graph);
        }

        sendAsynchronously(restApiClient::logGraph, createGraphRequest(graph), onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param startTimeMillis When you want to say that the experiment started
     */
    @Override
    public void logStartTime(long startTimeMillis) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logStartTime {}", startTimeMillis);
        }

        sendSynchronously(restApiClient::logStartEndTime, createLogStartTimeRequest(startTimeMillis));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param startTimeMillis When you want to say that the experiment started
     * @param onComplete      The optional action to be invoked when this operation asynchronously completes.
     *                        Can be {@code null} if not interested in completion signal.
     */
    void logStartTimeAsync(long startTimeMillis, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logStartTimeAsync {}", startTimeMillis);
        }

        sendAsynchronously(restApiClient::logStartEndTime, createLogStartTimeRequest(startTimeMillis), onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param endTimeMillis When you want to say that the experiment ended
     */
    @Override
    public void logEndTime(long endTimeMillis) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logEndTime {}", endTimeMillis);
        }

        sendSynchronously(restApiClient::logStartEndTime, createLogEndTimeRequest(endTimeMillis));
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param endTimeMillis When you want to say that the experiment ended
     * @param onComplete    The optional action to be invoked when this operation asynchronously completes.
     *                      Can be {@code null} if not interested in completion signal.
     */
    void logEndTimeAsync(long endTimeMillis, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logEndTimeAsync {}", endTimeMillis);
        }

        sendAsynchronously(restApiClient::logStartEndTime, createLogEndTimeRequest(endTimeMillis), onComplete);
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param gitMetadata The Git Metadata for the experiment.
     */
    @Override
    public void logGitMetadata(GitMetadata gitMetadata) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logGitMetadata {}", gitMetadata);
        }

        sendSynchronously(restApiClient::logGitMetadata, gitMetadata);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param gitMetadata The Git Metadata for the experiment.
     * @param onComplete  The optional action to be invoked when this operation asynchronously completes.
     *                    Can be {@code null} if not interested in completion signal.
     */
    void logGitMetadataAsync(GitMetadata gitMetadata, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logGitMetadata {}", gitMetadata);
        }

        sendAsynchronously(restApiClient::logGitMetadata, gitMetadata, onComplete);
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

        this.connection.sendPostAsync(code.getBytes(StandardCharsets.UTF_8), ADD_ASSET, params, null)
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

        this.connection.sendPostAsync(asset, ADD_ASSET, params, null)
                .toCompletableFuture()
                .exceptionally(t -> {
                    getLogger().error("failed to log source code from file {}", asset, t);
                    return null;
                });
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
                }}, null)
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
    public ExperimentMetadataRest getMetadata() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get metadata for experiment {}", this.experimentKey);
        }

        return loadRemote(restApiClient::getMetadata, "METADATA");
    }

    @Override
    public GitMetadataRest getGitMetadata() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get git metadata for experiment {}", this.experimentKey);
        }

        return loadRemote(restApiClient::getGitMetadata, "GIT METADATA");
    }

    @Override
    public Optional<String> getHtml() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get html for experiment {}", this.experimentKey);
        }

        return Optional.ofNullable(loadRemote(restApiClient::getHtml, "HTML").getHtml());
    }

    @Override
    public Optional<String> getOutput() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get output for experiment {}", this.experimentKey);
        }

        return Optional.ofNullable(loadRemote(restApiClient::getOutput, "StdOut").getOutput());
    }

    @Override
    public Optional<String> getGraph() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get graph for experiment {}", this.experimentKey);
        }

        return Optional.ofNullable(loadRemote(restApiClient::getGraph, "GRAPH").getGraph());
    }

    @Override
    public List<ValueMinMaxDto> getParameters() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get params for experiment {}", this.experimentKey);
        }

        return loadRemote(restApiClient::getParameters, "PARAMETERS").getValues();
    }

    @Override
    public List<ValueMinMaxDto> getMetrics() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get metrics summary for experiment {}", this.experimentKey);
        }

        return loadRemote(restApiClient::getMetrics, "METRICS").getValues();
    }

    @Override
    public List<ValueMinMaxDto> getLogOther() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get log other for experiment {}", this.experimentKey);
        }

        return loadRemote(restApiClient::getLogOther, "OTHER PARAMETERS").getValues();
    }

    @Override
    public List<String> getTags() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get tags for experiment {}", this.experimentKey);
        }

        return loadRemote(restApiClient::getTags, "TAGs").getTags();
    }

    @Override
    public List<ExperimentAssetLink> getAssetList(@NonNull AssetType type) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("get assets with type {} for experiment {}", type, this.experimentKey);
        }

        return validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.getAssetList(experimentKey, type))
                .doOnError(ex -> getLogger().error("Failed to read ASSETS list for the experiment, experiment key: {}",
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

    /**
     * Sends heartbeat to the server and returns the status response.
     *
     * @return the status response of the experiment.
     */
    Optional<ExperimentStatusResponse> sendExperimentStatus() {
        return Optional.ofNullable(validateAndGetExperimentKey()
                .concatMap(experimentKey -> restApiClient.sendExperimentStatus(experimentKey))
                .onErrorComplete()
                .blockingGet());
    }

    /**
     * Synchronously loads remote data using provided load function or throws an exception.
     *
     * @param loadFunc the function to be applied to load remote data.
     * @param alias    the data type alias used for logging.
     * @param <T>      the data type to be returned.
     * @return the loaded data.
     */
    private <T> T loadRemote(final Function<String, Single<T>> loadFunc, String alias) {
        return validateAndGetExperimentKey()
                .concatMap(loadFunc)
                .doOnError(ex -> getLogger().error("Failed to read {} for the experiment, experiment key: {}",
                        alias, this.experimentKey, ex))
                .blockingGet();
    }

    /**
     * Uses provided function to send request data asynchronously and log received output. Optionally, can use
     * provided {@link Action} handler to notify about completion of the operation.
     *
     * @param func       the function to be invoked to send request data.
     * @param request    the request data object.
     * @param onComplete the optional {@link Action} to be notified the operation completes either
     *                   successfully or erroneously.
     * @param <T>        the type of the request data object.
     */
    private <T> void sendAsynchronously(final BiFunction<T, String, Single<LogDataResponse>> func,
                                        final T request, final Action onComplete) {
        Single<LogDataResponse> single = validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> func.apply(request, experimentKey));

        // register notification action if provided
        if (onComplete != null) {
            single = single.doFinally(onComplete);
        }

        // subscribe to receive operation results
        single
                .observeOn(Schedulers.single())
                .subscribe(
                        (logDataResponse) -> DataResponseLogger.checkAndLog(logDataResponse, getLogger(), request),
                        (throwable) -> getLogger().error("failed to log {}", request, throwable),
                        disposables);
    }

    /**
     * Uses provided function to send request data synchronously. If response indicating the remote error
     * received the {@link CometApiException} will be thrown.
     *
     * @param func    the function to be invoked to send request data.
     * @param request the request data object.
     * @param <T>     the type of the request data object.
     * @throws CometApiException if received response with error indicating that data was not saved.
     */
    private <T> void sendSynchronously(final BiFunction<T, String, Single<LogDataResponse>> func,
                                       final T request) throws CometApiException {
        LogDataResponse response = validateAndGetExperimentKey()
                .concatMap(experimentKey -> func.apply(request, experimentKey))
                .blockingGet();

        if (response.hasFailed()) {
            throw new CometApiException("Failed to log {}, reason: %s", request, response.getMsg());
        }
    }

    /**
     * Validates the state of the experiment.
     *
     * @throws IllegalStateException if current state of the experiment is wrong, i.e., no experiment key found or
     *                               experiment already ended.
     */
    private void validate() throws IllegalStateException {
        if (StringUtils.isEmpty(this.experimentKey)) {
            throw new IllegalStateException("Experiment key must be present!");
        }
        if (!this.alive) {
            throw new IllegalStateException("Experiment was not initialized. You need to call init().");
        }
    }

    /**
     * Validates the experiment state and return the experiment key or error as a {@link Single}.
     *
     * @return the experiment key or error as {@link Single}.
     */
    private Single<String> validateAndGetExperimentKey() {
        if (StringUtils.isEmpty(this.experimentKey)) {
            return Single.error(new IllegalStateException("Experiment key must be present!"));
        }
        if (!this.alive) {
            return Single.error(new IllegalStateException("Experiment is not alive or already closed."));
        }
        return Single.just(getExperimentKey());
    }

    static MetricRest createLogMetricRequest(
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

    static ParameterRest createLogParamRequest(
            @NonNull String parameterName, @NonNull Object paramValue, long step, String context) {
        ParameterRest request = new ParameterRest();
        request.setParameterName(parameterName);
        request.setParameterValue(paramValue.toString());
        request.setStep(step);
        request.setTimestamp(System.currentTimeMillis());
        request.setContext(context);
        return request;
    }

    private HtmlRest createLogHtmlRequest(@NonNull String html, boolean override) {
        HtmlRest request = new HtmlRest();
        request.setHtml(html);
        request.setOverride(override);
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private LogOtherRest createLogOtherRequest(@NonNull String key, @NonNull Object value) {
        LogOtherRest request = new LogOtherRest();
        request.setKey(key);
        request.setValue(value.toString());
        request.setTimestamp(System.currentTimeMillis());
        return request;
    }

    private AddExperimentTagsRest createTagRequest(@NonNull String tag) {
        AddExperimentTagsRest request = new AddExperimentTagsRest();
        request.setAddedTags(Collections.singletonList(tag));
        return request;
    }

    private AddGraphRest createGraphRequest(@NonNull String graph) {
        AddGraphRest request = new AddGraphRest();
        request.setGraph(graph);
        return request;
    }

    private ExperimentTimeRequest createLogStartTimeRequest(long startTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setStartTimeMillis(startTimeMillis);
        return request;
    }

    private ExperimentTimeRequest createLogEndTimeRequest(long endTimeMillis) {
        ExperimentTimeRequest request = new ExperimentTimeRequest();
        request.setEndTimeMillis(endTimeMillis);
        return request;
    }

    static OutputUpdate createLogLineRequest(@NonNull String line, long offset, boolean stderr, String context) {
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
     * Utility class to log asynchronously received data responses.
     */
    static final class DataResponseLogger {
        static void checkAndLog(LogDataResponse logDataResponse, Logger logger, Object request) {
            if (logDataResponse.hasFailed()) {
                logger.error("failed to log {}, reason: {}", request, logDataResponse.getMsg());
            } else if (logger.isDebugEnabled()) {
                logger.debug("success {}", logDataResponse);
            }
        }
    }
}
