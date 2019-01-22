package com.comet.examples;

import com.comet.experiment.Experiment;
import com.comet.experiment.OnlineExperiment;

import java.util.Scanner;

public class OnlineExperimentExample {

    public static void main(String[] args) {
        String apiKey = askUserForInputOn("please enter api key: ");
        String workspace = askUserForInputOn("please enter workspace: ");
        String project = askUserForInputOn("please enter project: ");


        Experiment experiment = OnlineExperiment.of(apiKey, project, workspace);


        experiment.setExperimentName("Java-SDK");


        long step = 23l;
        experiment.logMetric("some name", "123", step);

    }


    public static String askUserForInputOn(String message){
        System.out.println(message);
        Scanner scan = new Scanner(System.in);
        String s = scan.next();
        return s;
    }
}
