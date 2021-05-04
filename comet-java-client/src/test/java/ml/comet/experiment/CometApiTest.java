package ml.comet.experiment;

import ml.comet.experiment.env.EnvironmentVariableExtractor;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.RestProject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

@Ignore
public class CometApiTest {
    private static String API_KEY;
    private static String PROJECT_NAME;
    private static String WORKSPACE_NAME;
    private static CometApi COMET_API;
    private static OnlineExperiment SHARED_EXPERIMENT;

    @BeforeClass
    public static void initEnvVariables() {
        API_KEY = EnvironmentVariableExtractor.getApiKeyOrThrow();
        PROJECT_NAME = EnvironmentVariableExtractor.getProjectNameOrThrow();
        WORKSPACE_NAME = EnvironmentVariableExtractor.getWorkspaceNameOrThrow();
        COMET_API = createCometApi();
        SHARED_EXPERIMENT = createExperiment();
    }

    @AfterClass
    public static void shutdownSharedExperiment() {
        if (SHARED_EXPERIMENT != null) {
            SHARED_EXPERIMENT.end();
        }
    }

    @Test
    public void testGetsAllWorkspaces() {
        List<String> workspaces = COMET_API.getAllWorkspaces();
        Assert.assertFalse(workspaces.isEmpty());
        Assert.assertTrue(workspaces.contains(WORKSPACE_NAME));
    }

    @Test
    public void testGetsAllProjects() {
        List<RestProject> projects = COMET_API.getAllProjects(WORKSPACE_NAME);
        Assert.assertFalse(projects.isEmpty());
        boolean projectExists = projects.stream()
                .anyMatch(project -> PROJECT_NAME.equals(project.getProjectName()));
        Assert.assertTrue(projectExists);
    }

    @Test
    public void testGetsAllExperiments() {
        List<RestProject> projects = COMET_API.getAllProjects(WORKSPACE_NAME);
        Assert.assertFalse(projects.isEmpty());
        Optional<List<ExperimentMetadataRest>> experimentsOpt = projects.stream()
                .filter(project -> PROJECT_NAME.equals(project.getProjectName()))
                .findFirst()
                .map(project -> COMET_API.getAllExperiments(project.getProjectId()));
        Assert.assertTrue(experimentsOpt.isPresent());
        List<ExperimentMetadataRest> experiments = experimentsOpt.get();
        Assert.assertFalse(experiments.isEmpty());
        boolean experimentExists = experiments.stream()
                .anyMatch(experiment -> SHARED_EXPERIMENT.getExperimentKey().equals(experiment.getExperimentKey()));
        Assert.assertTrue(experimentExists);
    }

    private static CometApiImpl createCometApi() {
        return CometApiImpl.builder()
                .withApiKey(API_KEY)
                .build();
    }

    private static OnlineExperiment createExperiment() {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withWorkspace(WORKSPACE_NAME)
                .withProjectName(PROJECT_NAME)
                .build();
    }

}
