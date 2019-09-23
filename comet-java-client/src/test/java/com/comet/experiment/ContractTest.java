package com.comet.experiment;

import com.comet.response.*;
import org.junit.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.comet.experiment.Constants.*;

public class ContractTest {
    private static final String apiKey = "PUT YOUR API KEY HERE";
    private static final String restApiKey = "PUT YOUR REST API KEY HERE";
    private static final String projectName = "testing-java-comet-library";
    private static final String workspace = "PUT YOUR WORKSPACE/USER NAME HERE";
    private static final String existingExperimentKey = "PUT THE KEY OF AN EXISTING EXPERIMENT HERE";

    private static Experiment sharedExperiment = null;
    private static CometApi sharedCometApi = null;

    @BeforeClass
    public static void setupSharedExperiment() {
        sharedExperiment =
                OnlineExperiment.builder(apiKey, projectName, workspace)
                        .withConfig(getOverrideConfig())
                        .build();
        sharedExperiment.setStep(1);

        sharedCometApi = CometApi.builder(restApiKey)
                .withConfig(getOverrideConfig()).build();
    }

    @AfterClass
    public static void shutdownSharedExperiment() {
        sharedExperiment.exit();
    }

    @Test
    public void testOnlineExperiment() {
        OnlineExperiment onlineExperiment =
                OnlineExperiment.builder(apiKey, projectName, workspace)
                        .withConfig(getOverrideConfig())
                        .build();
        onlineExperiment.exit();
    }

    @Test
    public void testGetWorkspaces() {
        CometApi cometApi = CometApi.builder(restApiKey)
                .withConfig(getOverrideConfig()).build();
        List<String> workspaces = cometApi.getAllWorkspaces();
        System.out.println(workspaces);
        Assert.assertFalse(workspaces.isEmpty());
    }

    @Test
    public void testGetProjectsAndExperiments() {
        List<ProjectRest> projects = sharedCometApi.getAllProjects(workspace);

        System.out.println(projects);
        Assert.assertFalse(projects.isEmpty());
        List<String> projectNames = projects.stream()
                .map(x -> x.getProject_name()).collect(Collectors.toList());
        Assert.assertTrue(projectNames.contains(projectName));
        Optional<ProjectRest> project = projects.stream()
                .filter(x -> x.getProject_name().equals(projectName)).findFirst();
        Assert.assertTrue(project.isPresent());

        List<ExperimentRest> experiments = sharedCometApi.getAllExperiments(project.get().getProject_id());

        Assert.assertFalse(experiments.isEmpty());
    }

    @Test
    public void testGitMetadata() throws InterruptedException {
        sharedExperiment = createAndRegisterExperiment();
        GitMetadata gitMetadata = new GitMetadata("user", "root", "branch", "parent", "origin");
        sharedExperiment.logGitMetadata(gitMetadata);

        Thread.sleep(1000);

        Optional<GitMetadata> gitMetadataFetch = sharedCometApi.getGitMetadata(sharedExperiment.getExperimentKey());

        Assert.assertTrue(gitMetadataFetch.isPresent());
        Assert.assertEquals(gitMetadata.getBranch(), gitMetadataFetch.get().getBranch());
        Assert.assertEquals(gitMetadata.getOrigin(), gitMetadataFetch.get().getOrigin());
        Assert.assertEquals(gitMetadata.getParent(), gitMetadataFetch.get().getParent());
        Assert.assertEquals(gitMetadata.getRoot(), gitMetadataFetch.get().getRoot());
        Assert.assertEquals(gitMetadata.getUser(), gitMetadataFetch.get().getUser());
    }

    @Test
    public void testLogHtml() throws InterruptedException {
        String html = "html";
        sharedExperiment.logHtml(html, false);

        Thread.sleep(1000);

        Optional<String> htmlFetch = sharedCometApi.getHtml(sharedExperiment.getExperimentKey());

        Assert.assertTrue(htmlFetch.isPresent());
        Assert.assertEquals(html, htmlFetch.get());
    }

    @Test
    public void testLogCode() throws InterruptedException {
        String code = "code";
        sharedExperiment.logCode(code);

        Thread.sleep(1000);

        Optional<String> codeFetch = sharedCometApi.getCode(sharedExperiment.getExperimentKey());

        Assert.assertTrue(codeFetch.isPresent());
        Assert.assertEquals(code, codeFetch.get());
    }

