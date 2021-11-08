package ml.comet.examples;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.OnlineExperimentImpl;

import java.io.IOException;

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

    /**
     * The main entry point to the example.
     *
     * @param args the command line arguments if any.
     */
    public static void main(String[] args) {
        OnlineExperimentExample main = new OnlineExperimentExample();
        try {
            main.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void run() throws IOException {
        //this will take configs from /comet-java-sdk/comet-examples/src/main/resources/application.conf
        //be sure you have set up apiKey, project, workspace in defaults.conf before you start!

        OnlineExperiment experiment = new OnlineExperimentImpl();
        experiment.setInterceptStdout();

        //you can use a builder or just inject params
        //OnlineExperimentImpl.builder();

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

        experiment.uploadAsset(getResourceFile("chart.png"), "amazing chart.png", false);
        experiment.uploadAsset(getResourceFile("model.hd5"), false);

        experiment.logOther("Parameter", 4);

        System.out.println("Epoch 1/20");
        System.out.println("- loss: 0.7858 - acc: 0.7759 - val_loss: 0.3416 - val_acc: 0.9026");

        experiment.logGraph(readResourceToString("graph.json"));

        System.out.println("===== Experiment completed ====");

        // Flush thread and wait for a while to make sure everything is flushed
        System.out.flush();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //will close connection, if not called connection will close on jvm exit
        experiment.end();
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

}