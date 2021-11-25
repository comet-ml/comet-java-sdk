package ml.comet.experiment.impl;

import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.exception.CometGeneralException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApiExperimentImplTest extends BaseApiTest {

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
    public void testApiExperimentInitialized() {
        OnlineExperiment experiment = createOnlineExperiment();
        String experimentKey = experiment.getExperimentKey();
        experiment.end();

        ApiExperiment apiExperiment = ApiExperimentImpl.builder(experimentKey)
                .withApiKey(API_KEY)
                .build();

        assertEquals(WORKSPACE_NAME, apiExperiment.getWorkspaceName());
        assertEquals(PROJECT_NAME, apiExperiment.getProjectName());

        apiExperiment.end();
    }
}