    @Test
    public void testCopyStdout() throws InterruptedException {
        OnlineExperiment experiment =
                OnlineExperiment.builder(apiKey, projectName, workspace)
                        .interceptStdout()
                        .withConfig(getOverrideConfig())
                        .build();

        String loggedLine = "This should end up in Comet ML.";
        String loggedErrorLine = "This error should also get to Comet ML.";
        String nonLoggedLine = "This should not end up in Comet ML.";
        System.out.println(experiment.getExperimentKey());
        System.out.println(experiment.getExperimentLink().get());
        System.out.println(loggedLine);
        System.err.println(loggedErrorLine);
        System.out.flush();
        System.err.flush();

        Thread.sleep(1000);
        experiment.stopInterceptStdout();
        Thread.sleep(1000);

        System.out.println(nonLoggedLine);

        Optional<String> output = sharedCometApi.getOutput(experiment.getExperimentKey());

        Assert.assertTrue(output.isPresent());

        Assert.assertTrue(output.get().contains(experiment.getExperimentKey()));
        Assert.assertTrue(output.get().contains(loggedLine));
        Assert.assertTrue(output.get().contains(loggedErrorLine));
        Assert.assertFalse(output.get().contains(nonLoggedLine));
    }

    @Test
    public void testLogGraph() throws InterruptedException {
        String graph = "graph";
        sharedExperiment.logGraph("graph");

        Thread.sleep(1000);

        Optional<String> graphFetch = sharedCometApi.getGraph(sharedExperiment.getExperimentKey());

        Assert.assertTrue(graphFetch.isPresent());
        Assert.assertEquals(graph, graphFetch.get());
    }

    @Test
    public void testLogParam() throws InterruptedException {
        String paramName = "paramName";
        String paramValue = "paramValue";
        sharedExperiment.logParameter(paramName, paramValue);

        Thread.sleep(3000);

        Optional<ParametersResponse> response = sharedCometApi.getParameters(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getResults().size());
        Assert.assertEquals(paramName, response.get().getResults().get(0).getName());
        Assert.assertEquals(paramValue, response.get().getResults().get(0).getValueCurrent());
    }

    @Test
    public void testLogMetric() throws InterruptedException {
        String metricName = "metricName";
        String metricValue = "1.0";
        sharedExperiment.logMetric(metricName, metricValue);

        Thread.sleep(3000);

        Optional<MetricsResponse> response = sharedCometApi.getMetrics(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getResults().size());
        Assert.assertEquals(metricName, response.get().getResults().get(0).getName());
        Assert.assertEquals(metricValue, response.get().getResults().get(0).getValueCurrent());
    }

    @Test
    public void testLogOther() throws InterruptedException {
        String logOtherName = "logOtherName";
        String logOtherValue = "logOtherValue";
        sharedExperiment.logOther(logOtherName, logOtherValue);

        Thread.sleep(3000);

        Optional<LogOtherResponse> response = sharedCometApi.getLogOther(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getLogOtherList().size());
        Assert.assertEquals(logOtherName, response.get().getLogOtherList().get(0).getName());
        Assert.assertEquals(logOtherValue, response.get().getLogOtherList().get(0).getValueCurrent());
    }

    @Test
    public void testTags() throws InterruptedException {
        String tag = "tagName";
        sharedExperiment.addTag(tag);

        Thread.sleep(1000);

        Optional<TagsResponse> response = sharedCometApi.getTags(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getTags().size());
        Assert.assertEquals(tag, response.get().getTags().get(0));
    }

    @Test
    public void testUploadAssetAndImage() throws InterruptedException {
        String fileName = "fileName.txt";
        File file = new File(getClass().getClassLoader().getResource("Logo.psd").getFile());
        sharedExperiment.uploadAsset(file,fileName, false);

        String imageName = "imageName.psd";
        sharedExperiment.uploadImage(file, imageName, false);

        Thread.sleep(1000);

        Optional<AssetListResponse> response = sharedCometApi.getAssetList(sharedExperiment.getExperimentKey(), ASSET_TYPE_ALL);
        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(2, response.get().getAssets().size());

        response = sharedCometApi.getAssetList(sharedExperiment.getExperimentKey(), ASSET_TYPE_IMAGE);
        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getAssets().size());
        Assert.assertEquals(imageName, response.get().getAssets().get(0).getFileName());

        response = sharedCometApi.getAssetList(sharedExperiment.getExperimentKey(), ASSET_TYPE_UNKNOWN);
        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getAssets().size());
        Assert.assertEquals(fileName, response.get().getAssets().get(0).getFileName());
    }

    @Ignore
    @Test
    public void testCreateExperiment() {
        Experiment experiment = OnlineExperiment.of(apiKey, projectName, workspace);
    }

    @Test
    public void testExistingExperiment() {
        Experiment onlineExperiment =
                OnlineExperiment.builder(apiKey, projectName, workspace)
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
    public void testLogStartTime() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logStartTime(System.currentTimeMillis());
    }

    @Test
    public void testLogEndTime() {
        Experiment experiment = createAndRegisterExperiment();
        experiment.logEndTime(System.currentTimeMillis());
    }

    private Experiment createAndRegisterExperiment() {
        OnlineExperiment onlineExperiment =
                OnlineExperiment.builder(apiKey, projectName, workspace)
                        .withConfig(getOverrideConfig())
                        .build();
        onlineExperiment.setContext("context");
        return onlineExperiment;
    }

    private static File getOverrideConfig() {
        return new File(ContractTest.class.getClassLoader().getResource("overrides.conf").getFile());
    }
}
