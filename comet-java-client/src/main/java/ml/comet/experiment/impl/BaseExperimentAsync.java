package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.asset.Asset;
import ml.comet.experiment.impl.utils.AssetUtils;
import ml.comet.experiment.model.GitMetadata;
import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.model.LogDataResponse;
import ml.comet.experiment.model.LogOtherRest;
import ml.comet.experiment.model.MetricRest;
import ml.comet.experiment.model.OutputUpdate;
import ml.comet.experiment.model.ParameterRest;
import org.slf4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static ml.comet.experiment.impl.asset.AssetType.ASSET_TYPE_ASSET;
import static ml.comet.experiment.impl.asset.AssetType.ASSET_TYPE_SOURCE_CODE;
import static ml.comet.experiment.impl.resources.LogMessages.ASSETS_FOLDER_UPLOAD_COMPLETED;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_ASSET_FOLDER;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_SEND_LOG_REQUEST;
import static ml.comet.experiment.impl.resources.LogMessages.LOG_ASSET_FOLDER_EMPTY;
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
 * The base class for all asynchronous experiment implementations providing implementation of common routines
 * using asynchronous networking.
 */
abstract class BaseExperimentAsync extends BaseExperiment {
    ExperimentContext context;

    final CompositeDisposable disposables = new CompositeDisposable();

    BaseExperimentAsync(@NonNull final String apiKey,
                        @NonNull final String baseUrl,
                        int maxAuthRetries,
                        final String experimentKey,
                        @NonNull final Duration cleaningTimeout,
                        final String projectName,
                        final String workspaceName) {
        super(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, projectName, workspaceName);
        this.context = ExperimentContext.empty();
    }

    @Override
    public void end() {
        if (!this.alive) {
            return;
        }
        super.end();

        // dispose all pending asynchronous calls
        if (disposables.size() > 0) {
            getLogger().warn("{} calls still has not been processed, disposing", disposables.size());
        }
        this.disposables.dispose();
    }

