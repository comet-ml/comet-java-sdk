package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;
import lombok.NonNull;
import lombok.SneakyThrows;
import ml.comet.experiment.CometApi;
import ml.comet.experiment.builder.BaseCometBuilder;
import ml.comet.experiment.builder.CometApiBuilder;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.impl.config.CometConfig;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.http.ConnectionInitializer;
import ml.comet.experiment.impl.rest.ExperimentModelListResponse;
import ml.comet.experiment.impl.rest.ExperimentModelResponse;
import ml.comet.experiment.impl.rest.RegistryModelCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelDetailsResponse;
import ml.comet.experiment.impl.rest.RegistryModelItemCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelNotesResponse;
import ml.comet.experiment.impl.rest.RegistryModelNotesUpdateRequest;
import ml.comet.experiment.impl.rest.RegistryModelOverviewListResponse;
import ml.comet.experiment.impl.rest.RegistryModelUpdateRequest;
import ml.comet.experiment.impl.rest.RestApiResponse;
import ml.comet.experiment.impl.utils.CometUtils;
import ml.comet.experiment.impl.utils.ExceptionUtils;
import ml.comet.experiment.impl.utils.ModelUtils;
import ml.comet.experiment.impl.utils.RestApiUtils;
import ml.comet.experiment.impl.utils.ZipUtils;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelDownloadInfo;
import ml.comet.experiment.registrymodel.ModelNotFoundException;
import ml.comet.experiment.registrymodel.ModelOverview;
import ml.comet.experiment.registrymodel.ModelRegistryRecord;
import ml.comet.experiment.registrymodel.ModelVersionOverview;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;

import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_BASE_URL;
import static ml.comet.experiment.impl.config.CometConfig.COMET_MAX_AUTH_RETRIES;
import static ml.comet.experiment.impl.constants.SdkErrorCodes.registryModelNotFound;
import static ml.comet.experiment.impl.resources.LogMessages.DOWNLOADING_REGISTRY_MODEL_PROMPT;
import static ml.comet.experiment.impl.resources.LogMessages.DOWNLOADING_REGISTRY_MODEL_TO_DIR;
import static ml.comet.experiment.impl.resources.LogMessages.DOWNLOADING_REGISTRY_MODEL_TO_FILE;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_HAS_NO_MODELS;
import static ml.comet.experiment.impl.resources.LogMessages.EXTRACTED_N_REGISTRY_MODEL_FILES;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_DOWNLOAD_REGISTRY_MODEL;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_FIND_EXPERIMENT_MODEL_BY_NAME;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_GET_REGISTRY_MODEL_DETAILS;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_GET_REGISTRY_MODEL_NOTES;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_GET_REGISTRY_MODEL_VERSIONS;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_UPDATE_REGISTRY_MODEL;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_UPDATE_REGISTRY_MODEL_NOTES;
import static ml.comet.experiment.impl.resources.LogMessages.MODEL_REGISTERED_IN_WORKSPACE;
import static ml.comet.experiment.impl.resources.LogMessages.MODEL_VERSION_CREATED_IN_WORKSPACE;
import static ml.comet.experiment.impl.resources.LogMessages.REGISTRY_MODEL_NOT_FOUND;
import static ml.comet.experiment.impl.resources.LogMessages.UPDATE_REGISTRY_MODEL_DESCRIPTION_IGNORED;
import static ml.comet.experiment.impl.resources.LogMessages.UPDATE_REGISTRY_MODEL_IS_PUBLIC_IGNORED;
import static ml.comet.experiment.impl.resources.LogMessages.WORKSPACE_HAS_NO_REGISTRY_MODELS;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.RestApiUtils.createRegistryModelNotesUpdateRequest;

/**
 * The implementation of the {@link  CometApi}.
 */
public final class CometApiImpl implements CometApi {
    private Logger logger = LoggerFactory.getLogger(CometApiImpl.class);
    private final String apiKey;
    private final String baseUrl;
    private final int maxAuthRetries;

    private RestApiClient restApiClient;
    private Connection connection;

    CometApiImpl(@NonNull String apiKey, @NonNull String baseUrl, int maxAuthRetries, Logger logger) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.maxAuthRetries = maxAuthRetries;

