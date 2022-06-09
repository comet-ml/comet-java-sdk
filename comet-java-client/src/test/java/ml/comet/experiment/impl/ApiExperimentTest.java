package ml.comet.experiment.impl;

import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.exception.CometGeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static ml.comet.experiment.impl.ExperimentTestFactory.API_KEY;
import static ml.comet.experiment.impl.ExperimentTestFactory.PROJECT_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.WORKSPACE_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The integration tests to test {@link ApiExperiment} implementation by sending/retrieving data from the backend.
 */
@DisplayName("ApiExperimentTest INTEGRATION")
@Tag("integration")
public class ApiExperimentTest {

    @Test
    public void testApiExperimentInitializedWithInvalidValues() {
        assertThrows(CometGeneralException.class, () -> OnlineExperimentImpl.builder()
                .withMaxAuthRetries(1)
                .withUrlOverride("https://invalid.invalid")
                .withApiKey("invalid")
                .withWorkspace("invalid")
                .withProjectName("invalid")
                .build());
    }

    @Test
    public void testApiExperimentInitialized() throws Exception {
        String experimentKey;
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            experimentKey = experiment.getExperimentKey();
        }

        try (ApiExperiment apiExperiment = ApiExperimentImpl.builder(experimentKey).withApiKey(API_KEY).build()) {
            assertEquals(WORKSPACE_NAME, apiExperiment.getWorkspaceName());
            assertEquals(PROJECT_NAME, apiExperiment.getProjectName());
        }
    }
}