    void setContext(@NonNull ExperimentContext context) {
        this.context = context;
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param context     the context to be associated with the parameter.
     * @param onComplete  The optional action to be invoked when this operation asynchronously completes.
     *                    Can be {@code null} if not interested in completion signal.
     */
    void logMetric(@NonNull String metricName, @NonNull Object metricValue,
                   @NonNull ExperimentContext context, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetricAsync {} = {}, context: {}", metricName, metricValue, context);
        }

        MetricRest metricRequest = createLogMetricRequest(metricName, metricValue, context);
        this.sendAsynchronously(getRestApiClient()::logMetric, metricRequest, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param context       the context to be associated with the parameter.
     * @param onComplete    The optional action to be invoked when this operation asynchronously completes.
     *                      Can be {@code null} if not interested in completion signal.
     */
    void logParameter(@NonNull String parameterName, @NonNull Object paramValue,
                      @NonNull ExperimentContext context, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameterAsync {} = {}, context: {}", parameterName, paramValue, context);
        }

        ParameterRest paramRequest = createLogParamRequest(parameterName, paramValue, context);
        this.sendAsynchronously(getRestApiClient()::logParameter, paramRequest, onComplete);
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
    void logHtml(@NonNull String html, boolean override, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logHtmlAsync {}, override: {}", html, override);
        }

        HtmlRest htmlRequest = createLogHtmlRequest(html, override);
        this.sendAsynchronously(getRestApiClient()::logHtml, htmlRequest, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param key        The key for the data to be stored
     * @param value      The value for said key
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logOther(@NonNull String key, @NonNull Object value, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logOtherAsync {} {}", key, value);
        }

        LogOtherRest request = createLogOtherRequest(key, value);
        sendAsynchronously(getRestApiClient()::logOther, request, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param tag        The tag to be added
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    public void addTag(@NonNull String tag, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("addTagAsync {}", tag);
        }

        sendAsynchronously(getRestApiClient()::addTag, createTagRequest(tag), onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param graph      The graph to be logged
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logGraph(@NonNull String graph, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logGraphAsync {}", graph);
        }

        sendAsynchronously(getRestApiClient()::logGraph, createGraphRequest(graph), onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param startTimeMillis When you want to say that the experiment started
     * @param onComplete      The optional action to be invoked when this operation asynchronously completes.
     *                        Can be {@code null} if not interested in completion signal.
     */
    void logStartTime(long startTimeMillis, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logStartTimeAsync {}", startTimeMillis);
        }

        sendAsynchronously(getRestApiClient()::logStartEndTime, createLogStartTimeRequest(startTimeMillis), onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param endTimeMillis When you want to say that the experiment ended
     * @param onComplete    The optional action to be invoked when this operation asynchronously completes.
     *                      Can be {@code null} if not interested in completion signal.
     */
    void logEndTime(long endTimeMillis, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logEndTimeAsync {}", endTimeMillis);
        }

        sendAsynchronously(getRestApiClient()::logStartEndTime, createLogEndTimeRequest(endTimeMillis), onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param gitMetadata The Git Metadata for the experiment.
     * @param onComplete  The optional action to be invoked when this operation asynchronously completes.
     *                    Can be {@code null} if not interested in completion signal.
     */
    void logGitMetadataAsync(@NonNull GitMetadata gitMetadata, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logGitMetadata {}", gitMetadata);
        }

        sendAsynchronously(getRestApiClient()::logGitMetadata, gitMetadata, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param line       Text to be logged
     * @param offset     Offset describes the place for current text to be inserted
     * @param stderr     the flag to indicate if this is StdErr message.
     * @param context    the context to be associated with the parameter.
     * @param onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logLine(String line, long offset, boolean stderr, String context, Action onComplete) {
        OutputUpdate request = createLogLineRequest(line, offset, stderr, context);
        Single<LogDataResponse> single = validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> getRestApiClient().logOutputLine(request, experimentKey));

        // register notification action if provided
        if (onComplete != null) {
            single = single.doFinally(onComplete);
        }

        // subscribe to receive operation results but do not log anything
        single.subscribe();
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param folder               the folder you want to log.
     * @param logFilePath          if {@code true}, log the file path with each file.
     * @param recursive            if {@code true}, recurse the folder.
     * @param prefixWithFolderName if {@code true} then path of each asset file will be prefixed with folder name
     *                             in case if {@code logFilePath} is {@code true}.
     * @param context              the context to be associated with logged assets.
     * @param onComplete           onComplete The optional action to be invoked when this operation
     *                             asynchronously completes. Can be {@code null} if not interested in completion signal.
     */
    void logAssetFolder(@NonNull File folder, boolean logFilePath, boolean recursive, boolean prefixWithFolderName,
                        @NonNull ExperimentContext context, Action onComplete) {
        if (!folder.isDirectory()) {
            getLogger().error(getString(LOG_ASSET_FOLDER_EMPTY, folder));
            return;
        }

        AtomicInteger count = new AtomicInteger();
        try {
            Stream<Asset> assets = AssetUtils.walkFolderAssets(folder, logFilePath, recursive, prefixWithFolderName)
                    .peek(asset -> {
                        asset.setExperimentContext(context);
                        asset.setType(ASSET_TYPE_ASSET);
                        count.incrementAndGet();
                    });

            // create parallel execution flow with errors delaying
            // allowing processing of items even if some of them failed
            Observable<LogDataResponse> observable =
                    Observable.fromStream(assets)
                            .flatMap(asset -> Observable.fromSingle(sendAssetAsync(asset)), true);

            // register on completion action
            if (onComplete != null) {
                observable = observable.doFinally(onComplete);
            }

            // subscribe for processing results
            observable
                    .ignoreElements() // ignore items which already processed, see: logAsset
                    .subscribe(
                            () -> getLogger().info(
                                    getString(ASSETS_FOLDER_UPLOAD_COMPLETED, folder, count.get())),
                            (throwable -> {
                                getLogger().error(
                                        getString(FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER, folder), throwable);
                            }),
                            disposables);
        } catch (Throwable t) {
            getLogger().error(getString(FAILED_TO_LOG_ASSET_FOLDER, folder), t);
        }
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param file       The file asset to be stored
     * @param fileName   The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite  Whether to overwrite files of the same name in Comet
     * @param context    the context to be associated with the asset.
     * @param onComplete onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void uploadAsset(@NonNull File file, @NonNull String fileName,
                     boolean overwrite, @NonNull ExperimentContext context, Action onComplete) {
        Asset asset = new Asset();
        asset.setFile(file);
        asset.setFileName(fileName);
        asset.setExperimentContext(context);
        asset.setOverwrite(overwrite);
        asset.setType(ASSET_TYPE_ASSET);

        this.logAsset(asset, context, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param code       Code to be sent to Comet
     * @param fileName   Name of source file to be displayed on UI 'code' tab
     * @param context    the context to be associated with the asset.
     * @param onComplete onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logCode(@NonNull String code, @NonNull String fileName,
                 @NonNull ExperimentContext context, Action onComplete) {
        Asset asset = new Asset();
        asset.setFileLikeData(code.getBytes(StandardCharsets.UTF_8));
        asset.setFileName(fileName);
        asset.setExperimentContext(context);
        asset.setType(ASSET_TYPE_SOURCE_CODE);

        this.logAsset(asset, context, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param file       Asset with source code to be sent
     * @param context    the context to be associated with the asset.
     * @param onComplete onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logCode(@NonNull File file, @NonNull ExperimentContext context, Action onComplete) {
        Asset asset = new Asset();
        asset.setFile(file);
        asset.setFileName(file.getName());
        asset.setExperimentContext(context);
        asset.setType(ASSET_TYPE_SOURCE_CODE);

        this.logAsset(asset, context, onComplete);
    }

    /**
     * Asynchronously logs provided asset and signals upload completion if {@code onComplete} action provided.
     *
     * @param asset      the {@link Asset} to be uploaded.
     * @param context    the current experiment context.
     * @param onComplete the optional {@link Action} to be called upon operation completed,
     *                   either successful or failure.
     */
    void logAsset(@NonNull final Asset asset, @NonNull ExperimentContext context, Action onComplete) {
        asset.setExperimentContext(context);
        Single<LogDataResponse> single = this.sendAssetAsync(asset);
        if (onComplete != null) {
            single = single.doFinally(onComplete);
        }

        // subscribe to get operation completed
        single.subscribe(
                (logDataResponse) -> {
                    // ignore - already logged, see: sendAssetAsync
                },
                (throwable) -> {
                    // ignore - already logged, see: sendAssetAsync
                },
                disposables);
    }

    /**
     * Attempts to send given {@link Asset} asynchronously.
     * This method will wrap send operation into {@link Single} and transparently log any errors that may happen.
     *
     * @param asset the {@link Asset} to be sent.
     * @return the {@link Single} which can be used to subscribe for operation results.
     */
    private Single<LogDataResponse> sendAssetAsync(@NonNull final Asset asset) {
        return validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> getRestApiClient().logAsset(asset, experimentKey))
                .doOnSuccess(logDataResponse ->
                        AsyncDataResponseLogger.checkAndLog(logDataResponse, getLogger(), asset))
                .doOnError(throwable ->
                        getLogger().error(getString(FAILED_TO_SEND_LOG_REQUEST, asset), throwable));
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
                        (logDataResponse) -> AsyncDataResponseLogger.checkAndLog(logDataResponse, getLogger(), request),
                        (throwable) -> getLogger().error(getString(FAILED_TO_SEND_LOG_REQUEST, request), throwable),
                        disposables);
    }

    /**
     * Utility class to log asynchronously received data responses.
     */
    static final class AsyncDataResponseLogger {
        static void checkAndLog(LogDataResponse logDataResponse, Logger logger, Object request) {
            if (logDataResponse.hasFailed()) {
                logger.error("failed to log {}, reason: {}", request, logDataResponse.getMsg());
            } else if (logger.isDebugEnabled()) {
                logger.debug("success {}", logDataResponse);
            }
        }
    }
}
