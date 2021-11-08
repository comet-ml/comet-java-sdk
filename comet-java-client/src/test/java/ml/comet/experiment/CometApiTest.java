package ml.comet.experiment;

import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.RestProject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CometApiTest extends BaseApiTest {
    private static CometApi COMET_API;
    private static OnlineExperiment SHARED_EXPERIMENT;

    @BeforeAll
    public static void initEnvVariables() {
        COMET_API = CometApiImpl.builder().withApiKey(API_KEY).build();
        SHARED_EXPERIMENT = createOnlineExperiment();
    }

    @AfterAll
    public static void shutdownSharedExperiment() throws IOException {
        SHARED_EXPERIMENT.end();
        COMET_API.close();
    }

    @Test
    public void testGetsAllWorkspaces() {
        List<String> workspaces = COMET_API.getAllWorkspaces();
        assertFalse(workspaces.isEmpty());
        assertTrue(workspaces.contains(WORKSPACE_NAME));
    }

    @Test
    public void testGetsAllProjects() {
        List<RestProject> projects = COMET_API.getAllProjects(WORKSPACE_NAME);
        assertFalse(projects.isEmpty());
        boolean projectExists = projects.stream()
                .anyMatch(project -> PROJECT_NAME.equals(project.getProjectName()));
        assertTrue(projectExists);
    }

    @Test
    public void testGetsAllExperiments() {
        List<RestProject> projects = COMET_API.getAllProjects(WORKSPACE_NAME);
        assertFalse(projects.isEmpty());
        Optional<List<ExperimentMetadataRest>> experimentsOpt = projects.stream()
                .filter(project -> PROJECT_NAME.equals(project.getProjectName()))
                .findFirst()
                .map(project -> COMET_API.getAllExperiments(project.getProjectId()));
        assertTrue(experimentsOpt.isPresent());
        List<ExperimentMetadataRest> experiments = experimentsOpt.get();
        assertFalse(experiments.isEmpty());
        boolean experimentExists = experiments.stream()
                .anyMatch(experiment -> SHARED_EXPERIMENT.getExperimentKey().equals(experiment.getExperimentKey()));
        assertTrue(experimentExists);
    }

}
