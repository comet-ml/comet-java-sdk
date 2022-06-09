package ml.comet.experiment.impl;

import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.ExperimentBuilder;
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
        return ExperimentBuilder.OnlineExperiment()
                .withWorkspace(WORKSPACE_NAME)
                .withProjectName(PROJECT_NAME)
                .withApiKey(API_KEY)
                .build();
    }

    static ApiExperiment createApiExperiment() {
        return ExperimentBuilder.ApiExperiment()
                .withProjectName(PROJECT_NAME)
                .withWorkspace(WORKSPACE_NAME)
                .withApiKey(API_KEY)
                .build();
    }
}
