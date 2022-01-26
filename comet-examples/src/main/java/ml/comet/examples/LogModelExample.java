package ml.comet.examples;

import ml.comet.experiment.CometApi;
import ml.comet.experiment.ExperimentBuilder;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.utils.ModelUtils;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelRegistryRecord;
import org.awaitility.Awaitility;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ml.comet.examples.Utils.getResourceFile;
import static ml.comet.experiment.impl.asset.AssetType.MODEL_ELEMENT;

/**
 * Provides examples of working with models logging, registering, and retrieval.
 *
 * <p>To run from command line execute the following at the root of this module:
 * <pre>
 * COMET_API_KEY=your_api_key \
 * COMET_WORKSPACE_NAME=your_workspace \
 * COMET_PROJECT_NAME=your_project_name \
 * mvn exec:java -Dexec.mainClass="ml.comet.examples.ArtifactExample"
 * </pre>
 * Make sure to provide correct values above.
 */
public class LogModelExample implements BaseExample {

    private static final String SOME_MODEL_NAME = "someModelNameExample";
    private static final String SOME_MODEL_LOGICAL_PATH = "someExampleModelData.dat";
    private static final String SOME_MODEL_DATA = "some model data string";
    private static final String SOME_MODEL_DESCRIPTION = "LogModelExample model";
    private static final String SOME_MODEL_VERSION = "1.0.0";
    private static final String SOME_MODEL_VERSION_UP = "1.0.1";

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     * @throws Exception if experiment failed.
     */
    public static void main(String[] args) throws Exception {
        try (OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment().interceptStdout().build()) {
            LogModelExample.run(experiment);
        }
    }

    private static void run(OnlineExperiment experiment) throws Exception {
        experiment.setExperimentName("LogModel Example");

        // log model file
        //
        ExperimentContext context = new ExperimentContext(0, 0, "example");
        Map<String, Object> metadata = BaseExample.createMetaData();
        File file = getResourceFile(CHART_IMAGE_FILE);
        experiment.logModel(SOME_MODEL_NAME, file, AMAZING_CHART_NAME, true, metadata, context);
        System.out.printf("Logging file '%s' of the model '%s' with logical path '%s'\n\n",
                file, SOME_MODEL_NAME, AMAZING_CHART_NAME);

        // log model data
        //
        byte[] data = SOME_MODEL_DATA.getBytes(StandardCharsets.UTF_8);
        experiment.logModel(SOME_MODEL_NAME, data, SOME_MODEL_LOGICAL_PATH, true, metadata, context);
        System.out.printf("Logging data of the model '%s' with logical path '%s'\n\n",
                SOME_MODEL_NAME, SOME_MODEL_LOGICAL_PATH);

        // log model folder
        //
        Path assetDir = BaseExample.copyResourcesToTmpDir();
        experiment.logModelFolder(SOME_MODEL_NAME, assetDir.toFile(), true, metadata, context);
        System.out.printf("Logging data folder '%s' of the model '%s''\n\n", assetDir, SOME_MODEL_NAME);

        // read logged assets of the model
        //
        List<LoggedExperimentAsset> assets = new ArrayList<>();
        // wait until all assets upload completes asynchronously
        Awaitility.await("failed to wait for model's assets to be uploaded")
                .atMost(60, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> {
                    List<LoggedExperimentAsset> loggedAsset = experiment.getAssetList(MODEL_ELEMENT.type());
                    if (loggedAsset.size() > 0) {
                        assets.addAll(loggedAsset);
                        return true;
                    } else {
                        return false;
                    }
                });
        System.out.printf("Retrieved %d logged assets of the model '%s':\n", assets.size(), SOME_MODEL_NAME);
        assets.forEach(loggedExperimentAsset -> System.out.printf("\t%s\n", loggedExperimentAsset));

        // Register experiment model in the Comet registry
        //
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
                    .withStages(Collections.singletonList("production"))
                    .withVersion(SOME_MODEL_VERSION_UP).build();

            record = api.registerModel(updatedModel, experiment.getExperimentKey());
            System.out.printf("The experiment's model was successfully updated with record: %s\n\n", record);
        }

        System.out.println("===== Experiment completed ====");
    }
}
