package ml.comet.experiment;


import ml.comet.experiment.config.CometConfig;

import static ml.comet.experiment.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.config.CometConfig.COMET_PROJECT_NAME;
import static ml.comet.experiment.config.CometConfig.COMET_WORKSPACE_NAME;

public class BaseApiTest {
    protected static String API_KEY;
    protected static String PROJECT_NAME;
    protected static String WORKSPACE_NAME;

    static {
        API_KEY = COMET_API_KEY.getString();
        PROJECT_NAME = COMET_PROJECT_NAME.getString();
        WORKSPACE_NAME = COMET_WORKSPACE_NAME.getString();
    }

    public static OnlineExperiment createOnlineExperiment() {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withWorkspace(WORKSPACE_NAME)
                .withProjectName(PROJECT_NAME)
                .build();
    }


}
