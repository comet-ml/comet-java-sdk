package ml.comet.experiment.impl;

import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.asset.LoggedExperimentAssetImpl;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ml.comet.experiment.impl.ExperimentTestFactory.API_KEY;
import static ml.comet.experiment.impl.ExperimentTestFactory.PROJECT_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.WORKSPACE_NAME;
import static ml.comet.experiment.impl.ExperimentTestFactory.createApiExperiment;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.impl.asset.AssetType.TEXT_SAMPLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The integration tests to test {@link ApiExperiment} implementation by sending/retrieving data from the backend.
 */
@DisplayName("ApiExperimentTest INTEGRATION")
@Tag("integration")
public class ApiExperimentTest {
    static Map<String, Object> SOME_METADATA = new HashMap<String, Object>() {{
        put("someString", "string");
        put("someInt", 10);
    }};
    static String SOME_TEXT = "this is some text to be used";

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

    @Test
    public void testLogTextFull() throws Exception {
        try (ApiExperiment apiExperiment = createApiExperiment()) {
            ExperimentContext ctx = ExperimentContext.builder()
                    .withStep(100)
                    .withContext("train")
                    .build();

            apiExperiment.logText(SOME_TEXT, ctx, SOME_METADATA);

            // check that TEXT asset was saved as expected
            List<LoggedExperimentAsset> assets = apiExperiment.getAssetList(TEXT_SAMPLE.type());
            assertEquals(1, assets.size(), "wrong number of assets returned");
            LoggedExperimentAsset asset = assets.get(0);
            assertEquals(TEXT_SAMPLE.type(), asset.getType(), "wrong asset type");
            ExperimentContext assetContext = ((LoggedExperimentAssetImpl) asset).getContext();
            assertEquals(ctx.getStep(), assetContext.getStep(), "wrong asset's context step");
            assertEquals(ctx.getContext(), assetContext.getContext(), "wrong asset's context ID");
            assertEquals(SOME_METADATA, asset.getMetadata(), "wrong metadata:");
            assertEquals(SOME_TEXT.length(), asset.getSize().orElse(0L), "wrong asset size");
        }
    }

    @Test
    public void testLogTextShort() throws Exception {
        try (ApiExperiment apiExperiment = createApiExperiment()) {
            apiExperiment.logText(SOME_TEXT);

            // check that TEXT asset was saved as expected
            List<LoggedExperimentAsset> assets = apiExperiment.getAssetList(TEXT_SAMPLE.type());
            assertEquals(1, assets.size(), "wrong number of assets returned");

            LoggedExperimentAsset asset = assets.get(0);
            assertEquals(TEXT_SAMPLE.type(), asset.getType(), "wrong asset type");
            assertEquals(SOME_TEXT.length(), asset.getSize().orElse(0L), "wrong asset size");
            assertEquals(0, asset.getMetadata().size(), "no metadata expected");
            ExperimentContext assetContext = ((LoggedExperimentAssetImpl) asset).getContext();
            assertEquals(0, assetContext.getStep(), "no context step expected");
            assertTrue(StringUtils.isBlank(assetContext.getContext()), "no context ID expected");
        }
    }
}
