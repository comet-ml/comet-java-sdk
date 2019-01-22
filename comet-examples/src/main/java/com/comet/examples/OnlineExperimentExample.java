package com.comet.examples;

import com.comet.experiment.Experiment;
import com.comet.experiment.OnlineExperiment;

import java.io.File;
import java.util.Scanner;

public class OnlineExperimentExample {

    public static void main(String[] args) {
        String apiKey = askUserForInputOn("please enter api key: ");
        String workspace = askUserForInputOn("please enter workspace: ");
        String project = askUserForInputOn("please enter project: ");

        Experiment experiment = OnlineExperiment.of(apiKey, project, workspace);

        experiment.setExperimentName("Java-SDK");

        experiment.nextStep();



        experiment.logMetric("string Metric", "123");
        experiment.logMetric("numeric Metric", 123, 123);
        experiment.logMetric("double Metric", 123.5d);

        experiment.setStep(1234);

        experiment.logHtml("<a href='www.comet.ml'>visit comet.ml web site</a>", false);

        experiment.logParameter("batch_size", "500");
        experiment.logParameter("learning_rate", 12);

        experiment.uploadImage(getImage("Logo.psd"), "comet logo", false);

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

    public static String askUserForInputOn(String message) {
        System.out.println(message);
        Scanner scan = new Scanner(System.in);
        String s = scan.next();
        return s;
    }

    private static File getImage(String imageName){
        File file = new File(OnlineExperimentExample.class.getClassLoader().getResource(imageName).getFile());
        return file;
    }
}