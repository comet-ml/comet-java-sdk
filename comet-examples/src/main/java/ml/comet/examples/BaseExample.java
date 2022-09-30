package ml.comet.examples;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.model.Curve;
import ml.comet.experiment.model.DataPoint;
import org.apache.commons.io.file.PathUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import static java.lang.Math.log;
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

    Map<String, Object> SOME_METADATA = new HashMap<String, Object>() {{
        put("metaInt", 10);
        put("metaString", "test");
        put("metaBoolean", true);
    }};

    static void generateCharts(OnlineExperiment experiment) {
        long currentStep = experiment.getStep();
        Random rd = new Random();

        for (int i = 1; i < 15; i++) {
            int value = (int) (Math.sin(i) * 20.0);
            experiment.logMetric("numMetricChart", value,
                    currentStep + i, getUpdatedEpochValue(experiment));
        }

        for (int i = 1; i < 15; i++) {
            float value = rd.nextFloat() * 100;
            experiment.logMetric("strMetricChart", String.valueOf(value),
                    currentStep + i, getUpdatedEpochValue(experiment));
        }

        for (int i = 1; i < 15; i++) {
            double value = Math.cos(i);
            experiment.logMetric("doubleMetricChart", 123.12d + value,
                    currentStep + i, getUpdatedEpochValue(experiment));
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

    static Map<String, Object> createMetaData() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("someInt", 10);
        metadata.put("someString", "test string");
        metadata.put("someBoolean", true);
        return metadata;
    }

    static Curve buildCurve(String name, int pointsCount) {
        DataPoint[] dataPoints = new DataPoint[pointsCount];
        for (int i = 0; i < pointsCount; i++) {
            dataPoints[i] = DataPoint.of(i, (float) log((i + 1) * 10));
        }
        return new Curve(dataPoints, name);
    }
}
