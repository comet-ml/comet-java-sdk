package com.comet.experiment;

import com.comet.response.*;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.junit.*;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import static com.comet.experiment.Constants.*;
import static java.util.concurrent.TimeUnit.SECONDS;

public class ContractTest {
    private static String apiKey = null;
    private static String restApiKey = null;
    private static final String projectName = "testing-java-comet-library";
    private static final String workspace = "corneliusphi";
    private static String existingExperimentKey = null;

    private static Experiment sharedExperiment = null;
    private static CometApi cometApi = null;

    @BeforeClass
    public static void setupSharedExperiment() {
        apiKey = System.getenv("COMET_API_KEY");
        restApiKey = System.getenv("COMET_REST_API_KEY");
        existingExperimentKey = System.getenv("COMET_EXISTING_EXPERIMENT_KEY");
        sharedExperiment =
                OnlineExperiment.builder(apiKey, projectName, workspace)
                        .withConfig(getOverrideConfig())
                        .build();
        sharedExperiment.setStep(1);

        cometApi = CometApi.builder(restApiKey)
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
        Assert.assertFalse(workspaces.isEmpty());
    }

    @Test
    public void testGetProjectsAndExperiments() {
        List<ProjectRest> projects = cometApi.getAllProjects(workspace);

        Assert.assertFalse(projects.isEmpty());
        List<String> projectNames = projects.stream()
                .map(x -> x.getProject_name()).collect(Collectors.toList());
        Assert.assertTrue(projectNames.contains(projectName));
        Optional<ProjectRest> project = projects.stream()
                .filter(x -> x.getProject_name().equals(projectName)).findFirst();
        Assert.assertTrue(project.isPresent());

        List<ExperimentRest> experiments = cometApi.getAllExperiments(project.get().getProject_id());

        Assert.assertFalse(experiments.isEmpty());
    }

