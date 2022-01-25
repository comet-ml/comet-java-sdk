package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import lombok.NonNull;
import lombok.SneakyThrows;
import ml.comet.experiment.CometApi;
import ml.comet.experiment.builder.BaseCometBuilder;
import ml.comet.experiment.builder.CometApiBuilder;
import ml.comet.experiment.impl.config.CometConfig;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.http.ConnectionInitializer;
import ml.comet.experiment.impl.rest.ExperimentModelListResponse;
import ml.comet.experiment.impl.rest.ExperimentModelResponse;
import ml.comet.experiment.impl.rest.RegistryModelCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelItemCreateRequest;
import ml.comet.experiment.impl.rest.RegistryModelOverviewListResponse;
import ml.comet.experiment.impl.utils.CometUtils;
import ml.comet.experiment.impl.utils.DataModelUtils;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelNotFoundException;
import ml.comet.experiment.registrymodel.ModelRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_BASE_URL;
import static ml.comet.experiment.impl.config.CometConfig.COMET_MAX_AUTH_RETRIES;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_HAS_NO_MODELS;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_FIND_EXPERIMENT_MODEL_BY_NAME;
import static ml.comet.experiment.impl.resources.LogMessages.MODEL_REGISTERED_IN_WORKSPACE;
import static ml.comet.experiment.impl.resources.LogMessages.MODEL_VERSION_CREATED_IN_WORKSPACE;
import static ml.comet.experiment.impl.resources.LogMessages.UPDATE_REGISTRY_MODEL_DESCRIPTION_IGNORED;
import static ml.comet.experiment.impl.resources.LogMessages.UPDATE_REGISTRY_MODEL_IS_PUBLIC_IGNORED;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

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
    public ModelRegistry registerModel(@NonNull final Model model, @NonNull final String experimentKey) {
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

        ModelRegistry registry;
        if (modelInRegistry) {
            // create new version of the model
            if (StringUtils.isNotBlank(modelImpl.getDescription())) {
                this.logger.warn(getString(UPDATE_REGISTRY_MODEL_DESCRIPTION_IGNORED));
            }
            if (modelImpl.getIsPublic() != null) {
                this.logger.warn(getString(UPDATE_REGISTRY_MODEL_IS_PUBLIC_IGNORED));
            }
            RegistryModelItemCreateRequest request = DataModelUtils.createRegistryModelItemCreateRequest(modelImpl);
            registry = this.restApiClient.createRegistryModelItem(request).blockingGet().toModelRegistry();
            this.logger.info(getString(MODEL_VERSION_CREATED_IN_WORKSPACE,
                    model.getVersion(), model.getRegistryName(), model.getWorkspace()));
        } else {
            // create model's registry record
            RegistryModelCreateRequest request = DataModelUtils.createRegistryModelCreateRequest(modelImpl);
            registry = this.restApiClient.createRegistryModel(request).blockingGet().toModelRegistry();
            this.logger.info(getString(MODEL_REGISTERED_IN_WORKSPACE,
                    model.getRegistryName(), model.getVersion(), model.getWorkspace()));
        }
        return registry;
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
