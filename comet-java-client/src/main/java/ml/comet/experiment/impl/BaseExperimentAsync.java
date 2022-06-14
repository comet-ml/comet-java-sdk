package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.functions.Action;
import io.reactivex.rxjava3.functions.BiFunction;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.NonNull;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.asset.RemoteAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import ml.comet.experiment.impl.asset.AssetImpl;
import ml.comet.experiment.impl.asset.AssetType;
import ml.comet.experiment.impl.asset.RemoteAssetImpl;
import ml.comet.experiment.impl.rest.ArtifactEntry;
import ml.comet.experiment.impl.rest.ArtifactVersionState;
import ml.comet.experiment.impl.rest.HtmlRest;
import ml.comet.experiment.impl.rest.LogOtherRest;
import ml.comet.experiment.impl.rest.MetricRest;
import ml.comet.experiment.impl.rest.OutputUpdate;
import ml.comet.experiment.impl.rest.ParameterRest;
import ml.comet.experiment.impl.rest.RestApiResponse;
import ml.comet.experiment.impl.utils.AssetUtils;
import ml.comet.experiment.model.GitMetaData;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.io.File;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static ml.comet.experiment.artifact.GetArtifactOptions.Op;
import static ml.comet.experiment.impl.resources.LogMessages.ARTIFACT_LOGGED_WITHOUT_ASSETS;
import static ml.comet.experiment.impl.resources.LogMessages.ARTIFACT_UPLOAD_COMPLETED;
import static ml.comet.experiment.impl.resources.LogMessages.ARTIFACT_UPLOAD_STARTED;
import static ml.comet.experiment.impl.resources.LogMessages.ASSETS_FOLDER_UPLOAD_COMPLETED;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_FINALIZE_ARTIFACT_VERSION;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_ASSET_FOLDER;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_SEND_LOG_ARTIFACT_ASSET_REQUEST;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_SEND_LOG_ASSET_REQUEST;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_SEND_LOG_REQUEST;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_UPLOAD_SOME_ARTIFACT_ASSET;
import static ml.comet.experiment.impl.resources.LogMessages.LOG_ASSET_FOLDER_EMPTY;
import static ml.comet.experiment.impl.resources.LogMessages.LOG_REMOTE_ASSET_URI_FILE_NAME_TO_DEFAULT;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromData;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromFile;
import static ml.comet.experiment.impl.utils.RestApiUtils.createGitMetadataRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createGraphRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createLogEndTimeRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createLogHtmlRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createLogLineRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createLogMetricRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createLogOtherRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createLogParamRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createLogStartTimeRequest;
import static ml.comet.experiment.impl.utils.RestApiUtils.createTagRequest;

/**
 * The base class for all asynchronous experiment implementations providing implementation of common routines
 * using asynchronous networking.
 */
abstract class BaseExperimentAsync extends BaseExperiment {
    ExperimentContext baseContext;

    BaseExperimentAsync(@NonNull final String apiKey,
                        @NonNull final String baseUrl,
                        int maxAuthRetries,
                        final String experimentKey,
                        @NonNull final Duration cleaningTimeout,
                        final String projectName,
                        final String workspaceName) {
        super(apiKey, baseUrl, maxAuthRetries, experimentKey, cleaningTimeout, projectName, workspaceName);
        this.baseContext = ExperimentContext.empty();
    }

