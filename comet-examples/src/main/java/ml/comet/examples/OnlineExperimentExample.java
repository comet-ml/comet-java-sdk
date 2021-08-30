package ml.comet.examples;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.OnlineExperimentImpl;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;


public class OnlineExperimentExample {

    public static void main(String[] args) throws IOException {
        //this will take configs from /comet-java-sdk/comet-examples/src/main/resources/defaults.conf
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

        experiment.uploadAsset(getFile("chart.png"), "amazing chart.png", false);
        experiment.uploadAsset(getFile("model.hd5"), false);

        experiment.logOther("Parameter", 4);

        System.out.println("Epoch 1/20");
        System.out.println("- loss: 0.7858 - acc: 0.7759 - val_loss: 0.3416 - val_acc: 0.9026");

        experiment.logGraph(loadGraph("graph.json"));

        //will close connection, if not called connection will close on jvm exit
        experiment.end();
    }

    public static String askUserForInputOn(String message) {
        System.out.println(message);
        Scanner scan = new Scanner(System.in);
        String s = scan.next();
        scan.close();
        return s;
    }

    private static File getFile(String imageName) {
        File file = new File(OnlineExperimentExample.class.getClassLoader().getResource(imageName).getFile());
        return file;
    }

    private static void generateCharts(OnlineExperiment experiment){
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
        File file = new File(OnlineExperimentExample.class.getClassLoader().getResource("report.html").getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }

    private static String loadGraph(String fileName) throws IOException {
        File file = new File(OnlineExperimentExample.class.getClassLoader().getResource(fileName).getFile());
        return FileUtils.readFileToString(file, "UTF-8");
    }

    private static long getUpdatedEpochValue(OnlineExperiment experiment) {
        return experiment.getEpoch() + experiment.getStep() / 5;
    }

}