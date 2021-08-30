package ml.comet.experiment;

import ml.comet.experiment.utils.ConfigUtils;

public class BaseApiTest {
    protected static String API_KEY;
    protected static String PROJECT_NAME;
    protected static String WORKSPACE_NAME;

    static {
        API_KEY = ConfigUtils.getApiKeyOrThrow();
        PROJECT_NAME = ConfigUtils.getProjectNameOrThrow();
        WORKSPACE_NAME = ConfigUtils.getWorkspaceNameOrThrow();
    }

    public static OnlineExperiment createOnlineExperiment() {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withWorkspace(WORKSPACE_NAME)
                .withProjectName(PROJECT_NAME)
                .build();
    }


}
