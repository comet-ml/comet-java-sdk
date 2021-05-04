package ml.comet.experiment;

import ml.comet.experiment.env.EnvironmentVariableExtractor;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ApiExperimentTest {
    private static String API_KEY;
    private static String PROJECT_NAME;
    private static String WORKSPACE_NAME;


    @BeforeClass
    public static void initEnvVariables() {
        API_KEY = EnvironmentVariableExtractor.getApiKeyOrThrow();
        PROJECT_NAME = EnvironmentVariableExtractor.getProjectNameOrThrow();
        WORKSPACE_NAME = EnvironmentVariableExtractor.getWorkspaceNameOrThrow();
    }

    @Test
    public void testApiExperimentInitialized() {
        OnlineExperiment experiment = createOnlineExperiment();
        String experimentKey = experiment.getExperimentKey();
        experiment.end();

        ApiExperiment apiExperiment = ApiExperiment.builder(experimentKey)
                .withApiKey(API_KEY)
                .build();

        Assert.assertEquals(WORKSPACE_NAME, apiExperiment.getWorkspaceName());
        Assert.assertEquals(PROJECT_NAME, apiExperiment.getProjectName());
    }

    private OnlineExperiment createOnlineExperiment() {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withWorkspace(WORKSPACE_NAME)
                .withProjectName(PROJECT_NAME)
                .build();
    }

}
