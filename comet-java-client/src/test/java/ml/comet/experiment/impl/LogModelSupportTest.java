package ml.comet.experiment.impl;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.impl.TestUtils.SOME_FULL_CONTEXT;
import static ml.comet.experiment.impl.TestUtils.SOME_METADATA;
import static ml.comet.experiment.impl.asset.AssetType.MODEL_ELEMENT;
import static ml.comet.experiment.impl.TestUtils.awaitForCondition;
import static ml.comet.experiment.impl.TestUtils.validateAsset;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The integration tests to test {@link OnlineExperiment} implementation related to log model methods.
 */
@DisplayName("LogModelSupportTest INTEGRATION")
@Tag("integration")
public class LogModelSupportTest extends AssetsBaseTest {

    private static final String SOME_MODEL_NAME = "someModelName";
    private static final String SOME_MODEL_LOGICAL_PATH = "someModel.dat";
    private static final String SOME_MODEL_DATA = "some model data string";

    @Test
    public void testLogAndGetModelFile() {
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            // Make sure experiment has no assets
            //
            assertTrue(experiment.getAllAssetList().isEmpty());

            // log model file and check results
            //
            experiment.logModel(SOME_MODEL_NAME, Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)),
                    CODE_FILE_NAME, true, SOME_METADATA, SOME_FULL_CONTEXT);

            awaitForCondition(() -> !experiment.getAssetList(MODEL_ELEMENT.type()).isEmpty(),
                    "Logged model file");

            List<LoggedExperimentAsset> assets = experiment.getAssetList(MODEL_ELEMENT.type());
            validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE, SOME_METADATA, SOME_FULL_CONTEXT);

        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testLogAndGetModelData() {
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            // Make sure experiment has no assets
            //
            assertTrue(experiment.getAllAssetList().isEmpty());

            // log model data and check results
            //
            byte []data = SOME_MODEL_DATA.getBytes(StandardCharsets.UTF_8);
            experiment.logModel(SOME_MODEL_NAME, data, SOME_MODEL_LOGICAL_PATH, true,
                    SOME_METADATA, SOME_FULL_CONTEXT);

            awaitForCondition(() -> !experiment.getAssetList(MODEL_ELEMENT.type()).isEmpty(),
                    "Logged model data");

            List<LoggedExperimentAsset> assets = experiment.getAssetList(MODEL_ELEMENT.type());
            validateAsset(assets, SOME_MODEL_LOGICAL_PATH, data.length, SOME_METADATA, SOME_FULL_CONTEXT);

        } catch (Exception e) {
            fail(e);
        }
    }
}
