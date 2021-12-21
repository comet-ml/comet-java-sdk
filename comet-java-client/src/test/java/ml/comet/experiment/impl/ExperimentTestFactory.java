package ml.comet.experiment.impl;

import ml.comet.experiment.OnlineExperiment;

import static ml.comet.experiment.impl.config.CometConfig.COMET_API_KEY;
import static ml.comet.experiment.impl.config.CometConfig.COMET_PROJECT_NAME;
import static ml.comet.experiment.impl.config.CometConfig.COMET_WORKSPACE_NAME;

/**
 * The factory to create initialized experiment instances for testing.
 */
public class ExperimentTestFactory {
    static String API_KEY;
    static String PROJECT_NAME;
    static String WORKSPACE_NAME;

    static {
        API_KEY = COMET_API_KEY.getString();
        PROJECT_NAME = COMET_PROJECT_NAME.getString();
        WORKSPACE_NAME = COMET_WORKSPACE_NAME.getString();
    }

    static OnlineExperiment createOnlineExperiment() {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withWorkspace(WORKSPACE_NAME)
                .withProjectName(PROJECT_NAME)
                .build();
    }
}
