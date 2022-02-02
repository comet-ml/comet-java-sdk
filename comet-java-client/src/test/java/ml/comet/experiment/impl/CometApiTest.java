package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import ml.comet.experiment.CometApi;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.impl.rest.RegistryModelItemOverview;
import ml.comet.experiment.impl.rest.RegistryModelOverview;
import ml.comet.experiment.impl.rest.RegistryModelOverviewListResponse;
import ml.comet.experiment.impl.utils.ZipUtils;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.ModelDownloadInfo;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelNotFoundException;
import ml.comet.experiment.registrymodel.ModelRegistryRecord;
import org.apache.commons.io.file.Counters;
import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
        boolean experimentExists = experiments.stream()
                .anyMatch(experiment -> SHARED_EXPERIMENT.getExperimentKey().equals(experiment.getExperimentKey()));
        assertTrue(experimentExists);
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
            // log model folder
            //
            logModelFolder(modelName);

            // register model and check results
            //
            Model model = Model.newModel(modelName)
                    .withComment(SOME_COMMENT)
                    .withDescription(SOME_DESCRIPTION)
                    .withStages(Collections.singletonList(SOME_STAGE))
                    .asPublic(true)
                    .build();
            ModelRegistryRecord modelRegistry = COMET_API.registerModel(model, SHARED_EXPERIMENT.getExperimentKey());
            assertNotNull(modelRegistry, "model registry record expected");
            assertEquals(modelRegistry.getRegistryName(), model.getRegistryName(), "wrong registry name");

            // download registry model to file and check results
            //
            DownloadModelOptions options = DownloadModelOptions.Op()
                    .withExpand(false).withStage(SOME_STAGE).build();
            ModelDownloadInfo info = COMET_API.downloadRegistryModel(
                    tmpDir, modelRegistry.getRegistryName(), SHARED_EXPERIMENT.getWorkspaceName(), options);
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

            // download registry model to folder and check results
            //
            ModelDownloadInfo info = COMET_API.downloadRegistryModel(
                    tmpDir, modelRegistry.getRegistryName(), SHARED_EXPERIMENT.getWorkspaceName());
            assertNotNull(info, "download info expected");
            assertNotNull(info.getDownloadPath(), "download path expected");
            assertNotNull(info.getDownloadOptions(), "download options expected");
            assertTrue(info.getDownloadOptions().isExpand(), "expand must be set");

            checkModelFileInDir(info.getDownloadPath(), toAssetFileNames(assetFolderFiles));
        } finally {
            PathUtils.deleteDirectory(tmpDir);
        }
    }

    // UnZip model's file into temporary directory and check that expected model files are present
    private static void checkModelZipFile(Path zipFilePath, String... fileNames) throws IOException {
        Path tmpDir = Files.createTempDirectory("checkModelZipFile");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
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
        assertTrue(Files.walk(dirPath).allMatch(path -> {
            if (Files.isRegularFile(path)) {
                return fileNamesList.contains(path.getFileName().toString());
            } else {
                // we do not check directories
                return true;
            }
        }), "expected model's file not found");
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
}
