package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import ml.comet.experiment.CometApi;
import ml.comet.experiment.ExperimentNotFoundException;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.impl.rest.RegistryModelItemOverview;
import ml.comet.experiment.impl.rest.RegistryModelOverview;
import ml.comet.experiment.impl.rest.RegistryModelOverviewListResponse;
import ml.comet.experiment.impl.utils.ZipUtils;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelBuilder;
import ml.comet.experiment.registrymodel.ModelDownloadInfo;
import ml.comet.experiment.registrymodel.ModelNotFoundException;
import ml.comet.experiment.registrymodel.ModelOverview;
import ml.comet.experiment.registrymodel.ModelRegistryRecord;
import ml.comet.experiment.registrymodel.ModelVersionNotFoundException;
import ml.comet.experiment.registrymodel.ModelVersionOverview;
import org.apache.commons.io.file.Counters;
import org.apache.commons.io.file.PathUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

import static ml.comet.experiment.impl.ExperimentTestFactory.API_KEY;
import static ml.comet.experiment.impl.ExperimentTestFactory.PROJECT_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.WORKSPACE_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.impl.LogModelSupportTest.SOME_MODEL_NAME;
import static ml.comet.experiment.impl.TestUtils.SOME_FULL_CONTEXT;
import static ml.comet.experiment.impl.TestUtils.SOME_METADATA;
import static ml.comet.experiment.impl.TestUtils.awaitForCondition;
import static ml.comet.experiment.impl.asset.AssetType.MODEL_ELEMENT;
import static ml.comet.experiment.impl.resources.LogMessages.EXPERIMENT_HAS_NO_MODELS;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The integration tests to test {@link CometApi} implementation by sending/retrieving data from the backend.
 */
@DisplayName("CometApiTest INTEGRATION")
@Tag("integration")
public class CometApiTest extends AssetsBaseTest {
    private static CometApiImpl COMET_API;
    private static OnlineExperiment SHARED_EXPERIMENT;

    private static final String SOME_COMMENT = "some short comment";
    private static final String SOME_DESCRIPTION = "test model for the experiment";
    private static final String DEFAULT_MODEL_VERSION = "1.0.0";
    private static final String SOME_STAGE = "some_stage";
    private static final String SOME_NOTES = "some notes";

    @BeforeAll
    public static void initEnvVariables() {
        COMET_API = (CometApiImpl) CometApiImpl.builder().withApiKey(API_KEY).build();
        SHARED_EXPERIMENT = createOnlineExperiment();
    }

    @AfterAll
    public static void shutdownSharedExperiment() throws IOException {
        SHARED_EXPERIMENT.end();
        COMET_API.close();
    }

    @Test
    public void testGetsAllWorkspaces() {
        List<String> workspaces = COMET_API.getAllWorkspaces();
        assertFalse(workspaces.isEmpty());
        assertTrue(workspaces.contains(WORKSPACE_NAME));
    }

    @Test
    public void testGetsAllProjects() {
        List<Project> projects = COMET_API.getAllProjects(WORKSPACE_NAME);
        assertFalse(projects.isEmpty());
        boolean projectExists = projects.stream()
                .anyMatch(project -> PROJECT_NAME.equals(project.getProjectName()));
        assertTrue(projectExists);
    }

    @Test
    public void testGetsAllExperiments() {
        List<Project> projects = COMET_API.getAllProjects(WORKSPACE_NAME);
        assertFalse(projects.isEmpty());

        Optional<List<ExperimentMetadata>> experimentsOpt = projects.stream()
                .filter(project -> PROJECT_NAME.equals(project.getProjectName()))
                .findFirst()
                .map(project -> COMET_API.getAllExperiments(project.getProjectId()));
        assertTrue(experimentsOpt.isPresent());

        List<ExperimentMetadata> experiments = experimentsOpt.get();
        assertFalse(experiments.isEmpty());
        assertExperimentInList(SHARED_EXPERIMENT.getExperimentKey(), experiments);
    }

    @Test
    public void testGetExperiments_Workspace() {
        List<ExperimentMetadata> experiments = COMET_API.getExperiments(WORKSPACE_NAME);
        assertFalse(experiments.isEmpty());
        assertExperimentInList(SHARED_EXPERIMENT.getExperimentKey(), experiments);
    }

