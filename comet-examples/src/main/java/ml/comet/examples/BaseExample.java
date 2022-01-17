package ml.comet.examples;

import ml.comet.experiment.OnlineExperiment;
import org.apache.commons.io.file.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import static ml.comet.examples.Utils.getResourceFile;
import static ml.comet.examples.Utils.readResourceToString;

/**
 * The definition of common methods and constants to be used by examples.
 */
interface BaseExample {
    String CHART_IMAGE_FILE = "chart.png";
    String AMAZING_CHART_NAME = "amazing chart.png";
    String MODEL_FILE = "model.hd5";
    String HTML_REPORT_FILE = "report.html";
    String GRAPH_JSON_FILE = "graph.json";
    String CODE_FILE = "code_sample.py";

    static void generateCharts(OnlineExperiment experiment) {
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

    static String generateCustomHtmlReport() throws IOException {
        return readResourceToString("report.html");
    }

    static long getUpdatedEpochValue(OnlineExperiment experiment) {
        return experiment.getEpoch() + experiment.getStep() / 5;
    }

    static Path copyResourcesToTmpDir() throws IOException {
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
