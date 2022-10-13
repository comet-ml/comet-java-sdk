package ml.comet.examples;

import ml.comet.experiment.CometApi;
import ml.comet.experiment.ExperimentBuilder;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.utils.ModelUtils;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelDownloadInfo;
import ml.comet.experiment.registrymodel.ModelOverview;
import ml.comet.experiment.registrymodel.ModelRegistryRecord;
import ml.comet.experiment.registrymodel.ModelVersionOverview;
import org.apache.commons.io.file.PathUtils;
import org.awaitility.Awaitility;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static ml.comet.examples.LogModelExample.SOME_MODEL_NAME;
import static ml.comet.experiment.impl.asset.AssetType.MODEL_ELEMENT;

/**
 * Provides examples of working with registry models.
 *
 * <p>To run from command line execute the following at the root of this module:
 * <pre>
 * COMET_API_KEY=your_api_key \
 * COMET_WORKSPACE_NAME=your_workspace \
 * COMET_PROJECT_NAME=your_project_name \
 * mvn exec:java -Dexec.mainClass="ml.comet.examples.RegistryModelExample"
 * </pre>
 * Make sure to provide correct values above.
 */
public class RegistryModelExample {

    static final String SOME_MODEL_DESCRIPTION = "RegistryModelExample model";
    static final String SOME_MODEL_VERSION = "1.0.0";
    static final String SOME_MODEL_VERSION_UP = "1.0.1";
    static final String STAGE_PRODUCTION = "production";
    static final String STAGE_STAGING = "staging";
    static final String SOME_NOTES = "some model notes";

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     * @throws Exception if experiment failed.
     */
    public static void main(String[] args) throws Exception {
        try (OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment()
                .interceptStdout().withExperimentName("RegistryModel Examples").build()) {
            RegistryModelExample.run(experiment);
        }
    }