    ExperimentContext mergeWithBaseContextIfEmpty(ExperimentContext context) {
        if (context.isEmpty()) {
            return new ExperimentContext(this.baseContext);
        } else {
            return context;
        }
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logMetric(@NonNull String metricName, @NonNull Object metricValue,
                   @NonNull ExperimentContext context, @NonNull Optional<Action> onComplete) {
        ExperimentContext ctx = mergeWithBaseContextIfEmpty(context);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logMetricAsync {} = {}, context: {}", metricName, metricValue, ctx);
        }

        MetricRest metricRequest = createLogMetricRequest(metricName, metricValue, ctx);
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logParameter(@NonNull String parameterName, @NonNull Object paramValue,
                      @NonNull ExperimentContext context, @NonNull Optional<Action> onComplete) {
        ExperimentContext ctx = mergeWithBaseContextIfEmpty(context);

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logParameterAsync {} = {}, context: {}", parameterName, paramValue, ctx);
        }

        ParameterRest paramRequest = createLogParamRequest(parameterName, paramValue, ctx);
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logHtml(@NonNull String html, boolean override, @NonNull Optional<Action> onComplete) {
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logOther(@NonNull String key, @NonNull Object value, @NonNull Optional<Action> onComplete) {
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public void addTag(@NonNull String tag, @NonNull Optional<Action> onComplete) {
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logGraph(@NonNull String graph, @NonNull Optional<Action> onComplete) {
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logStartTime(long startTimeMillis, @NonNull Optional<Action> onComplete) {
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logEndTime(long endTimeMillis, @NonNull Optional<Action> onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logEndTimeAsync {}", endTimeMillis);
        }

        sendAsynchronously(getRestApiClient()::logStartEndTime, createLogEndTimeRequest(endTimeMillis), onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param gitMetaData The Git Metadata for the experiment.
     * @param onComplete  The optional action to be invoked when this operation asynchronously completes.
     *                    Can be {@code null} if not interested in completion signal.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logGitMetadataAsync(@NonNull GitMetaData gitMetaData, @NonNull Optional<Action> onComplete) {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("logGitMetadata {}", gitMetaData);
        }

        sendAsynchronously(getRestApiClient()::logGitMetadata, createGitMetadataRequest(gitMetaData), onComplete);
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logLine(String line, long offset, boolean stderr, String context, @NonNull Optional<Action> onComplete) {
        if (!this.alive || StringUtils.isBlank(this.experimentKey)) {
            // to avoid exceptions from StdOut logger
            return;
        }

        OutputUpdate request = createLogLineRequest(line, offset, stderr, context);
        Single<RestApiResponse> single = validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> getRestApiClient().logOutputLine(request, experimentKey));

        // register notification action if provided
        if (onComplete.isPresent()) {
            single = single.doFinally(onComplete.get());
        }

        // subscribe to receive operation results but do not log anything
        //noinspection ResultOfMethodCallIgnored
        single.subscribe(
                apiResponse -> {
                    // nothing to process here
                },
                throwable -> {
                    // just ignore to avoid infinite loop
                });
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param folder               the folder you want to log.
     * @param logFilePath          if {@code true}, log the file path with each file.
     * @param recursive            if {@code true}, recurse the folder.
     * @param prefixWithFolderName if {@code true} then path of each asset file will be prefixed with folder name
     *                             in case if {@code logFilePath} is {@code true}.
     * @param assetType            optional type of the asset (default: ASSET).
     * @param groupingName         optional name of group the assets should belong.
     * @param metadata             the optional metadata to associate.
     * @param context              the context to be associated with logged assets.
     * @param onCompleteAction     The optional action to be invoked when this operation
     *                             asynchronously completes. Can be empty if not interested in completion signal.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logAssetFolder(@NonNull File folder, boolean logFilePath, boolean recursive, boolean prefixWithFolderName,
                        @NonNull Optional<String> assetType,
                        @NonNull Optional<String> groupingName,
                        @NonNull Optional<Map<String, Object>> metadata,
                        @NonNull ExperimentContext context, @NonNull Optional<Action> onCompleteAction) {
        if (!folder.isDirectory()) {
            getLogger().warn(getString(LOG_ASSET_FOLDER_EMPTY, folder));
            return;
        }
        ExperimentContext assetContext = mergeWithBaseContextIfEmpty(context);

        AtomicInteger successfullyLoggedCount = new AtomicInteger();
        try {
            Stream<AssetImpl> assets = AssetUtils.walkFolderAssets(
                            folder, logFilePath, recursive, prefixWithFolderName, metadata, assetType, groupingName)
                    .peek(asset -> asset.setContext(assetContext));

            // create parallel execution flow with errors delaying
            // allowing processing of items even if some of them failed
            Observable<RestApiResponse> responseObservable =
                    Observable.fromStream(assets)
                            .flatMap(asset -> Observable.fromSingle(
                                    this.sendAssetAsync(getRestApiClient()::logAsset, asset)
                                            .doOnSuccess(apiResponse -> {
                                                if (!apiResponse.hasFailed()) {
                                                    successfullyLoggedCount.incrementAndGet();
                                                }
                                            })), true);

            if (onCompleteAction.isPresent()) {
                responseObservable = responseObservable.doFinally(onCompleteAction.get());
            }

            // subscribe for processing results
            //noinspection ResultOfMethodCallIgnored
            responseObservable
                    .ignoreElements() // ignore items which already processed, see: logAsset
                    .subscribe(
                            () -> getLogger().info(getString(ASSETS_FOLDER_UPLOAD_COMPLETED,
                                    folder, successfullyLoggedCount.get())),
                            throwable -> getLogger().error(
                                    getString(FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER, folder), throwable));
        } catch (Throwable t) {
            getLogger().error(getString(FAILED_TO_LOG_ASSET_FOLDER, folder), t);
        }
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logAssetFolder(@NonNull File folder, boolean logFilePath, boolean recursive, boolean prefixWithFolderName,
                        @NonNull ExperimentContext context, @NonNull Optional<Action> onCompleteAction) {
        this.logAssetFolder(folder, logFilePath, recursive, prefixWithFolderName, Optional.of(AssetType.ASSET.type()),
                empty(), empty(), context, onCompleteAction);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param uri         the {@link URI} pointing to the remote asset location. There is no imposed format,
     *                    and it could be a private link.
     * @param logicalPath the optional "name" of the remote asset, could be a dataset name, a model file name.
     * @param overwrite   if {@code true} will overwrite all existing assets with the same name.
     * @param metadata    Some additional data to attach to the remote asset.
     *                    The dictionary values must be JSON compatible.
     * @param context     the experiment context to be associated with the logged assets.
     * @param onComplete  The optional action to be invoked when this operation asynchronously completes.
     *                    Can be {@code null} if not interested in completion signal.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logRemoteAsset(@NonNull URI uri, Optional<String> logicalPath, boolean overwrite,
                        @NonNull Optional<Map<String, Object>> metadata, @NonNull ExperimentContext context,
                        @NonNull Optional<Action> onComplete) {
        ExperimentContext ctx = mergeWithBaseContextIfEmpty(context);

        RemoteAssetImpl asset = AssetUtils.createRemoteAsset(uri, logicalPath, overwrite, metadata, empty());
        this.logAssetAsync(getRestApiClient()::logRemoteAsset, asset, ctx, onComplete);

        if (Objects.equals(asset.getLogicalPath(), AssetUtils.REMOTE_FILE_NAME_DEFAULT)) {
            getLogger().warn(
                    getString(LOG_REMOTE_ASSET_URI_FILE_NAME_TO_DEFAULT, uri, AssetUtils.REMOTE_FILE_NAME_DEFAULT));
        }
    }

    /**
     * Asynchronously logs provided {@link Artifact}. First it synchronously upserts the artifact into the Comet
     * backend. If successful then artifact assets uploaded to the server. Finally, the artifact status committed
     * to the backend to confirm transaction status.
     *
     * @param artifact   the Comet artifact to be sent to the server.
     * @param onComplete the optional {@link Action} to be called upon operation completed,
     *                   either successful or failure.
     * @return the instance of {@link CompletableFuture} which can be used to query for {@link LoggedArtifact} with
     * details about new artifact version.
     * @throws ArtifactException if operation failed.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    CompletableFuture<LoggedArtifact> logArtifact(@NonNull final Artifact artifact,
                                                  @NonNull Optional<Action> onComplete)
            throws ArtifactException {
        // upsert artifact
        final ArtifactEntry entry = super.upsertArtifact(artifact);

        // get new artifact's version details
        final LoggedArtifactImpl loggedArtifact = (LoggedArtifactImpl) this.getArtifactVersionDetail(
                Op().artifactId(entry.getArtifactId()).versionId(entry.getArtifactVersionId()).build());

        // try to log artifact assets asynchronously
        final ArtifactImpl artifactImpl = (ArtifactImpl) artifact;
        if (artifactImpl.getAssets().size() == 0) {
            getLogger().warn(getString(ARTIFACT_LOGGED_WITHOUT_ASSETS, artifactImpl.getName()));
            return CompletableFuture.completedFuture(loggedArtifact);
        }

        getLogger().info(
                getString(ARTIFACT_UPLOAD_STARTED, loggedArtifact.getFullName(), artifactImpl.getAssets().size()));

        CompletableFuture<LoggedArtifact> future = new CompletableFuture<>();

        // upload artifact assets
        final String artifactVersionId = entry.getArtifactVersionId();

        Stream<ArtifactAsset> assets = artifactImpl.getAssets().stream()
                .peek(asset -> ((ArtifactAssetImpl) asset).setArtifactVersionId(artifactVersionId));

        // create parallel execution flow with errors delaying
        // allowing processing of items even if some of them failed
        AtomicInteger successfullySentCount = new AtomicInteger();
        Observable<RestApiResponse> observable = Observable
                .fromStream(assets)
                .flatMap(asset -> Observable.fromSingle(
                        this.sendArtifactAssetAsync(asset)
                                .doOnSuccess(restApiResponse -> {
                                    if (!restApiResponse.hasFailed()) {
                                        successfullySentCount.incrementAndGet();
                                    }
                                })), true);

        if (onComplete.isPresent()) {
            observable = observable.doFinally(onComplete.get());
        }

        // subscribe to get processing results
        //noinspection ResultOfMethodCallIgnored
        observable
                .ignoreElements() // ignore already processed items (see: logAsset), we are interested only in result
                .subscribe(
                        () -> {
                            getLogger().info(
                                    getString(ARTIFACT_UPLOAD_COMPLETED, loggedArtifact.getFullName(),
                                            successfullySentCount.get()));
                            // mark artifact version status as closed
                            this.updateArtifactVersionState(loggedArtifact, ArtifactVersionState.CLOSED, future);
                            // mark future as completed
                            if (!future.isCompletedExceptionally()) {
                                future.complete(loggedArtifact);
                            }
                        },
                        (throwable) -> {
                            getLogger().error(
                                    getString(FAILED_TO_UPLOAD_SOME_ARTIFACT_ASSET, loggedArtifact.getFullName()),
                                    throwable);
                            // mark artifact version status as failed
                            this.updateArtifactVersionState(loggedArtifact, ArtifactVersionState.ERROR, future);
                            // mark future as failed
                            if (!future.isCompletedExceptionally()) {
                                future.obtrudeException(throwable);
                            }
                        }
                );

        return future;
    }

    /**
     * Synchronously updates the state associated with Comet artifact version.
     *
     * @param loggedArtifact the {@link LoggedArtifact} instance with version details.
     * @param state          the state to be associated.
     * @param future         the {@link CompletableFuture} to be completed exceptionally if operation failed.
     */
    void updateArtifactVersionState(@NonNull LoggedArtifact loggedArtifact,
                                    @NonNull ArtifactVersionState state,
                                    CompletableFuture<LoggedArtifact> future) {
        try {
            super.updateArtifactVersionState(loggedArtifact.getVersionId(), state);
        } catch (Throwable t) {
            getLogger().error(getString(FAILED_TO_FINALIZE_ARTIFACT_VERSION, loggedArtifact.getFullName()), t);
            future.completeExceptionally(t);
        }
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param data         The data of the asset to be logged.
     * @param fileName     The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite    Whether to overwrite files of the same name in Comet
     * @param assetType    the type of the asset.
     * @param groupingName optional name of group this asset should belong.
     * @param metadata     the optional metadata to associate.
     * @param context      the experiment context to be associated with given assets.
     * @param onComplete   The optional action to be invoked when this operation asynchronously completes.
     *                     Can be {@code null} if not interested in completion signal.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logAssetDataAsync(byte[] data, @NonNull String fileName, boolean overwrite,
                           @NonNull Optional<String> assetType,
                           @NonNull Optional<String> groupingName,
                           @NonNull Optional<Map<String, Object>> metadata,
                           @NonNull ExperimentContext context,
                           @NonNull Optional<Action> onComplete) {

        AssetImpl asset = createAssetFromData(data, fileName, overwrite, metadata, assetType);
        groupingName.ifPresent(asset::setGroupingName);
        ExperimentContext ctx = mergeWithBaseContextIfEmpty(context);

        this.logAssetAsync(asset, ctx, onComplete);
    }

    /**
     * Asynchronous version that only logs any received exceptions or failures.
     *
     * @param file         The file asset to be stored
     * @param fileName     The file name under which the asset should be stored in Comet. E.g. "someFile.txt"
     * @param overwrite    Whether to overwrite files of the same name in Comet
     * @param assetType    the type of the asset.
     * @param groupingName optional name of group this asset should belong.
     * @param metadata     the optional metadata to associate.
     * @param context      the experiment context to be associated with given assets.
     * @param onComplete   The optional action to be invoked when this operation asynchronously completes.
     *                     Can be {@code null} if not interested in completion signal.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logAssetFileAsync(@NonNull File file, @NonNull String fileName, boolean overwrite,
                           @NonNull Optional<String> assetType,
                           @NonNull Optional<String> groupingName,
                           @NonNull Optional<Map<String, Object>> metadata,
                           @NonNull ExperimentContext context,
                           @NonNull Optional<Action> onComplete) {

        AssetImpl asset = createAssetFromFile(file, Optional.of(fileName), overwrite, metadata, assetType);
        groupingName.ifPresent(asset::setGroupingName);
        ExperimentContext ctx = mergeWithBaseContextIfEmpty(context);

        this.logAssetAsync(asset, ctx, onComplete);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logAssetFileAsync(@NonNull File file, @NonNull String fileName, boolean overwrite,
                           @NonNull ExperimentContext context,
                           @NonNull Optional<Action> onComplete) {

        this.logAssetFileAsync(file, fileName, overwrite, Optional.of(AssetType.ASSET.type()), empty(),
                empty(), context, onComplete);
    }

    /**
     * Asynchronously logs provided asset and signals upload completion if {@code onComplete} action provided.
     *
     * @param asset      the {@link Asset} to be uploaded.
     * @param context    the experiment context to be associated with given assets.
     * @param onComplete the optional {@link Action} to be called upon operation completed,
     *                   either successful or failure.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    void logAssetAsync(@NonNull final Asset asset, @NonNull ExperimentContext context,
                               @NonNull Optional<Action> onComplete) {
        this.logAssetAsync(getRestApiClient()::logAsset, asset, context, onComplete);
    }

    /**
     * Attempts to log provided {@link AssetImpl} or its subclass asynchronously using specified log function.
     *
     * @param <T>        the {@link AssetImpl} or its subclass.
     * @param func       the function to be invoked to send asset to the backend.
     * @param asset      the {@link AssetImpl} or subclass to be sent.
     * @param context    the experiment context to be associated with given assets.
     * @param onComplete The optional action to be invoked when this operation
     *                   asynchronously completes. Can be empty if not interested in completion signal.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T extends Asset> void logAssetAsync(@NonNull final BiFunction<T, String, Single<RestApiResponse>> func,
                                                 @NonNull final T asset, @NonNull ExperimentContext context,
                                                 @NonNull Optional<Action> onComplete) {
        ((AssetImpl) asset).setContext(context);
        Single<RestApiResponse> single = this.sendAssetAsync(func, asset);

        if (onComplete.isPresent()) {
            single = single.doFinally(onComplete.get());
        }

        // subscribe to get operation completed
        //noinspection ResultOfMethodCallIgnored
        single.subscribe(
                (apiResponse, throwable) -> {
                    // ignore - it is already processed by sendAssetAsync()
                }
        );
    }

    /**
     * Attempts to send given {@link Asset} or its subclass asynchronously.
     * This method will wrap send operation into {@link Single}.
     *
     * @param func  the function to be invoked to send asset to the backend.
     * @param asset the {@link Asset} or subclass to be sent.
     * @param <T>   the {@link Asset} or its subclass.
     * @return the {@link Single} which can be used to subscribe for operation results.
     */
    private <T extends Asset> Single<RestApiResponse> sendAssetAsync(
            @NonNull final BiFunction<T, String, Single<RestApiResponse>> func, @NonNull final T asset) {

        return validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> func.apply(asset, experimentKey))
                .doOnSuccess(restApiResponse ->
                        checkAndLogAssetResponse(restApiResponse, getLogger(), asset))
                .doOnError(throwable ->
                        getLogger().error(getString(FAILED_TO_SEND_LOG_ASSET_REQUEST, asset), throwable));
    }

    /**
     * Attempts to send given {@link ArtifactAsset} or its subclass asynchronously.
     *
     * @param asset the artifact asset.
     * @param <T>   the type of the artifact asset.
     * @return the {@link Single} which can be used to subscribe for operation results.
     */
    private <T extends ArtifactAsset> Single<RestApiResponse> sendArtifactAssetAsync(@NonNull final T asset) {
        Single<RestApiResponse> single;
        Scheduler scheduler = Schedulers.io();
        if (asset.isRemote()) {
            // remote asset
            single = validateAndGetExperimentKey()
                    .subscribeOn(scheduler)
                    .concatMap(experimentKey ->
                            getRestApiClient().logRemoteAsset((RemoteAsset) asset, experimentKey));
        } else {
            // local asset
            single = validateAndGetExperimentKey()
                    .subscribeOn(scheduler)
                    .concatMap(experimentKey -> getRestApiClient().logAsset(asset, experimentKey));
        }

        return single.doOnSuccess(restApiResponse -> checkAndLogAssetResponse(restApiResponse, getLogger(), asset))
                .doOnError(throwable -> getLogger().error(getString(FAILED_TO_SEND_LOG_ARTIFACT_ASSET_REQUEST, asset),
                        throwable));
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
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T> void sendAsynchronously(@NonNull final BiFunction<T, String, Single<RestApiResponse>> func,
                                        @NonNull final T request, final @NonNull Optional<Action> onComplete) {
        Single<RestApiResponse> single = validateAndGetExperimentKey()
                .subscribeOn(Schedulers.io())
                .concatMap(experimentKey -> func.apply(request, experimentKey));

        // register notification action if provided
        if (onComplete.isPresent()) {
            single = single.doFinally(onComplete.get());
        }

        // subscribe to receive operation results
        //noinspection ResultOfMethodCallIgnored
        single
                .observeOn(Schedulers.single())
                .subscribe(
                        restApiResponse -> checkAndLogResponse(restApiResponse, getLogger(), request),
                        throwable -> getLogger().error(getString(FAILED_TO_SEND_LOG_REQUEST, request), throwable)
                );
    }

    /**
     * Utility method to log asynchronously received data responses.
     */
    static void checkAndLogResponse(@NonNull RestApiResponse restApiResponse, @NonNull Logger logger, Object request) {
        if (restApiResponse.hasFailed()) {
            logger.error("failed to log {}, reason: {}, sdk error code: {}",
                    request, restApiResponse.getMsg(), restApiResponse.getSdkErrorCode());
        } else if (logger.isDebugEnabled()) {
            logger.debug("successful response {} received for request {}", restApiResponse, request);
        }
    }

    /**
     * Utility method to log asynchronously received data responses for Asset logging.
     */
    static void checkAndLogAssetResponse(@NonNull RestApiResponse restApiResponse, @NonNull Logger logger,
                                         Asset asset) {
        if (restApiResponse.hasFailed()) {
            logger.error("failed to log asset {}, reason: {}, sdk error code: {}",
                    asset, restApiResponse.getMsg(), restApiResponse.getSdkErrorCode());
        } else {
            logger.info("Successfully logged asset {}", asset);
        }
    }
}
