package com.comet.experiment;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class ExperimentTest {
    private final String restApiKey = "PUT YOUR REST API KEY HERE TO RUN TESTS";
    private final String projectName = "Testing Java Comet Library";
    private final String workspace = "PUT YOUR USER NAME HERE TO RUN TESTS";

    @Test
    public void testCreateExperiment() {
        Experiment experiment = Experiment.of("restApiKey", "projectName", "workspace");
    }

    @Test
    public void testExperimentBuilder() throws IOException {
        Experiment experiment =
                Experiment.builder()
                        .withRestApiKey(restApiKey)
                        .withProjectName(projectName)
                        .withWorkspace(workspace)
                        .build();
    }

    @Test
    public void testStepOperations() throws IOException {
        Experiment experiment =
                Experiment.builder()
                        .withRestApiKey(restApiKey)
                        .withProjectName(projectName)
                        .withWorkspace(workspace)
                        .build();
        experiment.setStep(7);
        experiment.nextStep();
        Assert.assertEquals(8, experiment.getStep());
    }

    @Test
    public void testRegisterExperiment() {
        createAndRegisterExperiment();
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
    public void testCopyStdout() throws IOException, InterruptedException {
        Experiment experiment =
                Experiment.builder()
                        .withRestApiKey(restApiKey)
                        .withProjectName(projectName)
                        .withWorkspace(workspace)
                        .interceptStdout()
                        .build();
        Assert.assertTrue(experiment.registerExperiment());

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
        try {
            Experiment experiment =
                    Experiment.builder()
                            .withRestApiKey(restApiKey)
                            .withProjectName(projectName)
                            .withWorkspace(workspace)
                            .build();
            Assert.assertTrue(experiment.registerExperiment());
            experiment.setContext("context");
            return experiment;
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
            return null; // unreachable
        }
    }
}