    private static void run(OnlineExperiment experiment) throws Exception {
        // log model folder
        //
        ExperimentContext context = new ExperimentContext(0, 0, "example");
        Map<String, Object> metadata = BaseExample.createMetaData();
        Path assetDir = BaseExample.copyResourcesToTmpDir();
        experiment.logModelFolder(SOME_MODEL_NAME, assetDir.toFile(), true, metadata, context);
        System.out.printf("Logging data folder '%s' of the model '%s''\n\n", assetDir, SOME_MODEL_NAME);

        // wait until all assets upload completes asynchronously
        Awaitility.await("failed to wait for model's assets to be uploaded")
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    List<LoggedExperimentAsset> loggedAsset = experiment.getAssetList(MODEL_ELEMENT.type());
                    if (loggedAsset.size() > 0) {
                        System.out.printf("Successfully uploaded %d logged assets of the model '%s':\n",
                                loggedAsset.size(), SOME_MODEL_NAME);
                        return true;
                    } else {
                        return false;
                    }
                });

        // Register logged experiment model in the Comet registry
        //
        Path modelTmpDir = Files.createTempDirectory("RegistryModelExample");
        try (CometApi api = ExperimentBuilder.CometApi().build()) {
            String registryName = String.format("%s-%d", SOME_MODEL_NAME, System.currentTimeMillis());
            registryName = ModelUtils.createRegistryModelName(registryName);
            System.out.printf("\nRegistering model '%s' in the Comet model registry under workspace '%s'.\n",
                    registryName, experiment.getWorkspaceName());

            Model model = Model.newModel(SOME_MODEL_NAME)
                    .withRegistryName(registryName)
                    .withDescription(SOME_MODEL_DESCRIPTION)
                    .withStages(Collections.singletonList("example"))
                    .withVersion(SOME_MODEL_VERSION).build();

            ModelRegistryRecord record = api.registerModel(model, experiment.getExperimentKey());
            System.out.printf("The experiment's model was successfully registered under record: %s\n\n", record);


            // create new version of the registered model
            //
            System.out.printf("Updating model '%s' in the Comet model registry with new version '%s'.\n",
                    registryName, SOME_MODEL_VERSION_UP);
            Model updatedModel = Model.newModel(SOME_MODEL_NAME)
                    .withRegistryName(registryName)
                    .withDescription(SOME_MODEL_DESCRIPTION)
                    .withStages(Collections.singletonList(STAGE_PRODUCTION))
                    .withVersion(SOME_MODEL_VERSION_UP).build();

            record = api.registerModel(updatedModel, experiment.getExperimentKey());
            System.out.printf("The experiment's model was successfully updated with record: %s\n\n", record);

            // download model's asset files to folder
            //
            System.out.println("Downloading and expanding model's file assets.");
            ModelDownloadInfo info = api.downloadRegistryModel(
                    modelTmpDir, registryName, experiment.getWorkspaceName());
            System.out.printf("Successfully downloaded model's file assets to folder '%s'.\n\n",
                    info.getDownloadPath());

            // download model's asset files as ZIP file
            //
            System.out.println("Downloading model's file assets as ZIP file.");
            DownloadModelOptions opts = DownloadModelOptions.Op()
                    .withExpand(false).withStage(STAGE_PRODUCTION).build();
            info = api.downloadRegistryModel(
                    modelTmpDir, registryName, experiment.getWorkspaceName(), opts);
            System.out.printf("Successfully downloaded model's file assets as ZIP file '%s'.\n\n",
                    info.getDownloadPath());

            // get model's overview
            //
            System.out.printf("Retrieving overview details of the model '%s'\n", registryName);
            Optional<ModelOverview> modelOverviewOptional = api.getRegistryModelDetails(
                    registryName, experiment.getWorkspaceName());
            if (modelOverviewOptional.isPresent()) {
                ModelOverview modelOverview = modelOverviewOptional.get();
                System.out.printf(
                        "Retrieved overview details of the model '%s' which was created at '%s' and has %d versions.\n",
                        registryName, modelOverview.getCreatedAt(), modelOverview.getVersions().size());
            } else {
                System.out.printf("Overview of the model '%s' not found\n", registryName);
            }

            // add stage to the model
            //
            System.out.printf("Adding stage `%s' to the registered model version '%s:%s'\n",
                    STAGE_STAGING, registryName, SOME_MODEL_VERSION_UP);
            api.addRegistryModelVersionStage(registryName, experiment.getWorkspaceName(),
                    SOME_MODEL_VERSION_UP, STAGE_STAGING);

            // get details about model version
            //
            System.out.printf("Retrieving details of the model version '%s:%s'\n", registryName, SOME_MODEL_VERSION_UP);
            Optional<ModelVersionOverview> versionOverviewOptional = api.getRegistryModelVersion(
                    registryName, experiment.getWorkspaceName(), SOME_MODEL_VERSION_UP);
            if (versionOverviewOptional.isPresent()) {
                ModelVersionOverview versionOverview = versionOverviewOptional.get();
                System.out.printf(
                        "Retrieved overview of the model version '%s:%s' which was created at '%s' with stages '%s'.\n",
                        registryName, versionOverview.getVersion(), versionOverview.getCreatedAt(),
                        versionOverview.getStages());
            } else {
                System.out.printf("Overview of the model version '%s:%s' not found\n",
                        registryName, SOME_MODEL_VERSION_UP);
            }

            // get registered model names
            //
            System.out.printf("Retrieving model names registered in workspace '%s'.\n", experiment.getWorkspaceName());
            List<String> names = api.getRegistryModelNames(experiment.getWorkspaceName());
            System.out.printf("Retrieved model names: '%s'\n", names);

            // get registered model versions
            //
            System.out.printf("Retrieving versions of model '%s/%s'.\n",
                    experiment.getWorkspaceName(), SOME_MODEL_NAME);
            List<String> versions = api.getRegistryModelVersions(registryName, experiment.getWorkspaceName());
            System.out.printf("Retrieved model versions: '%s'.\n", versions);

            // create and retrieve registry model notes
            //
            System.out.printf("Updating notes of the registry model '%s/%s'.\n",
                    experiment.getWorkspaceName(), SOME_MODEL_NAME);
            api.updateRegistryModelNotes(SOME_NOTES, registryName, experiment.getWorkspaceName());
            System.out.println("Retrieving model notes...");
            Optional<String> notes = api.getRegistryModelNotes(registryName, experiment.getWorkspaceName());
            notes.ifPresent(s -> System.out.printf("Retrieved model notes: '%s'\n", s));

            // update registry model
            //
            System.out.printf("Updating registry model '%s/%s'.\n", experiment.getWorkspaceName(), SOME_MODEL_NAME);
            String newModelName = String.format("%s-%d", "updatedModelNameExample", System.currentTimeMillis());
            String newDescription = SOME_MODEL_DESCRIPTION + " updated";
            api.updateRegistryModel(registryName, experiment.getWorkspaceName(), newModelName, newDescription);
            System.out.println("Model was successfully updated");

            // update registry model version details
            //
            System.out.printf("Updating version details of the registry model '%s/%s:%s'.\n",
                    experiment.getWorkspaceName(), newModelName, SOME_MODEL_VERSION_UP);
            String newComment = "updated version comment";
            List<String> newStages = Collections.singletonList("updated stage");
            api.updateRegistryModelVersion(newModelName, experiment.getWorkspaceName(), SOME_MODEL_VERSION_UP,
                    newComment, newStages);
            System.out.printf("Model version details was successfully updated for: %s\n", SOME_MODEL_VERSION_UP);

            // read the number of registered models
            //
            Optional<Integer> modelsCount = api.getRegistryModelsCount(experiment.getWorkspaceName());
            if (modelsCount.isPresent()) {
                System.out.printf("The number of registered models under workspace '%s' is: %d\n",
                        experiment.getWorkspaceName(), modelsCount.get());
            } else {
                System.out.printf("Failed to get registered models count. The workspace '%s' doesn't exists.\n",
                        experiment.getWorkspaceName());
            }

            // delete registry model version
            //
            System.out.printf("Deleting registry model version '%s/%s:%s'.\n",
                    experiment.getWorkspaceName(), newModelName, SOME_MODEL_VERSION);
            api.deleteRegistryModelVersion(newModelName, experiment.getWorkspaceName(), SOME_MODEL_VERSION);
            System.out.printf("Version '%s' of the registry model was successfully deleted.\n", SOME_MODEL_VERSION);

            // delete the registry model
            //
            System.out.printf("Deleting registry model '%s/%s'.\n", experiment.getWorkspaceName(), newModelName);
            api.deleteRegistryModel(newModelName, experiment.getWorkspaceName());
            System.out.println("Model was successfully deleted.");

        } finally {
            PathUtils.deleteDirectory(modelTmpDir);
        }

        System.out.println("===== Experiment completed ====");
    }
}