    @Test
    public void testGitMetadata() {
        sharedExperiment = createAndRegisterExperiment();
        GitMetadata gitMetadata = new GitMetadata("user", "root", "branch", "parent", "origin");
        sharedExperiment.logGitMetadata(gitMetadata);

        awaitData(() -> {
            Optional<GitMetadata> response = cometApi.getGitMetadata(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return StringUtils.isNotEmpty(response.get().getBranch());
        });
        awaitData(() -> cometApi.getGitMetadata(sharedExperiment.getExperimentKey()).isPresent());

        Optional<GitMetadata> gitMetadataFetch = cometApi.getGitMetadata(sharedExperiment.getExperimentKey());

        Assert.assertTrue(gitMetadataFetch.isPresent());
        Assert.assertEquals(gitMetadata.getBranch(), gitMetadataFetch.get().getBranch());
        Assert.assertEquals(gitMetadata.getOrigin(), gitMetadataFetch.get().getOrigin());
        Assert.assertEquals(gitMetadata.getParent(), gitMetadataFetch.get().getParent());
        Assert.assertEquals(gitMetadata.getRoot(), gitMetadataFetch.get().getRoot());
        Assert.assertEquals(gitMetadata.getUser(), gitMetadataFetch.get().getUser());
    }

    @Test
    public void testLogHtml() {
        String html = "html";
        sharedExperiment.logHtml(html, false);

        awaitData(() -> {
            Optional<String> response = cometApi.getHtml(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return StringUtils.isNotEmpty(response.get());
        });

        Optional<String> htmlFetch = cometApi.getHtml(sharedExperiment.getExperimentKey());

        Assert.assertTrue(htmlFetch.isPresent());
        Assert.assertEquals(html, htmlFetch.get());
    }

    @Test
    public void testLogCode() {
        String code = "code";
        sharedExperiment.logCode(code);

        awaitData(() -> {
            Optional<String> response = cometApi.getCode(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return StringUtils.isNotEmpty(response.get());
        });

        Optional<String> codeFetch = cometApi.getCode(sharedExperiment.getExperimentKey());

        Assert.assertTrue(codeFetch.isPresent());
        Assert.assertEquals(code, codeFetch.get());
    }

    @Test
    public void testCopyStdout() {
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

        experiment.stopInterceptStdout();

        System.out.println(nonLoggedLine);
        System.out.flush();

        awaitData(() -> {
            Optional<String> response = cometApi.getOutput(experiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return StringUtils.isNotEmpty(response.get());
        });

        Optional<String> output = cometApi.getOutput(experiment.getExperimentKey());

        Assert.assertTrue(output.isPresent());

        Assert.assertTrue(output.get().contains(experiment.getExperimentKey()));
        Assert.assertTrue(output.get().contains(loggedLine));
        Assert.assertTrue(output.get().contains(loggedErrorLine));
        Assert.assertFalse(output.get().contains(nonLoggedLine));
    }

    @Test
    public void testLogGraph() {
        String graph = "graph";
        sharedExperiment.logGraph("graph");

        awaitData(() -> {
            Optional<String> response = cometApi.getGraph(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return StringUtils.isNotEmpty(response.get());
        });

        Optional<String> graphFetch = cometApi.getGraph(sharedExperiment.getExperimentKey());

        Assert.assertTrue(graphFetch.isPresent());
        Assert.assertEquals(graph, graphFetch.get());
    }

    @Test
    public void testLogParam() {
        String paramName = "paramName";
        String paramValue = "paramValue";
        sharedExperiment.logParameter(paramName, paramValue);

        awaitData(() -> {
            Optional<ParametersResponse> response = cometApi.getParameters(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return !response.get().getResults().isEmpty();
        });

        Optional<ParametersResponse> response = cometApi.getParameters(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getResults().size());
        Assert.assertEquals(paramName, response.get().getResults().get(0).getName());
        Assert.assertEquals(paramValue, response.get().getResults().get(0).getValueCurrent());
    }

    @Test
    public void testLogMetric() {
        String metricName = "metricName";
        String metricValue = "1.0";
        sharedExperiment.logMetric(metricName, metricValue);

        awaitData(() -> {
            Optional<MetricsResponse> response = cometApi.getMetrics(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return !response.get().getResults().isEmpty();
        });

        Optional<MetricsResponse> response = cometApi.getMetrics(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getResults().size());
        Assert.assertEquals(metricName, response.get().getResults().get(0).getName());
        Assert.assertEquals(metricValue, response.get().getResults().get(0).getValueCurrent());
    }

    @Test
    public void testLogOther() {
        String logOtherName = "logOtherName";
        String logOtherValue = "logOtherValue";
        sharedExperiment.logOther(logOtherName, logOtherValue);

        awaitData(() -> {
            Optional<LogOtherResponse> response = cometApi.getLogOther(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return !response.get().getLogOtherList().isEmpty();
        });

        Optional<LogOtherResponse> response = cometApi.getLogOther(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getLogOtherList().size());
        Assert.assertEquals(logOtherName, response.get().getLogOtherList().get(0).getName());
        Assert.assertEquals(logOtherValue, response.get().getLogOtherList().get(0).getValueCurrent());
    }

    @Test
    public void testTags() {
        String tag = "tagName";
        sharedExperiment.addTag(tag);

        awaitData(() -> {
            Optional<TagsResponse> response = cometApi.getTags(sharedExperiment.getExperimentKey());
            if (!response.isPresent()) return false;
            return !response.get().getTags().isEmpty();
        });

        Optional<TagsResponse> response = cometApi.getTags(sharedExperiment.getExperimentKey());

        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getTags().size());
        Assert.assertEquals(tag, response.get().getTags().get(0));
    }

    @Test
    public void testUploadAssetAndImage() {
        String fileName = "fileName.txt";
        File file = new File(getClass().getClassLoader().getResource("Logo.psd").getFile());
        sharedExperiment.uploadAsset(file,fileName, false);

        String imageName = "imageName.psd";
        sharedExperiment.uploadImage(file, imageName, false);

        awaitData(() -> {
            Optional<AssetListResponse> response = cometApi.getAssetList(sharedExperiment.getExperimentKey(), ASSET_TYPE_ALL);
            if (!response.isPresent()) return false;
            return !response.get().getAssets().isEmpty();
        });

        Optional<AssetListResponse> response = cometApi.getAssetList(sharedExperiment.getExperimentKey(), ASSET_TYPE_ALL);
        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(2, response.get().getAssets().size());

        response = cometApi.getAssetList(sharedExperiment.getExperimentKey(), ASSET_TYPE_IMAGE);
        Assert.assertTrue(response.isPresent());
        Assert.assertEquals(1, response.get().getAssets().size());
        Assert.assertEquals(imageName, response.get().getAssets().get(0).getFileName());

        response = cometApi.getAssetList(sharedExperiment.getExperimentKey(), ASSET_TYPE_UNKNOWN);
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

    private void awaitData(BooleanSupplier booleanSupplier) {
        Awaitility.await().atMost(5, SECONDS).until(() -> booleanSupplier.getAsBoolean());
    }

    private static File getOverrideConfig() {
        return new File(ContractTest.class.getClassLoader().getResource("overrides.conf").getFile());
    }
}
