package com.comet.experiment;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;

public class OnlineExperimentTest {
    private final String apiKey = "PUT YOUR API KEY HERE TO RUN TESTS";
    private final String projectName = "Testing Java Comet Library";
    private final String workspace = "PUT YOUR USER NAME HERE TO RUN TESTS";
    private final String existingExperimentKey = "PUT AN EXISTING EXPERIMENT KEY HERE";

    @Test
    public void testOnlineExperiment() {
        OnlineExperiment onlineExperiment =
                OnlineExperiment.builder(projectName, workspace, apiKey)
                        .withConfig(getOverrideConfig())
                        .build();
        onlineExperiment.exit();
    }

    @Test
    public void testCreateExperiment() {
        Experiment experiment = OnlineExperiment.of(apiKey, projectName, workspace);
    }

    @Test
    public void testExistingExperiment() {
        Experiment onlineExperiment =
                OnlineExperiment.builder(projectName, workspace, apiKey)
                        .withConfig(getOverrideConfig())
                        .withExistingExperimentKey(existingExperimentKey)
                        .build();
        onlineExperiment.logEndTime(System.currentTimeMillis());
    }

    @Test
    public void testStepOperations() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.setStep(7);
        experiment.nextStep();
        Assert.assertEquals(8, experiment.getStep());
    }

    @Test
    public void testLogMetric() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logMetric("metricName", "metricValue");
    }

    @Test
    public void testLogParam() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logParameter("paramName", "paramValue");
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
                OnlineExperiment.builder(projectName, workspace, apiKey)
                        .interceptStdout()
                        .withConfig(getOverrideConfig())
                        .build();

        System.out.println(experiment.getExperimentKey());
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
                OnlineExperiment.builder(projectName, workspace, apiKey)
                        .withConfig(getOverrideConfig())
                        .build();
        onlineExperiment.setContext("context");
        return onlineExperiment;
    }

    private File getOverrideConfig() {
        return new File(getClass().getClassLoader().getResource("overrides.conf").getFile());
    }
}
