package com.comet.examples;

import com.comet.experiment.Experiment;
import com.comet.experiment.OnlineExperiment;
import org.apache.commons.io.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.Scanner;


public class OnlineExperimentExample {

    public static void main(String[] args) throws IOException {
        //this will take configs from /comet-java-sdk/comet-examples/src/main/resources/defaults.conf
        Experiment experiment = new OnlineExperiment();
        experiment.setInterceptStdout();

        //you can use a builder or just inject params
        //OnlineExperiment.builder();

        experiment.setExperimentName("Java-SDK");
        experiment.nextStep();

        //metric can be a number, string , or double
        experiment.logMetric("strMetric", "123");
        experiment.logMetric("numMetric", 123, 123);
        experiment.logMetric("doubleMetric", 123.5d);

        generateCharts(experiment);

        experiment.setStep(1234);

        experiment.logHtml(generateCustomHtmlReport(), false);

        experiment.logParameter("batch_size", "500");
        experiment.logParameter("learning_rate", 12);

        experiment.uploadImage(getFile("chart.png"), "comet logo", false);
        experiment.uploadAsset(getFile("model.hd5"), false);

        experiment.logOther("dataset-link", "/tmp/1.csv");

        System.out.println("Epoch 1/20");
        System.out.println("- loss: 0.7858 - acc: 0.7759 - val_loss: 0.3416 - val_acc: 0.9026");

        experiment.logGraph(loadGraph("graph.json"));

        //will close connection, if not called connection will close on jvm exit
        //experiment.exit();
    }

    public static String askUserForInputOn(String message) {
        System.out.println(message);
        Scanner scan = new Scanner(System.in);
        String s = scan.next();
        return s;
    }

    private static File getFile(String imageName) {
        File file = new File(OnlineExperimentExample.class.getClassLoader().getResource(imageName).getFile());
        return file;
    }

    private static void generateCharts(Experiment experiment){
        long currentStep = experiment.getStep();

        for (int i = 1; i < 150; i++) {
            experiment.logMetric("numMetric", 123 + i, currentStep + i);
        }

        for (int i = 1; i < 150; i++) {
            experiment.logMetric("strMetric", "123" + i, currentStep + i);
        }

        for (int i = 1; i < 150; i++) {
            experiment.logMetric("doubleMetric", 123.12d + i, currentStep + i);
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

}