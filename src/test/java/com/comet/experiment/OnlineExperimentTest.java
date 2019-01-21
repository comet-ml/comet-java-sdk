package com.comet.experiment;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class OnlineExperimentTest {
    private final String restApiKey = "PUT YOUR REST API KEY HERE TO RUN TESTS";
    private final String projectName = "Testing Java Comet Library";
    private final String workspace = "PUT YOUR USER NAME HERE TO RUN TESTS";

    @Test
    public void testOnlineExperiment() {
        OnlineExperiment onlineExperiment =
                OnlineExperiment.builder(projectName, workspace)
                        .withRestApiKey(restApiKey)
                        .build();
        onlineExperiment.exit();
    }

    @Test
    public void testCreateExperiment() {
        Experiment experiment = OnlineExperiment.of(restApiKey, projectName, workspace);
    }

    @Test
    public void testExperimentBuilder() {
        Experiment onlineExperiment =
                OnlineExperiment.builder(projectName, workspace)
                        .withRestApiKey(restApiKey)
                        .build();
    }

    @Test
    public void testStepOperations() {
        Experiment onlineExperiment =
                OnlineExperiment.builder(projectName, workspace)
                        .withRestApiKey(restApiKey)
                        .build();
        onlineExperiment.setStep(7);
        onlineExperiment.nextStep();
        Assert.assertEquals(8, onlineExperiment.getStep());
    }

    @Test
    public void testLogMetric() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logMetric("metricName", "metricValue");
    }

    @Test
    public void testLogParam() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logParam("paramName", "paramValue");
    }

    @Test
    public void testLogHtml() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logHtml("html", false);
    }

    @Test
    public void testLogOther() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logOther("otherName", "otherValue");
    }

    @Test
    public void testLogStartTime() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logStartTime(System.currentTimeMillis());
    }

    @Test
    public void testLogEndTime() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logEndTime(System.currentTimeMillis());
    }

    @Test
    public void testUploadAsset() {
        File file = new File(getClass().getClassLoader().getResource("Logo.psd").getFile());

        Experiment experiment = createAndRegisterExperiment();
        experiment.uploadAsset(file,"fileName", false);
    }

    @Test
    public void testUploadImage()  {
        File file = new File(getClass().getClassLoader().getResource("Logo.psd").getFile());

        Experiment experiment = createAndRegisterExperiment();
        experiment.uploadImage(file, "fileName", false);
    }

    @Test
    public void testCopyStdout() throws InterruptedException {
        OnlineExperiment experiment =
                OnlineExperiment.builder(projectName, workspace)
                        .withRestApiKey(restApiKey)
                        .interceptStdout()
                        .build();

        System.out.println(experiment.getExperimentKey().get());
        System.out.println(experiment.getExperimentLink().get());
        System.out.println("This should end up in Comet ML.");
        System.out.println("So should this.");
        System.err.println("This error should also get to Comet ML.");
        System.out.flush();
        System.err.flush();

        Thread.sleep(1000);
        experiment.stopInterceptStdout();
        Thread.sleep(1000);

        System.out.println("This should not end up in Comet ML.");
        System.out.println("This error should also not end up in Comet ML.");
    }

    private Experiment createAndRegisterExperiment() {
        OnlineExperiment onlineExperiment =
                OnlineExperiment.builder(projectName, workspace)
                        .withRestApiKey(restApiKey)
                        .build();
        onlineExperiment.setContext("context");
        return onlineExperiment;
    }
}
