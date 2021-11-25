package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import ml.comet.experiment.model.GitMetadata;
import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.model.LogDataResponse;
import ml.comet.experiment.model.LogOtherRest;
import ml.comet.experiment.model.MetricRest;
import ml.comet.experiment.model.OutputUpdate;
import ml.comet.experiment.model.ParameterRest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.time.Duration;

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
    @Setter
    @Getter
    long step;
    @Setter
    @Getter
    long epoch;
    @Getter
    @Setter
    String context = StringUtils.EMPTY;

    final CompositeDisposable disposables = new CompositeDisposable();

    BaseExperimentAsync(@NonNull final String apiKey,
                        @NonNull final String baseUrl,
                        int maxAuthRetries,
                        final String experimentKey,
                        @NonNull final Duration cleaningTimeout,
                        final String projectName,
                        final String workspaceName) {
        super(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, projectName, workspaceName);
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

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param metricName  The name for the metric to be logged
     * @param metricValue The new value for the metric.  If the values for a metric are plottable we will plot them
     * @param step        The current step for this metric, this will set the given step for this experiment
     * @param epoch       The current epoch for this metric, this will set the given epoch for this experiment
     * @param context     the context to be associated with the parameter.
     * @param onComplete  The optional action to be invoked when this operation asynchronously completes.
     *                    Can be {@code null} if not interested in completion signal.
     */
    void logMetricAsync(@NonNull String metricName, @NonNull Object metricValue,
                        long step, long epoch, String context, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetricAsync {} = {}, step: {}, epoch: {}", metricName, metricValue, step, epoch);
        }

        MetricRest metricRequest = createLogMetricRequest(metricName, metricValue, step, epoch, context);
        this.sendAsynchronously(getRestApiClient()::logMetric, metricRequest, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param parameterName The name of the param being logged
     * @param paramValue    The value for the param being logged
     * @param step          The current step for this metric, this will set the given step for this experiment
     * @param context       the context to be associated with the parameter.
     * @param onComplete    The optional action to be invoked when this operation asynchronously completes.
     *                      Can be {@code null} if not interested in completion signal.
     */
    void logParameterAsync(@NonNull String parameterName, @NonNull Object paramValue,
                           long step, String context, Action onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameterAsync {} = {}, step: {}", parameterName, paramValue, step);
        }

        ParameterRest paramRequest = createLogParamRequest(parameterName, paramValue, step, context);
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
    void logHtmlAsync(@NonNull String html, boolean override, Action onComplete) {
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
    void logOtherAsync(@NonNull String key, @NonNull Object value, Action onComplete) {
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
    public void addTagAsync(@NonNull String tag, Action onComplete) {
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
    void logGraphAsync(@NonNull String graph, Action onComplete) {
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
    void logStartTimeAsync(long startTimeMillis, Action onComplete) {
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
    void logEndTimeAsync(long endTimeMillis, Action onComplete) {
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
    void logGitMetadataAsync(GitMetadata gitMetadata, Action onComplete) {
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
    void logLineAsync(String line, long offset, boolean stderr, String context, Action onComplete) {
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
     * @param asset      The asset to be stored
     * @param fileName   The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite  Whether to overwrite files of the same name in Comet
     * @param step       the step to be associated with the asset
     * @param epoch      the epoch to be associated with the asset
     * @param context    the context to be associated with the asset.
     * @param onComplete onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void uploadAssetAsync(@NonNull File asset, @NonNull String fileName,
                          boolean overwrite, long step, long epoch, String context, Action onComplete) {
        // TODO implement me
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
    void logCodeAsync(@NonNull String code, @NonNull String fileName, String context, Action onComplete) {
        // TODO implement me
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param file       Asset with source code to be sent
     * @param context    the context to be associated with the asset.
     * @param onComplete onComplete The optional action to be invoked when this operation asynchronously completes.
     *                   Can be {@code null} if not interested in completion signal.
     */
    void logCodeAsync(@NonNull File file, String context, Action onComplete) {
        // TODO implement me
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
                        (throwable) -> getLogger().error("failed to log {}", request, throwable),
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
