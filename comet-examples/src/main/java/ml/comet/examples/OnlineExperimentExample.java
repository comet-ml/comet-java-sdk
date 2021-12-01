package ml.comet.examples;

import ml.comet.experiment.ExperimentBuilder;
import ml.comet.experiment.OnlineExperiment;
import org.apache.commons.io.file.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static ml.comet.examples.Utils.getResourceFile;
import static ml.comet.examples.Utils.readResourceToString;

/**
 * Provides variety of example logging using OnlineExperiment.
 *
 * <p>To run from command line execute the following at the root of this module:
 * <pre>
 * COMET_API_KEY=your_api_key \
 * COMET_WORKSPACE_NAME=your_workspace \
 * COMET_PROJECT_NAME=your_project_name \
 * mvn exec:java -Dexec.mainClass="ml.comet.examples.OnlineExperimentExample"
 * </pre>
 * Make sure to provide correct values above.
 */
public class OnlineExperimentExample {

    private static final String CHART_IMAGE_FILE = "chart.png";
    private static final String MODEL_FILE = "model.hd5";
    private static final String HTML_REPORT_FILE = "report.html";
    private static final String GRAPH_JSON_FILE = "graph.json";

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     */
    public static void main(String[] args) {

        //this will take configs from /comet-java-sdk/comet-examples/src/main/resources/application.conf
        //be sure you have set up apiKey, project, workspace in defaults.conf before you start!

        OnlineExperiment experiment = ExperimentBuilder
                .OnlineExperiment()
                .interceptStdout()
                .build();

        //you can use a default builder or just inject params
        //OnlineExperiment experiment = ExperimentBuilder.OnlineExperiment().builder();

        try {
            OnlineExperimentExample.run(experiment);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            experiment.end();
        }
    }

    private static void run(OnlineExperiment experiment) throws IOException {
        experiment.setExperimentName("Java-SDK 2.0.2");
        experiment.nextStep();

        //metric can be a number, string , or double
        experiment.logMetric("strMetric", "123");
        experiment.logMetric("numMetric", 123, 123, 4);
        experiment.nextEpoch();
        experiment.logMetric("doubleMetric", 123.5d);

        experiment.setEpoch(3);

        generateCharts(experiment);

        experiment.setStep(1234);

        experiment.logHtml(generateCustomHtmlReport(), false);

        experiment.logParameter("batch_size", "500");
        experiment.logParameter("learning_rate", 12);

        experiment.uploadAsset(getResourceFile(CHART_IMAGE_FILE), "amazing chart.png", false);
        experiment.uploadAsset(getResourceFile(MODEL_FILE), false);

        // upload assets from folder
        Path assetDir = copyResourcesToTmpDir();
        experiment.logAssetFolder(assetDir.toFile(), true, true);

        experiment.logOther("Parameter", 4);

        System.out.println("Epoch 1/20");
        System.out.println("- loss: 0.7858 - acc: 0.7759 - val_loss: 0.3416 - val_acc: 0.9026");

        experiment.logGraph(readResourceToString(GRAPH_JSON_FILE));

        System.out.println("===== Experiment completed ====");

        // will close connection, if not called connection will close on jvm exit
        experiment.end();

        // remove tmp directory
        PathUtils.deleteDirectory(assetDir);
    }

    private static void generateCharts(OnlineExperiment experiment) {
        long currentStep = experiment.getStep();

        for (int i = 1; i < 15; i++) {
            experiment.logMetric("numMetric", 123 + i, currentStep + i, getUpdatedEpochValue(experiment));
        }

        for (int i = 1; i < 15; i++) {
            experiment.logMetric("strMetric", "123" + i, currentStep + i, getUpdatedEpochValue(experiment));
        }

        for (int i = 1; i < 15; i++) {
            experiment.logMetric("doubleMetric", 123.12d + i, currentStep + i, getUpdatedEpochValue(experiment));
        }
    }

    private static String generateCustomHtmlReport() throws IOException {
        return readResourceToString("report.html");
    }

    private static long getUpdatedEpochValue(OnlineExperiment experiment) {
        return experiment.getEpoch() + experiment.getStep() / 5;
    }

    private static Path copyResourcesToTmpDir() throws IOException {
        Path root = Files.createTempDirectory("onlineExperimentExample");
        PathUtils.copyFileToDirectory(
                Objects.requireNonNull(getResourceFile(CHART_IMAGE_FILE)).toPath(), root);
        PathUtils.copyFileToDirectory(
                Objects.requireNonNull(getResourceFile(MODEL_FILE)).toPath(), root);
        Files.createTempFile(root, "empty_file", ".txt");

        Path subDir = Files.createTempDirectory(root, "subDir");
        PathUtils.copyFileToDirectory(
                Objects.requireNonNull(getResourceFile(HTML_REPORT_FILE)).toPath(), subDir);
        PathUtils.copyFileToDirectory(
                Objects.requireNonNull(getResourceFile(GRAPH_JSON_FILE)).toPath(), subDir);

        return root;
    }
}