package ml.comet.experiment.impl;

import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.asset.LoggedExperimentAssetImpl;
import ml.comet.experiment.model.Curve;
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
import static ml.comet.experiment.impl.TestUtils.SOME_FULL_CONTEXT;
import static ml.comet.experiment.impl.TestUtils.createCurve;
import static ml.comet.experiment.impl.asset.AssetType.CURVE;
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

    @Test
    public void testLogCurve() throws Exception {
        try (ApiExperiment apiExperiment = createApiExperiment()) {
            // check that no curve was logged
            assertTrue(apiExperiment.getAllAssetList().isEmpty());

            String fileName = "someCurve";
            Curve curve = createCurve(fileName, 10);
            apiExperiment.logCurve(curve, true, SOME_FULL_CONTEXT);

            // check that CURVE asset was saved as expected
            List<LoggedExperimentAsset> assets = apiExperiment.getAssetList(CURVE.type());
            assertEquals(1, assets.size(), "wrong number of assets returned");

            LoggedExperimentAsset asset = assets.get(0);
            assertEquals(CURVE.type(), asset.getType(), "wrong asset type");
            assertEquals(fileName, asset.getLogicalPath(), "wrong asset path");
            assertEquals(0, asset.getMetadata().size(), "no metadata expected");
            ExperimentContext assetContext = ((LoggedExperimentAssetImpl) asset).getContext();
            assertEquals(SOME_FULL_CONTEXT.getStep(), assetContext.getStep(), "wrong context step");
            assertEquals(SOME_FULL_CONTEXT.getContext(), assetContext.getContext(), "wrong context ID");
        }
    }

    @Test
    public void testLogCurveOverwrite() throws Exception {
        String experimentKey;
        String fileName = "someCurve";
        int pointsCount = 10;
        long size;
        try (ApiExperiment apiExperiment = createApiExperiment()) {
            Curve curve = createCurve(fileName, pointsCount);
            apiExperiment.logCurve(curve, false, SOME_FULL_CONTEXT);

            // check that CURVE asset was saved as expected
            List<LoggedExperimentAsset> assets = apiExperiment.getAssetList(CURVE.type());
            assertEquals(1, assets.size(), "wrong number of assets returned");

            LoggedExperimentAsset asset = assets.get(0);
            assertEquals(fileName, asset.getLogicalPath(), "wrong asset path");
            size = asset.getSize().orElse((long) -1);
            assertTrue(size > 0, "wrong asset size");

            experimentKey = apiExperiment.getExperimentKey();
        }

        // create new experiment and overwrite curve
        try (ApiExperiment apiExperiment = createApiExperiment(experimentKey)) {
            // overwrite created curve with bigger ones
            //
            Curve curve = createCurve(fileName, pointsCount * 2);
            apiExperiment.logCurve(curve, true, SOME_FULL_CONTEXT);

            List<LoggedExperimentAsset> assets = apiExperiment.getAssetList(CURVE.type());
            if (assets.size() > 1) {
                for (LoggedExperimentAsset asset : assets) {
                    System.out.println(asset.getLogicalPath());
                }
            }
            assertEquals(1, assets.size(), "wrong number of assets returned");

            long newSize = assets.get(0).getSize().orElse((long) -1);
            assertTrue(newSize > size);
        }
    }
}
