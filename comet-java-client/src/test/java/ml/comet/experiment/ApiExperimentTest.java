package ml.comet.experiment;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class ApiExperimentTest extends BaseApiTest {

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

}
