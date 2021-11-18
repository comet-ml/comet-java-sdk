package ml.comet.experiment.impl;

import lombok.NonNull;
import ml.comet.experiment.CometApi;
import ml.comet.experiment.builder.BaseCometBuilder;
import ml.comet.experiment.builder.CometApiBuilder;
import ml.comet.experiment.impl.config.CometConfig;
import ml.comet.experiment.impl.http.Connection;
import ml.comet.experiment.impl.http.ConnectionInitializer;
import ml.comet.experiment.impl.utils.CometUtils;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.RestProject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_BASE_URL;
import static ml.comet.experiment.impl.config.CometConfig.COMET_MAX_AUTH_RETRIES;

/**
 * The implementation of the {@link  CometApi}.
 */
public final class CometApiImpl implements CometApi {
    private Logger logger = LoggerFactory.getLogger(CometApiImpl.class);
    private final RestApiClient restApiClient;

    CometApiImpl(@NonNull String apiKey, @NonNull String baseUrl, int maxAuthRetries, Logger logger) {
        if (logger != null) {
            this.logger = logger;
        }
        Connection connection = ConnectionInitializer.initConnection(apiKey, baseUrl, maxAuthRetries, this.logger);
        this.restApiClient = new RestApiClient(connection);
        CometUtils.printCometSdkVersion();
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public List<String> getAllWorkspaces() {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllWorkspaces invoked");
        }

        try {
            return this.restApiClient
                    .getAllWorkspaces()
                    .blockingSingle()
                    .getWorkspaceNames();
        } catch (NoSuchElementException ignore) {
            this.logger.error("No workspaces found for the current user");
        } catch (Exception ex) {
            this.logger.error("Failed to read workspaces for the current user", ex);
            throw ex;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public List<RestProject> getAllProjects(@NonNull String workspaceName) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllProjects invoked");
        }
        try {
            return this.restApiClient
                    .getAllProjects(workspaceName)
                    .blockingSingle()
                    .getProjects();
        } catch (NoSuchElementException ignore) {
            this.logger.error("No projects found in the workspace {}", workspaceName);
        } catch (Exception ex) {
            this.logger.error("Failed to read projects in the workspace {}", workspaceName, ex);
            throw ex;
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    public List<ExperimentMetadataRest> getAllExperiments(@NonNull String projectId) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("getAllExperiments invoked");
        }
        try {
            return this.restApiClient
                    .getAllExperiments(projectId)
                    .blockingSingle()
                    .getExperiments();
        } catch (NoSuchElementException ignore) {
            this.logger.error("No experiments found in the project {}", projectId);
        } catch (Exception ex) {
            this.logger.error("Failed to read experiments found in the project {}", projectId, ex);
            throw ex;
        }
        return Collections.emptyList();
    }

    /**
     * Release all resources hold by this instance, such as connection to the Comet server.
     *
     * @throws IOException if an I/O exception occurs.
     */
    @Override
    public void close() throws IOException {
        this.restApiClient.dispose();
        // no need to wait for asynchronous requests to proceed because this class use only synchronous requests
        this.restApiClient.getConnection().close();
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

        public CometApiBuilder withConfigOverride(@NonNull File overrideConfig) {
            CometConfig.applyConfigOverride(overrideConfig);
            return this;
        }

        @Override
        public BaseCometBuilder<CometApi> withLogger(@NonNull Logger logger) {
            this.logger = logger;
            return this;
        }

        public CometApiBuilder withApiKey(@NonNull String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * Factory method to build fully initialized instance of the CometApiImpl.
         *
         * @return the fully initialized instance of the CometApiImpl.
         */
        public CometApi build() {
            if (StringUtils.isEmpty(this.apiKey)) {
                this.apiKey = COMET_API_KEY.getString();
            }
            return new CometApiImpl(
                    this.apiKey, COMET_BASE_URL.getString(), COMET_MAX_AUTH_RETRIES.getInt(), this.logger);
        }
    }
}