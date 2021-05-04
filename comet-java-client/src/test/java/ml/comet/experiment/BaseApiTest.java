package ml.comet.experiment;

import ml.comet.experiment.env.EnvironmentVariableExtractor;
import org.junit.BeforeClass;

public class BaseApiTest {
    protected static String API_KEY;
    protected static String PROJECT_NAME;
    protected static String WORKSPACE_NAME;

    static {
        API_KEY = EnvironmentVariableExtractor.getApiKeyOrThrow();
        PROJECT_NAME = EnvironmentVariableExtractor.getProjectNameOrThrow();
        WORKSPACE_NAME = EnvironmentVariableExtractor.getWorkspaceNameOrThrow();
    }

    public static OnlineExperiment createOnlineExperiment() {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withWorkspace(WORKSPACE_NAME)
                .withProjectName(PROJECT_NAME)
                .build();
    }


}