        if (logger != null) {
            this.logger = logger;
        }
    }

    @Override
    public List<String> getAllWorkspaces() {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllWorkspaces invoked");
        }

        return restApiClient.getAllWorkspaces()
                .doOnError(ex -> this.logger.error("Failed to read workspaces for the current user", ex))
                .blockingGet()
                .getWorkspaceNames();
    }

    @Override
    public List<Project> getAllProjects(@NonNull String workspaceName) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllProjects invoked");
        }

        return restApiClient.getAllProjects(workspaceName)
                .doOnError(ex -> this.logger.error("Failed to read projects in the workspace {}", workspaceName, ex))
                .blockingGet()
                .getProjects()
                .stream()
                .collect(ArrayList::new,
                        (projects, restProject) -> projects.add(restProject.toProject()),
                        ArrayList::addAll);
    }

    @Override
    public List<ExperimentMetadata> getAllExperiments(@NonNull String projectId) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllExperiments invoked");
        }

        return restApiClient.getAllExperiments(projectId)
                .doOnError(ex -> this.logger.error("Failed to read experiments found in the project {}", projectId, ex))
                .blockingGet()
                .getExperiments()
                .stream()
                .collect(ArrayList::new,
                        (metadataList, metadataRest) -> metadataList.add(metadataRest.toExperimentMetadata()),
                        ArrayList::addAll);
    }

    @Override
    public ModelRegistryRecord registerModel(@NonNull final Model model, @NonNull final String experimentKey) {
        // get list of experiment models
        List<ExperimentModelResponse> experimentModels = this.restApiClient
                .getExperimentModels(experimentKey)
                .map(ExperimentModelListResponse::getModels)
                .blockingGet();

        // check that experiment has models registered
        if (experimentModels == null || experimentModels.size() == 0) {
            throw new ModelNotFoundException(getString(EXPERIMENT_HAS_NO_MODELS, experimentKey));
        }

        // check that experiment has our model in the list
        Optional<ExperimentModelResponse> details = experimentModels.stream()
                .filter(modelResponse -> Objects.equals(modelResponse.getModelName(), model.getName()))
                .findFirst();
        if (!details.isPresent()) {
            String names = experimentModels.stream()
                    .map(ExperimentModelResponse::getModelName)
                    .collect(Collectors.joining(", "));
            throw new ModelNotFoundException(
                    getString(FAILED_TO_FIND_EXPERIMENT_MODEL_BY_NAME, model.getName(), names));
        }

        // set model fields
        final RegistryModelImpl modelImpl = (RegistryModelImpl) model;
        modelImpl.setExperimentModelId(details.get().getExperimentModelId());

        // check if model already registered in the experiment's workspace records
        Boolean modelInRegistry = this.restApiClient.getMetadata(experimentKey)
                .concatMap(experimentMetadataRest -> {
                    modelImpl.setWorkspace(experimentMetadataRest.getWorkspaceName());
                    return this.restApiClient.getRegistryModelsForWorkspace(experimentMetadataRest.getWorkspaceName());
                })
                .map(RegistryModelOverviewListResponse::getRegistryModels)
                .flatMapObservable(Observable::fromIterable)
                .any(registryModel -> Objects.equals(registryModel.getModelName(), model.getRegistryName()))
                .blockingGet();

        ModelRegistryRecord registry;
        if (modelInRegistry) {
            // create new version of the model
            if (StringUtils.isNotBlank(modelImpl.getDescription())) {
                this.logger.warn(getString(UPDATE_REGISTRY_MODEL_DESCRIPTION_IGNORED));
            }
            if (modelImpl.getIsPublic() != null) {
                this.logger.warn(getString(UPDATE_REGISTRY_MODEL_IS_PUBLIC_IGNORED));
            }
            RegistryModelItemCreateRequest request = RestApiUtils.createRegistryModelItemCreateRequest(modelImpl);
            registry = this.restApiClient.createRegistryModelItem(request).blockingGet().toModelRegistry();
            this.logger.info(getString(MODEL_VERSION_CREATED_IN_WORKSPACE,
                    model.getVersion(), model.getRegistryName(), model.getWorkspace()));
        } else {
            // create model's registry record
            RegistryModelCreateRequest request = RestApiUtils.createRegistryModelCreateRequest(modelImpl);
            registry = this.restApiClient.createRegistryModel(request).blockingGet().toModelRegistry();
            this.logger.info(getString(MODEL_REGISTERED_IN_WORKSPACE,
                    model.getRegistryName(), model.getVersion(), model.getWorkspace()));
        }
        registry.setRegistryName(model.getRegistryName());
        return registry;
    }

    @Override
    public ModelDownloadInfo downloadRegistryModel(@NonNull Path outputPath, @NonNull String registryName,
                                                   @NonNull String workspace, @NonNull DownloadModelOptions options)
            throws IOException {
        this.logger.info(getString(DOWNLOADING_REGISTRY_MODEL_PROMPT,
                registryName, options.getVersion(), options.getStage(), workspace));
        RestApiResponse response;
        ModelDownloadInfo info;
        if (!options.isExpand()) {
            String fileName = ModelUtils.registryModelZipFileName(registryName, options);
            Path filePath = outputPath.resolve(fileName);
            this.logger.info(getString(DOWNLOADING_REGISTRY_MODEL_TO_FILE, filePath));
            response = this.downloadRegistryModelToFile(filePath, workspace, registryName, options);
            info = new ModelDownloadInfo(filePath, options);
        } else {
            this.logger.info(getString(DOWNLOADING_REGISTRY_MODEL_TO_DIR, outputPath));
            response = this.downloadRegistryModelToDir(outputPath, workspace, registryName, options);
            info = new ModelDownloadInfo(outputPath, options);
        }

        if (response.hasFailed()) {
            this.logger.info(getString(
                    FAILED_TO_DOWNLOAD_REGISTRY_MODEL, response.getMsg(), response.getSdkErrorCode()));
            throw new CometApiException(getString(
                    FAILED_TO_DOWNLOAD_REGISTRY_MODEL, response.getMsg(), response.getSdkErrorCode()));
        }
        return info;
    }

    @Override
    public ModelDownloadInfo downloadRegistryModel(@NonNull Path outputPath, @NonNull String registryName,
                                                   @NonNull String workspace) throws IOException {
        return this.downloadRegistryModel(outputPath, registryName, workspace, DownloadModelOptions.Op().build());
    }

    @Override
    public Optional<ModelOverview> getRegistryModelDetails(@NonNull String registryName, @NonNull String workspace) {
        try {
            RegistryModelDetailsResponse details = this.restApiClient.getRegistryModelDetails(registryName, workspace)
                    .blockingGet();
            return Optional.of(details.toModelOverview(this.logger));
        } catch (CometApiException ex) {
            this.logger.error(getString(FAILED_TO_GET_REGISTRY_MODEL_DETAILS,
                    workspace, registryName, ex.getStatusMessage(), ex.getSdkErrorCode()), ex);
            if (ex.getSdkErrorCode() == registryModelNotFound) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    @Override
    public Optional<ModelVersionOverview> getRegistryModelVersion(
            @NonNull String registryName, @NonNull String workspace, @NonNull String version) {
        Optional<ModelOverview> overviewOptional = this.getRegistryModelDetails(registryName, workspace);
        if (!overviewOptional.isPresent()) {
            return Optional.empty();
        }
        ModelOverview overview = overviewOptional.get();
        if (overview.getVersions() == null || overview.getVersions().size() == 0) {
            String msg = getString(FAILED_TO_GET_REGISTRY_MODEL_VERSIONS, workspace, registryName);
            this.logger.error(msg);
            throw new ModelNotFoundException(msg);
        }
        return overview.getVersions()
                .stream()
                .filter(versionOverview -> Objects.equals(versionOverview.getVersion(), version))
                .findFirst();
    }

    @Override
    public List<String> getRegistryModelNames(@NonNull String workspace) {
        RegistryModelOverviewListResponse listResponse = this.restApiClient.getRegistryModelsForWorkspace(workspace)
                .blockingGet();
        if (listResponse.getRegistryModels() == null) {
            this.logger.info(getString(WORKSPACE_HAS_NO_REGISTRY_MODELS, workspace));
            return Collections.emptyList();
        }
        return listResponse.getRegistryModels().stream().collect(
                ArrayList::new,
                (strings, registryModelOverview) -> strings.add(registryModelOverview.getModelName()),
                ArrayList::addAll);
    }

    @Override
    public List<String> getRegistryModelVersions(@NonNull String registryName, @NonNull String workspace) {
        Optional<ModelOverview> overviewOptional = this.getRegistryModelDetails(registryName, workspace);
        if (!overviewOptional.isPresent()) {
            this.logger.warn(getString(REGISTRY_MODEL_NOT_FOUND, workspace, registryName));
            return Collections.emptyList();
        }

        List<ModelVersionOverview> versions = overviewOptional.get().getVersions();
        if (versions == null) {
            this.logger.warn(getString(FAILED_TO_GET_REGISTRY_MODEL_VERSIONS, workspace, registryName));
            return Collections.emptyList();
        }

        return versions.stream().collect(
                ArrayList::new,
                (strings, modelVersionOverview) -> strings.add(modelVersionOverview.getVersion()),
                ArrayList::addAll);
    }

    @Override
    public void updateRegistryModelNotes(
            @NonNull String notes, @NonNull String registryName, @NonNull String workspace) {
        RegistryModelNotesUpdateRequest request = createRegistryModelNotesUpdateRequest(notes, registryName, workspace);
        String errorMsg = getString(FAILED_TO_UPDATE_REGISTRY_MODEL_NOTES, workspace, registryName);
        RestApiResponse response = this.executeSyncRequest(
                this.restApiClient::updateRegistryModelNotes, request, errorMsg);

        this.checkRestApiResponse(response, errorMsg);
    }

    @Override
    public Optional<String> getRegistryModelNotes(@NonNull String registryName, @NonNull String workspace) {
        try {
            RegistryModelNotesResponse response = this.restApiClient
                    .getRegistryModelNotes(registryName, workspace)
                    .blockingGet();
            return Optional.ofNullable(response.getNotes());
        } catch (CometApiException ex) {
            this.logger.error(getString(FAILED_TO_GET_REGISTRY_MODEL_NOTES,
                    workspace, registryName, ex.getStatusMessage(), ex.getSdkErrorCode()), ex);
            if (ex.getSdkErrorCode() == registryModelNotFound) {
                return Optional.empty();
            }
            throw ex;
        }
    }

    @Override
    public void updateRegistryModel(@NonNull String registryName, @NonNull String workspace,
                                    String newRegistryName, String newDescription, boolean isPublic)
            throws ModelNotFoundException {
        this.updateRegistryModel(registryName, workspace,
                RestApiUtils.createRegistryModelUpdateRequest(newRegistryName, newDescription, isPublic));
    }

    @Override
    public void updateRegistryModel(@NonNull String registryName, @NonNull String workspace, String newRegistryName,
                                    String newDescription) throws ModelNotFoundException {
        this.updateRegistryModel(registryName, workspace,
                RestApiUtils.createRegistryModelUpdateRequest(newRegistryName, newDescription, null));
    }

    @Override
    public void updateRegistryModel(@NonNull String registryName, @NonNull String workspace, String newRegistryName)
            throws ModelNotFoundException {
        this.updateRegistryModel(registryName, workspace,
                RestApiUtils.createRegistryModelUpdateRequest(newRegistryName, null, null));
    }

    private void updateRegistryModel(@NonNull String registryName, @NonNull String workspace,
                                     @NonNull RegistryModelUpdateRequest request) {
        // get registry model details
        Optional<ModelOverview> overviewOptional = this.getRegistryModelDetails(registryName, workspace);
        if (!overviewOptional.isPresent()) {
            throw new ModelNotFoundException(getString(REGISTRY_MODEL_NOT_FOUND, workspace, registryName));
        }

        // update registry model details
        request.setRegistryModelId(overviewOptional.get().getRegistryModelId());
        String errorMsg = getString(FAILED_TO_UPDATE_REGISTRY_MODEL, workspace, registryName, request);
        RestApiResponse response = this.executeSyncRequest(this.restApiClient::updateRegistryModel, request, errorMsg);
        this.checkRestApiResponse(response, errorMsg);
    }

    /**
     * Release all resources hold by this instance, such as connection to the Comet server.
     *
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void close() throws IOException {
        if (Objects.nonNull(this.restApiClient)) {
            this.restApiClient.dispose();
        }
        // no need to wait for asynchronous requests to proceed because this class use only synchronous requests
        if (Objects.nonNull(this.connection)) {
            this.connection.close();
        }
    }

    /**
     * Initializes this instance with connection and REST API client.
     */
    void init() {
        CometUtils.printCometSdkVersion();
        this.connection = ConnectionInitializer.initConnection(
                this.apiKey, this.baseUrl, this.maxAuthRetries, this.logger);
        this.restApiClient = new RestApiClient(this.connection);
    }

    RestApiClient getRestApiClient() {
        return this.restApiClient;
    }

    /**
     * Downloads registry model with given name into specified file.
     *
     * @param filePath     the path to the model file.
     * @param workspace    the name of the workspace.
     * @param registryName the model name in the registry.
     * @param options      the download options.
     * @throws IOException thrown if I/O exception occurred during while saving model to the file.
     */
    RestApiResponse downloadRegistryModelToFile(@NonNull Path filePath, @NonNull String workspace,
                                                @NonNull String registryName, @NonNull DownloadModelOptions options)
            throws IOException {

        try (OutputStream out = Files.newOutputStream(filePath, CREATE_NEW)) {
            return this.restApiClient.downloadRegistryModel(out, workspace, registryName, options)
                    .blockingGet();
        }
    }

    /**
     * Downloads and expand the registry model file into specified folder.
     *
     * @param dirPath      the folder to save registry model files.
     * @param workspace    the name of the model's workspace.
     * @param registryName the name of the model in the registry.
     * @param options      the download options.
     * @throws IOException thrown if I/O exception occurred during while saving model's files.
     */
    RestApiResponse downloadRegistryModelToDir(@NonNull Path dirPath, @NonNull String workspace,
                                               @NonNull String registryName, @NonNull DownloadModelOptions options)
            throws IOException {
        try (PipedOutputStream out = new PipedOutputStream()) {
            try (PipedInputStream pin = new PipedInputStream(out)) {
                return Observable.zip(
                        Observable.fromCallable(() -> {
                            // ZIP stream deflate
                            try (ZipInputStream zis = new ZipInputStream(pin)) {
                                return ZipUtils.unzipToFolder(zis, dirPath);
                            }
                        }),
                        Observable.fromSingle(
                                this.restApiClient.downloadRegistryModel(out, workspace, registryName, options)),
                        (numFiles, apiResponse) -> {
                            this.logger.info(getString(EXTRACTED_N_REGISTRY_MODEL_FILES, numFiles, dirPath));
                            return apiResponse;
                        }).blockingFirst();
            }
        }
    }

    /**
     * Executes synchronous request to the Comet API using provided function with given request object as argument.
     *
     * @param func         the function to wrapping Comet API request.
     * @param request      the request object.
     * @param errorMessage the error message to be displayed if exception thrown during function execution.
     * @param <T>          the type of the request object.
     * @return the {@link RestApiResponse} encapsulating response.
     */
    <T> RestApiResponse executeSyncRequest(Function<T, Single<RestApiResponse>> func, T request, String errorMessage) {
        try {
            return func.apply(request).blockingGet();
        } catch (Throwable e) {
            this.logger.error(errorMessage, e);
            Throwable rootCause = ExceptionUtils.unwrap(e);
            if (rootCause instanceof CometApiException) {
                // the root is CometApiException - rethrow it
                throw (CometApiException) rootCause;
            } else {
                // wrap into runtime exception
                throw new RuntimeException(e);
            }
        }
    }

    private void checkRestApiResponse(RestApiResponse response, String errorMessage) {
        if (response.hasFailed()) {
            this.logger.error(errorMessage);
            throw new CometApiException(errorMessage);
        }
    }

    /**
     * Returns builder to be used to properly create instance of this class.
     *
     * @return the builder to be used to properly create instance of this class.
     */
    public static CometApiBuilder builder() {
        return new CometApiBuilderImpl();
    }

    /**
     * The builder to create properly configured instance of the CometApiImpl.
     */
    static final class CometApiBuilderImpl implements CometApiBuilder {
        private String apiKey;
        private Logger logger;

        @Override
        public CometApiBuilder withConfigOverride(@NonNull File overrideConfig) {
            CometConfig.applyConfigOverride(overrideConfig);
            return this;
        }

        @Override
        public BaseCometBuilder<CometApi> withLogger(@NonNull Logger logger) {
            this.logger = logger;
            return this;
        }

        @Override
        public CometApiBuilder withApiKey(@NonNull String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Factory method to build fully initialized instance of the {@link CometApi}.
         *
         * @return the fully initialized instance of the CometApiImpl.
         */
        @SneakyThrows
        @Override
        public CometApi build() {
            if (StringUtils.isBlank(this.apiKey)) {
                this.apiKey = COMET_API_KEY.getString();
            }
            CometApiImpl api = new CometApiImpl(
                    this.apiKey, COMET_BASE_URL.getString(), COMET_MAX_AUTH_RETRIES.getInt(), this.logger);
            try {
                api.init();
            } catch (Throwable throwable) {
                api.close();
                throw throwable;
            }
            return api;
        }
    }
}
