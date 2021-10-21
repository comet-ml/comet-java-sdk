package ml.comet.experiment;

import ml.comet.experiment.exception.CometGeneralException;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URL;

import static org.junit.Assert.fail;

public class ApiExperimentTest extends BaseApiTest {


    @Test(expected = CometGeneralException.class)
    public void testApiExperimentInitializedWithInvalidValues() {
        OnlineExperimentImpl.builder()
                .withMaxAuthRetries(1)
                .withUrlOverride("https://invalid.invalid")
                .withApiKey("invalid")
                .withWorkspace("invalid")
                .withProjectName("invalid")
                .build();
    }

    @Test
    public void testApiExperimentInitializedWithConfigOverride() {
        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource("empty-comet-config.conf");
            File file = new File(url.getPath());
            OnlineExperimentImpl.builder()
                    .withConfig(file)
                    .build();
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("Apikey is not specified!", ex.getMessage());
            return;
        }

        fail("expected to read comet override config file");
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

}
