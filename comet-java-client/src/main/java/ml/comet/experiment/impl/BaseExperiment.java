package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.functions.Function;
import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.Experiment;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.asset.AssetType;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.http.ConnectionInitializer;
import ml.comet.experiment.impl.utils.CometUtils;
import ml.comet.experiment.model.CreateExperimentRequest;
import ml.comet.experiment.model.CreateExperimentResponse;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.ExperimentStatusResponse;
import ml.comet.experiment.model.GitMetadata;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.LogDataResponse;
import ml.comet.experiment.model.ValueMinMaxDto;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ml.comet.experiment.impl.asset.AssetType.ASSET_TYPE_SOURCE_CODE;
import static ml.comet.experiment.impl.constants.ApiEndpoints.ADD_ASSET;
import static ml.comet.experiment.impl.constants.QueryParamName.CONTEXT;
import static ml.comet.experiment.impl.constants.QueryParamName.EPOCH;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.FILE_NAME;
import static ml.comet.experiment.impl.constants.QueryParamName.OVERWRITE;
import static ml.comet.experiment.impl.constants.QueryParamName.STEP;
import static ml.comet.experiment.impl.constants.QueryParamName.TYPE;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_CLEANUP_PROMPT;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_LIVE;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_READ_DATA_FOR_EXPERIMENT;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.DataUtils.createGraphRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createLogEndTimeRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createLogHtmlRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createLogLineRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createLogMetricRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createLogOtherRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createLogParamRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createLogStartTimeRequest;
import static ml.comet.experiment.impl.utils.DataUtils.createTagRequest;

/**
 * The base class for all synchronous experiment implementations providing implementation of common routines
 * using synchronous networking.
 */
abstract class BaseExperiment implements Experiment {
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

    @Getter
    private RestApiClient restApiClient;
    @Getter
    private Connection connection;

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

        getLogger().info(getString(EXPERIMENT_LIVE, this.experimentLink));

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
     * @param context     the context to be associated with the parameter.
     * @throws CometApiException if received response with failure code.
     */
    @Override
    public void logMetric(@NonNull String metricName, @NonNull Object metricValue,
                          @NonNull ExperimentContext context) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetric {} = {}, context: {}", metricName, metricValue, context);
        }

        sendSynchronously(restApiClient::logMetric,
                createLogMetricRequest(metricName, metricValue, context));
    }

    @Override
    public void logMetric(String metricName, Object metricValue, long step, long epoch) {
        this.logMetric(metricName, metricValue, new ExperimentContext(step, epoch));
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param context       the context to be associated with the parameter.
     */
    @Override
    public void logParameter(String parameterName, Object paramValue, ExperimentContext context) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameter {} = {}, context: {}", parameterName, paramValue, context);
        }

        sendSynchronously(restApiClient::logParameter,
                createLogParamRequest(parameterName, paramValue, context));
    }

    @Override
    public void logParameter(String parameterName, Object paramValue, long step) {
        this.logParameter(parameterName, paramValue, new ExperimentContext(step));
    }

    /**
     * Synchronous version that waits for result or exception. Also, it checks the response status for failure.
     *
     * @param line    Text to be logged
     * @param offset  Offset describes the place for current text to be inserted
     * @param stderr  the flag to indicate if this is StdErr message.
     * @param context the context to be associated with the parameter.
     */
    @Override
    public void logLine(String line, long offset, boolean stderr, String context) {
        validate();

        sendSynchronously(restApiClient::logOutputLine,
                createLogLineRequest(line, offset, stderr, context));
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

        sendSynchronously(restApiClient::logHtml, createLogHtmlRequest(html, override));
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

    @Override
    public void logCode(@NonNull String code, @NonNull String fileName, String context) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("log raw source code, file name: {}", fileName);
        }

        validate();

        Map<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put(FILE_NAME, fileName);
            put(CONTEXT, context);
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
    public void logCode(@NonNull File asset, String context) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("log source code from file {}", asset.getName());
        }
        validate();

        Map<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, getExperimentKey());
            put(FILE_NAME, asset.getName());
            put(CONTEXT, context);
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
    public void uploadAsset(@NonNull File asset, @NonNull String fileName,
                            boolean overwrite, @NonNull ExperimentContext context) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("uploadAsset from file {}, name {}, override {}, context {}",
                    asset.getName(), fileName, overwrite, context);
        }
        validate();

        this.connection
                .sendPostAsync(asset, ADD_ASSET, new HashMap<QueryParamName, String>() {{
                            put(EXPERIMENT_KEY, getExperimentKey());
                            put(FILE_NAME, fileName);
                            put(STEP, Long.toString(context.getStep()));
                            put(EPOCH, Long.toString(context.getEpoch()));
                            put(CONTEXT, context.getContext());
                            put(OVERWRITE, Boolean.toString(overwrite));
                        }},
                        null)
                .toCompletableFuture()
                .exceptionally(t -> {
                    getLogger().error("failed to upload asset from file {} with name {}", asset, fileName, t);
                    return null;
                });
    }

    @Override
    public void uploadAsset(File asset, String fileName, boolean overwrite, long step, long epoch) {
        this.uploadAsset(asset, fileName, overwrite, new ExperimentContext(step, epoch));
    }

    @Override
    public void uploadAsset(File asset, boolean overwrite, ExperimentContext context) {
        this.uploadAsset(asset, asset.getName(), overwrite, context);
    }

    @Override
    public void uploadAsset(@NonNull File asset, boolean overwrite, long step, long epoch) {
        this.uploadAsset(asset, overwrite, new ExperimentContext(step, epoch));
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
        getLogger().info(getString(EXPERIMENT_CLEANUP_PROMPT, cleaningTimeout.getSeconds()));

        // mark as not alive
        this.alive = false;

        // close REST API
        if (this.restApiClient != null) {
            this.restApiClient.dispose();
        }

        // close connection
        if (this.connection != null) {
            try {
                this.connection.waitAndClose(this.cleaningTimeout);
                this.connection = null;
            } catch (Exception e) {
                getLogger().error("failed to close connection", e);
            }
        }
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
                .doOnError(ex -> getLogger().error(
                        getString(FAILED_READ_DATA_FOR_EXPERIMENT, alias, this.experimentKey), ex))
                .blockingGet();
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
    Single<String> validateAndGetExperimentKey() {
        if (StringUtils.isEmpty(this.experimentKey)) {
            return Single.error(new IllegalStateException("Experiment key must be present!"));
        }
        if (!this.alive) {
            return Single.error(new IllegalStateException("Experiment is not alive or already closed."));
        }
        return Single.just(getExperimentKey());
    }
}