    @Test
    public void testGetExperiments_Workspace_Project() {
        List<ExperimentMetadata> experiments = COMET_API.getExperiments(WORKSPACE_NAME, PROJECT_NAME);
        assertFalse(experiments.isEmpty());
        assertExperimentInList(SHARED_EXPERIMENT.getExperimentKey(), experiments);
    }

    @Test
    public void testGetExperiments() throws Exception {
        String experimentName = UUID.randomUUID().toString();
        String experimentKey;
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            experiment.setExperimentName(experimentName);
            experimentKey = experiment.getExperimentKey();

            // wait for experiment name to be updated
            awaitForCondition(() -> experimentName.equals(experiment.getMetadata().getExperimentName()),
                    "Experiment name update timeout");
        }

        List<ExperimentMetadata> experiments = COMET_API.getExperiments(WORKSPACE_NAME, PROJECT_NAME, experimentName);
        assertEquals(1, experiments.size(), "one experiment expected");

        ExperimentMetadata experimentMetadata = experiments.get(0);
        assertEquals(experimentKey, experimentMetadata.getExperimentKey());
    }

    @Test
    public void testGetExperiments_emptyProject_with_experimentName() {
        assertThrows(IllegalArgumentException.class, () ->
                COMET_API.getExperiments(WORKSPACE_NAME, null, "someExperiment*."));
    }

    @Test
    public void testGetExperimentMetadata() {
        ExperimentMetadata experimentMetadata = COMET_API.getExperimentMetadata(SHARED_EXPERIMENT.getExperimentKey());

        assertEquals(SHARED_EXPERIMENT.getExperimentKey(), experimentMetadata.getExperimentKey());
        assertEquals(SHARED_EXPERIMENT.getExperimentName(), experimentMetadata.getExperimentName());
        assertEquals(SHARED_EXPERIMENT.getProjectName(), PROJECT_NAME);
        assertEquals(SHARED_EXPERIMENT.getWorkspaceName(), WORKSPACE_NAME);
    }

    @Test
    public void testGetExperimentMetadata_not_found() {
        assertThrows(ExperimentNotFoundException.class, () ->
                COMET_API.getExperimentMetadata("not existing experiment key"));
    }

    @Test
    public void testRegisterModel() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
        // log model
        //
        logModel(modelName, true);

        // register model and check results
        //
        List<String> stages = Collections.singletonList("production");
        Model model = Model.newModel(modelName)
                .withComment(SOME_COMMENT)
                .withDescription(SOME_DESCRIPTION)
                .asPublic(true)
                .withStages(stages).build();
        ModelRegistryRecord modelRegistry = COMET_API.registerModel(model, SHARED_EXPERIMENT.getExperimentKey());
        assertNotNull(modelRegistry, "model registry record expected");
        assertEquals(modelRegistry.getRegistryName(), model.getRegistryName(), "wrong registry name");

        // check that model record exists and has appropriate values
        //
        RegistryModelOverview registryModel = getRegistryModelWithName(model.getRegistryName());
        assertNotNull(registryModel, "model registry record expected");

        checkLatestModelVersionItem(registryModel, model.getRegistryName(), DEFAULT_MODEL_VERSION, SOME_COMMENT);
        assertEquals(stages, registryModel.getLatestVersion().getStages(), "wrong stages");
    }

    @Test
    public void testRegisterModel_updateModelItem() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
        // log model
        //
        logModel(modelName, true);

        // register model and check results
        //
        Model model = Model.newModel(modelName)
                .withVersion(DEFAULT_MODEL_VERSION)
                .withComment(SOME_COMMENT)
                .withDescription(SOME_DESCRIPTION)
                .asPublic(true)
                .withStages(Collections.singletonList("production")).build();
        ModelRegistryRecord modelRegistry = COMET_API.registerModel(model, SHARED_EXPERIMENT.getExperimentKey());
        assertNotNull(modelRegistry, "model registry record expected");

        // update model
        //
        String newVersion = "1.0.1";
        String newComment = "updated model";
        Model updatedModel = Model.newModel(modelName)
                .withComment(newComment)
                .withVersion(newVersion)
                .build();
        modelRegistry = COMET_API.registerModel(updatedModel, SHARED_EXPERIMENT.getExperimentKey());
        assertNotNull(modelRegistry, "model registry record expected");

        // check that model record exists and has appropriate values
        //
        RegistryModelOverview registryModel = getRegistryModelWithName(model.getRegistryName());
        assertNotNull(registryModel, "model registry record expected");

        checkLatestModelVersionItem(registryModel, model.getRegistryName(), newVersion, newComment);
        List<String> stages = registryModel.getLatestVersion().getStages();
        assertEquals(0, stages.size(), "no stages expected");
    }

    @Test
    public void testRegisterModel_wrongModelName() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
        // log model
        //
        logModel(modelName, false);

        // check that exception thrown for wrong model name
        //
        String wrongModelName = "doesn't exists";
        Model model = Model.newModel(wrongModelName).build();
        ModelNotFoundException ex = assertThrows(ModelNotFoundException.class,
                () -> COMET_API.registerModel(model, SHARED_EXPERIMENT.getExperimentKey()));
        assertTrue(ex.getMessage().contains(modelName), String.format("wrong message: %s", ex.getMessage()));
    }

    @Test
    public void testRegisterModel_experimentHasNoModels() {
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

            // check that exception thrown if experiment has no models
            //
            Model model = Model.newModel(modelName)
                    .withVersion(DEFAULT_MODEL_VERSION)
                    .withComment(SOME_COMMENT)
                    .withDescription(SOME_DESCRIPTION)
                    .asPublic(true)
                    .withStages(Collections.singletonList("production")).build();
            ModelNotFoundException ex = assertThrows(ModelNotFoundException.class,
                    () -> COMET_API.registerModel(model, experiment.getExperimentKey()));
            String expectedMessage = getString(EXPERIMENT_HAS_NO_MODELS, experiment.getExperimentKey());
            assertEquals(expectedMessage, ex.getMessage(), "wrong error message");
        } catch (Throwable t) {
            fail(t);
        }
    }


    @Test
    public void testDownloadRegistryModel_toFile() throws IOException {
        Path tmpDir = Files.createTempDirectory("testDownloadRegistryModel_ToFile");
        try {
            String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
            // register model with defaults
            //
            String registryName = registerModelWithDefaults(modelName);

            // download registry model to file and check results
            //
            DownloadModelOptions options = DownloadModelOptions.Op()
                    .withExpand(false).withStage(SOME_STAGE).build();
            ModelDownloadInfo info = COMET_API.downloadRegistryModel(
                    tmpDir, registryName, SHARED_EXPERIMENT.getWorkspaceName(), options);
            assertNotNull(info, "download info expected");
            assertNotNull(info.getDownloadPath(), "download path expected");
            assertNotNull(info.getDownloadOptions(), "download options expected");
            options = info.getDownloadOptions();
            assertFalse(options.isExpand(), "expand must not be set");
            assertEquals(SOME_STAGE, options.getStage(), "wrong stage");

            assertTrue(Files.isRegularFile(info.getDownloadPath()), "model file not found");
            checkModelZipFile(info.getDownloadPath(), toAssetFileNames(assetFolderFiles));

        } finally {
            PathUtils.deleteDirectory(tmpDir);
        }
    }

    @Test
    public void testDownloadRegistryModel_toDir() throws IOException {
        Path tmpDir = Files.createTempDirectory("testDownloadRegistryModel_toDir");
        try {
            String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
            // register model with defaults
            //
            String registryName = registerModelWithDefaults(modelName);

            // download registry model to folder and check results
            //
            ModelDownloadInfo info = COMET_API.downloadRegistryModel(
                    tmpDir, registryName, SHARED_EXPERIMENT.getWorkspaceName());
            assertNotNull(info, "download info expected");
            assertNotNull(info.getDownloadPath(), "download path expected");
            assertNotNull(info.getDownloadOptions(), "download options expected");
            assertTrue(info.getDownloadOptions().isExpand(), "expand must be set");

            checkModelFileInDir(info.getDownloadPath(), toAssetFileNames(assetFolderFiles));
        } finally {
            PathUtils.deleteDirectory(tmpDir);
        }
    }

    @Test
    public void testGetRegistryModelDetails() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        String registryName = registerModelWithDefaults(modelName);

        // get model details and check results
        //
        Optional<ModelOverview> overviewOptional = COMET_API.getRegistryModelDetails(registryName,
                SHARED_EXPERIMENT.getWorkspaceName());

        assertTrue(overviewOptional.isPresent(), "overview expected");

        ModelOverview overview = overviewOptional.get();
        assertEquals(registryName, overview.getModelName(), "wrong name");
        assertEquals(SOME_DESCRIPTION, overview.getDescription(), "wrong description");
        assertTrue(overview.isPublic(), "must be public");
        assertEquals(1L, overview.getNumberOfVersions(), "wrong number of versions");
        assertNotNull(overview.getUserName(), "user name expected");
        assertNotNull(overview.getCreatedAt(), "createdAt expected");
        assertNotNull(overview.getLastUpdated(), "last updated expected");

        assertNotNull(overview.getVersions(), "at least one version expected");
        ModelVersionOverview version = overview.getVersions().get(0);

        assertEquals("1.0.0", version.getVersion(), "wrong version");
        assertEquals(SOME_COMMENT, version.getComment(), "wrong version's comment");
        assertEquals(Collections.singletonList(SOME_STAGE), version.getStages(), "wrong stages");
        assertNotNull(version.getUserName(), "user name expected");
        assertNotNull(version.getRestApiUrl(), "REST API URL missing");
        assertNotNull(version.getCreatedAt(), "createdAt expected");
        assertNotNull(version.getLastUpdated(), "last updated expected");

        assertNotNull(version.getAssets(), "assets expected");
        String[] fileNames = toAssetFileNames(assetFolderFiles);
        assertEquals(fileNames.length, version.getAssets().size(), "wrong assets number");
    }

    @Test
    public void testGetRegistryModelDetails_not_found() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // try to get not existing model
        //
        Optional<ModelOverview> overviewOptional = COMET_API.getRegistryModelDetails(modelName,
                SHARED_EXPERIMENT.getWorkspaceName());
        assertFalse(overviewOptional.isPresent());
    }

    @Test
    public void testGetRegistryModelVersion() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // Register two model versions
        //
        String newVersion = "1.0.1";
        String newComment = "updated model";
        List<String> stages = Collections.singletonList("production");
        String registryName = registerTwoModelVersions(modelName, newVersion, newComment, stages);

        // trying to get model's version and check result
        //
        Optional<ModelVersionOverview> versionOverviewOptional = COMET_API.getRegistryModelVersion(
                registryName, SHARED_EXPERIMENT.getWorkspaceName(), newVersion);
        assertTrue(versionOverviewOptional.isPresent(), "version details expected");

        ModelVersionOverview versionOverview = versionOverviewOptional.get();
        assertEquals(newVersion, versionOverview.getVersion(), "wrong version");
        assertEquals(newComment, versionOverview.getComment(), "wrong comment");
        assertEquals(stages, versionOverview.getStages(), "wrong stages");

        assertNotNull(versionOverview.getAssets(), "assets expected");
        String[] fileNames = toAssetFileNames(assetFolderFiles);
        assertEquals(fileNames.length, versionOverview.getAssets().size(), "wrong assets number");
    }

    @Test
    public void testGetRegistryModelVersion_not_found() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        String registryName = registerModelWithDefaults(modelName);

        // trying to get not existing model's version and check result
        //
        Optional<ModelVersionOverview> versionOverviewOptional = COMET_API.getRegistryModelVersion(
                registryName, SHARED_EXPERIMENT.getWorkspaceName(), "10.0.0");
        assertFalse(versionOverviewOptional.isPresent());
    }

    @Test
    public void testGetRegistryModelNames() {
        // There are no way to delete models right now, so we almost always will get some model names.
        // Until we have a way to remove models, we just check that model list returned without any errors
        List<String> names = COMET_API.getRegistryModelNames(SHARED_EXPERIMENT.getWorkspaceName());
        assertNotNull(names, "models list names expected either empty or populated");
    }

    @Test
    public void testGetRegistryModelVersions() {
        // Test model not found
        //
        String modelName = "not existing model";
        List<String> versions = COMET_API.getRegistryModelVersions(modelName, SHARED_EXPERIMENT.getWorkspaceName());
        assertEquals(0, versions.size(), "empty list expected");

        // Create model with two versions and check
        //
        modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
        String newVersion = "1.0.1";
        String newComment = "updated model";
        List<String> stages = Collections.singletonList("production");
        registerTwoModelVersions(modelName, newVersion, newComment, stages);

        versions = COMET_API.getRegistryModelVersions(modelName, SHARED_EXPERIMENT.getWorkspaceName());
        assertEquals(2, versions.size(), "expected two model versions");
        assertTrue(versions.contains(DEFAULT_MODEL_VERSION), "expected DEFAULT_MODEL_VERSION");
        assertTrue(versions.contains(newVersion), "expected version");
    }

    @Test
    public void testGetRegistryModelNotes_model_doesnt_exists() {
        // Test get notes from not existing model
        //
        String modelName = "not existing model";
        Optional<String> notes = COMET_API.getRegistryModelNotes(modelName, SHARED_EXPERIMENT.getWorkspaceName());
        assertFalse(notes.isPresent());
    }

    @Test
    public void testUpdateRegistryModelNotes_model_doesnt_exists() {
        // Test update notes from not existing model
        //
        String modelName = "not existing model";
        assertThrows(CometApiException.class, () ->
                COMET_API.updateRegistryModelNotes(SOME_NOTES, modelName, SHARED_EXPERIMENT.getWorkspaceName()));
    }

    @Test
    public void testGetRegistryModelsCount() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // wait for registry model to be processed by backend
        //
        Awaitility.await("failed to get registry model")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelDetails(modelName,
                        SHARED_EXPERIMENT.getWorkspaceName()).isPresent());

        // get models count and do assert
        //
        Optional<Integer> modelsCount = COMET_API.getRegistryModelsCount(SHARED_EXPERIMENT.getWorkspaceName());
        assertTrue(modelsCount.isPresent());
        // there could be more than one model registered from other tests
        assertTrue(modelsCount.get() > 1);
    }

    @Test
    public void testGetRegistryModelsCount_not_existing_workspace() {
        String notExistingWorkspace = "not existing workspace";
        Optional<Integer> modelsCount = COMET_API.getRegistryModelsCount(notExistingWorkspace);
        assertFalse(modelsCount.isPresent());
    }

    @Test
    public void testUpdateRegistryModelNotes() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // Update model with notes and check results
        //
        COMET_API.updateRegistryModelNotes(SOME_NOTES, modelName, SHARED_EXPERIMENT.getWorkspaceName());

        Optional<String> notes = COMET_API.getRegistryModelNotes(modelName, SHARED_EXPERIMENT.getWorkspaceName());
        assertTrue(notes.isPresent(), "notes not present");
        assertEquals(SOME_NOTES, notes.get(), "wrong notes");
    }

    @Test
    public void testUpdateRegistryModel_model_doesnt_exists() {
        // try to update not existing model
        //
        String modelName = "not existing model";
        assertThrows(CometApiException.class, () ->
                COMET_API.updateRegistryModel(modelName, SHARED_EXPERIMENT.getWorkspaceName(), null));
    }

    @Test
    public void testUpdateRegistryModel() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // update registry model with new values
        //
        String newModelName = String.format("%s-%d", "new-model-name", System.currentTimeMillis());
        String newDescription = "updated model description";
        COMET_API.updateRegistryModel(modelName, SHARED_EXPERIMENT.getWorkspaceName(), newModelName, newDescription, false);

        // get registry model and check that it was updated
        //
        Awaitility.await("failed to get updated model")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelDetails(newModelName,
                        SHARED_EXPERIMENT.getWorkspaceName()).isPresent());

        Optional<ModelOverview> overviewOptional = COMET_API.getRegistryModelDetails(newModelName,
                SHARED_EXPERIMENT.getWorkspaceName());

        assertTrue(overviewOptional.isPresent(), "model expected");
        ModelOverview modelOverview = overviewOptional.get();

        assertEquals(newModelName, modelOverview.getModelName(), "wrong model name");
        assertEquals(newDescription, modelOverview.getDescription(), "wrong description");
        assertFalse(modelOverview.isPublic(), "wrong visibility status");
    }

    @Test
    public void testUpdateRegistryModelVersion_model_doesnt_exists() {
        // try to update not existing model
        //
        String modelName = "not existing model";
        assertThrows(CometApiException.class, () ->
                COMET_API.updateRegistryModelVersion(
                        modelName, SHARED_EXPERIMENT.getWorkspaceName(), "1.0.1", "some comment"));
    }

    @Test
    public void testUpdateRegistryModelVersion_version_doesnt_exists() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // try to update not existing model
        //
        assertThrows(ModelVersionNotFoundException.class, () ->
                COMET_API.updateRegistryModelVersion(
                        modelName, SHARED_EXPERIMENT.getWorkspaceName(), "1.0.1", "some comment"));
    }

    @Test
    public void testUpdateRegistryModelVersion() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // wait for registry model to be processed by backend
        //
        Awaitility.await("failed to get registry model")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelDetails(modelName,
                        SHARED_EXPERIMENT.getWorkspaceName()).isPresent());

        // update model's version
        //
        String comment = "testUpdateRegistryModelVersion comment";
        List<String> stages = Collections.singletonList("testUpdateRegistryModelVersion");
        COMET_API.updateRegistryModelVersion(
                modelName, SHARED_EXPERIMENT.getWorkspaceName(), DEFAULT_MODEL_VERSION, comment, stages);

        // get registry model and check that it was updated
        //
        Awaitility.await("failed to get registry model")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelDetails(modelName,
                        SHARED_EXPERIMENT.getWorkspaceName()).isPresent());

        Optional<ModelOverview> overviewOptional = COMET_API.getRegistryModelDetails(modelName,
                SHARED_EXPERIMENT.getWorkspaceName());

        assertTrue(overviewOptional.isPresent(), "model expected");
        ModelOverview modelOverview = overviewOptional.get();

        assertNotNull(modelOverview.getVersions(), "versions list expected");
        assertEquals(1, modelOverview.getVersions().size(), "wrong versions list size");

        ModelVersionOverview versionOverview = modelOverview.getVersions().get(0);
        assertEquals(comment, versionOverview.getComment(), "wrong comment");
        assertEquals(stages, versionOverview.getStages(), "wrong stages");
    }

    @Test
    public void testAddRegistryModelVersionStage() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // wait for registry model to be processed by backend
        //
        Awaitility.await("failed to get registry model")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelDetails(modelName,
                        SHARED_EXPERIMENT.getWorkspaceName()).isPresent());

        String stage = "testAddRegistryModelVersionStage";
        COMET_API.addRegistryModelVersionStage(
                modelName, SHARED_EXPERIMENT.getWorkspaceName(), DEFAULT_MODEL_VERSION, stage);

        // get registry model and check that it was updated
        //
        Awaitility.await("failed to get registry model")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelDetails(modelName,
                        SHARED_EXPERIMENT.getWorkspaceName()).isPresent());

        Optional<ModelOverview> overviewOptional = COMET_API.getRegistryModelDetails(modelName,
                SHARED_EXPERIMENT.getWorkspaceName());

        assertTrue(overviewOptional.isPresent(), "model expected");
        ModelOverview modelOverview = overviewOptional.get();
        assertNotNull(modelOverview.getVersions(), "versions list expected");
        assertEquals(1, modelOverview.getVersions().size(), "wrong versions list size");

        ModelVersionOverview versionOverview = modelOverview.getVersions().get(0);
        boolean stageFound = versionOverview.getStages()
                .stream()
                .anyMatch(s -> Objects.equals(stage, s));
        assertTrue(stageFound);

        // check error when attempting to add the same stage again
        //
        assertThrows(CometApiException.class, () ->
                COMET_API.addRegistryModelVersionStage(
                        modelName, SHARED_EXPERIMENT.getWorkspaceName(), DEFAULT_MODEL_VERSION, stage));
    }

    @Test
    public void testDeleteRegistryModel() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // wait for registry model to be processed by backend
        //
        Awaitility.await("failed to get registry model")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelDetails(modelName,
                        SHARED_EXPERIMENT.getWorkspaceName()).isPresent());

        // try to delete the model
        //
        COMET_API.deleteRegistryModel(modelName, SHARED_EXPERIMENT.getWorkspaceName());
    }

    @Test
    public void testDeleteRegistryModel_doesnt_exists() {
        String modelName = "not existing model";
        assertThrows(CometApiException.class, () ->
                COMET_API.deleteRegistryModel(modelName, SHARED_EXPERIMENT.getWorkspaceName()));
    }

    private static String registerModelWithDefaults(String modelName) {
        // log model folder
        //
        logModelFolder(modelName);

        // register model and check results
        //
        Model model = Model.newModel(modelName)
                .withComment(SOME_COMMENT)
                .withDescription(SOME_DESCRIPTION)
                .asPublic(true)
                .withStages(Collections.singletonList(SOME_STAGE))
                .withVersion(DEFAULT_MODEL_VERSION)
                .build();
        ModelRegistryRecord modelRegistry = COMET_API.registerModel(model, SHARED_EXPERIMENT.getExperimentKey());
        assertNotNull(modelRegistry, "model registry record expected");
        assertEquals(modelRegistry.getRegistryName(), model.getRegistryName(), "wrong registry name");

        return model.getRegistryName();
    }

    private static String registerTwoModelVersions(String modelName, String nextVersion, String nextVersionComment,
                                                   List<String> nextVersionStages) {
        // log model folder
        //
        logModelFolder(modelName);

        // register model and check results
        //
        Model model = Model.newModel(modelName)
                .withComment(SOME_COMMENT)
                .withDescription(SOME_DESCRIPTION)
                .asPublic(true)
                .withStages(Collections.singletonList(SOME_STAGE))
                .build();
        ModelRegistryRecord modelRegistry = COMET_API.registerModel(model, SHARED_EXPERIMENT.getExperimentKey());
        assertNotNull(modelRegistry, "model registry record expected");
        assertEquals(modelRegistry.getRegistryName(), model.getRegistryName(), "wrong registry name");

        // update model's version
        //
        ModelBuilder builder = Model.newModel(modelName)
                .withComment(nextVersionComment)
                .withVersion(nextVersion);
        if (nextVersionStages != null) {
            builder.withStages(nextVersionStages);
        }

        Model updatedModel = builder.build();
        modelRegistry = COMET_API.registerModel(updatedModel, SHARED_EXPERIMENT.getExperimentKey());
        assertNotNull(modelRegistry, "model registry record expected");

        return model.getRegistryName();
    }

    @Test
    public void testDeleteRegistryModelVersion_model_doesnt_exists() {
        // try to delete version of not existing model
        //
        String modelName = "not existing model";
        assertThrows(CometApiException.class, () ->
                COMET_API.deleteRegistryModelVersion(modelName, SHARED_EXPERIMENT.getWorkspaceName(),
                        DEFAULT_MODEL_VERSION));
    }

    @Test
    public void testDeleteRegistryModelVersion_version_doesnt_exists() {
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());

        // register model with defaults
        //
        registerModelWithDefaults(modelName);

        // try to delete not existing version of the model
        //
        assertThrows(ModelVersionNotFoundException.class, () ->
                COMET_API.deleteRegistryModelVersion(modelName, SHARED_EXPERIMENT.getWorkspaceName(),
                        "1.0.1"));
    }

    @Test
    public void testDeleteRegistryModelVersion() {
        // Create model with two versions and check
        //
        String modelName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
        String newVersion = "1.0.1";
        String newComment = "updated model";
        List<String> stages = Collections.singletonList("production");
        registerTwoModelVersions(modelName, newVersion, newComment, stages);

        // wait for model versions to be processed by backend
        //
        Awaitility.await("failed to get registry model version")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> COMET_API.getRegistryModelVersion(
                        modelName, SHARED_EXPERIMENT.getWorkspaceName(), newVersion).isPresent());

        // Delete model version
        //
        COMET_API.deleteRegistryModelVersion(modelName, SHARED_EXPERIMENT.getWorkspaceName(), DEFAULT_MODEL_VERSION);

        // Check that version was deleted
        //
        Awaitility.await("registry model version was not deleted")
                .pollInterval(1, TimeUnit.SECONDS)
                .atMost(60, TimeUnit.SECONDS)
                .until(() -> !COMET_API.getRegistryModelVersion(
                        modelName, SHARED_EXPERIMENT.getWorkspaceName(), DEFAULT_MODEL_VERSION).isPresent());
    }

    // UnZip model's file into temporary directory and check that expected model files are present
    private static void checkModelZipFile(Path zipFilePath, String... fileNames) throws IOException {
        Path tmpDir = Files.createTempDirectory("checkModelZipFile");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFilePath.toFile().toPath()))) {
            ZipUtils.unzipToFolder(zis, tmpDir);
            checkModelFileInDir(tmpDir, fileNames);
        } finally {
            PathUtils.deleteDirectory(tmpDir);
        }
    }

    // Checks that provided dir has model file inside
    private static void checkModelFileInDir(Path dirPath, String... fileNames) throws IOException {
        List<String> fileNamesList = Arrays.asList(fileNames);
        Counters.PathCounters counters = PathUtils.countDirectory(dirPath);
        assertEquals(fileNames.length, counters.getFileCounter().get(), "wrong number of files in model's zip");
        try (Stream<Path> dirPathStream = Files.walk(dirPath)) {
            assertTrue(dirPathStream.allMatch(path -> {
                if (Files.isRegularFile(path)) {
                    return fileNamesList.contains(path.getFileName().toString());
                } else {
                    // we do not check directories
                    return true;
                }
            }), "expected model's file not found");
        }
    }

    private static void checkLatestModelVersionItem(RegistryModelOverview registryModel, String registryName,
                                                    String version, String comment) {
        assertEquals(registryName, registryModel.getModelName(), "wrong model name");
        assertTrue(registryModel.isPublic(), "should be public");
        assertEquals(SOME_DESCRIPTION, registryModel.getDescription(), "wrong description");

        RegistryModelItemOverview modelItem = registryModel.getLatestVersion();
        assertNotNull(modelItem, "latest model item version expected");
        assertEquals(version, modelItem.getVersion(), "wrong version");
        assertEquals(comment, modelItem.getComment(), "wrong comment");
    }

    private static RegistryModelOverview getRegistryModelWithName(String modelName) {
        return COMET_API.getRestApiClient().getRegistryModelsForWorkspace(SHARED_EXPERIMENT.getWorkspaceName())
                .map(RegistryModelOverviewListResponse::getRegistryModels)
                .flatMapObservable(Observable::fromIterable)
                .filter(registryModelOverview -> Objects.equals(registryModelOverview.getModelName(), modelName))
                .blockingFirst();
    }

    private static void logModel(String modelName, boolean witMetadata) {
        Map<String, Object> metadata = witMetadata ? SOME_METADATA : null;
        SHARED_EXPERIMENT.logModel(modelName, Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)),
                CODE_FILE_NAME, true, metadata, SOME_FULL_CONTEXT);

        awaitForCondition(() -> !SHARED_EXPERIMENT.getAssetList(MODEL_ELEMENT.type()).isEmpty(),
                "Failed to get logged model file");
    }

    private static void logModelFolder(String modelName) {
        SHARED_EXPERIMENT.logModelFolder(modelName, assetsFolder.toFile());

        awaitForCondition(() -> !SHARED_EXPERIMENT.getAssetList(MODEL_ELEMENT.type()).isEmpty(),
                "Failed to get logged model file");
    }

    private static void assertExperimentInList(String experimentKey, List<ExperimentMetadata> experiments) {
        boolean experimentExists = experiments.stream()
                .anyMatch(experiment -> experimentKey.equals(experiment.getExperimentKey()));
        assertTrue(experimentExists);
    }
}
