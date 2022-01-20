package ml.comet.experiment.impl;

import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.asset.LoggedExperimentAssetImpl;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Collection of utilities and test objects to be used across test suites.
 */
public class TestUtils {

    public static final Map<String, Object> SOME_METADATA = new HashMap<String, Object>() {{
        put("someInt", 10);
        put("someString", "test string");
        put("someBoolean", true);
    }};

    static final ExperimentContext SOME_FULL_CONTEXT =
            new ExperimentContext(10, 101, "train");

    /**
     * Allows getting file from test resources.
     *
     * @param name the resource file name.
     * @return the <code>File</code> object or <code>null</code> if resource not found.
     */
    public static File getFile(String name) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
        if (resource == null) {
            return null;
        }
        return new File(resource.getFile());
    }

    /**
     * Allows waiting for boolean supplier (condition) to return result.
     *
     * @param booleanSupplier the {@link BooleanSupplier} returning the result.
     * @param conditionAlias  the alias that will be shown if the default await timeouts (30 sec).
     */
    public static void awaitForCondition(BooleanSupplier booleanSupplier, String conditionAlias) {
        Awaitility.await(conditionAlias).atMost(30, SECONDS)
                .pollDelay(1, SECONDS)
                .pollInterval(300L, MILLISECONDS)
                .until(booleanSupplier::getAsBoolean);
    }

    /**
     * Allows waiting for boolean supplier (condition) to return result.
     *
     * @param booleanSupplier the {@link BooleanSupplier} returning the result.
     * @param conditionAlias  the alias that will be shown if the {@code timeoutSeconds} await expired.
     * @param timeoutSeconds  the await timeout in seconds.
     */
    public static void awaitForCondition(BooleanSupplier booleanSupplier, String conditionAlias, long timeoutSeconds) {
        Awaitility.await(conditionAlias).atMost(timeoutSeconds, SECONDS)
                .pollDelay(1, SECONDS)
                .pollInterval(300L, MILLISECONDS)
                .until(booleanSupplier::getAsBoolean);
    }

    /**
     * Validates that provided list of assets contains asset with provided logical path and this asset conforms to the
     * provided parameters.
     *
     * @param assets                   the list of assets to check against.
     * @param expectedAssetLogicalPath the logical path of asset in question.
     * @param expectedSize             the expected size of the asset.
     * @param expectedContext          the expected {@link ExperimentContext} to be associated with the asset.
     */
    public static void validateAsset(List<LoggedExperimentAsset> assets, String expectedAssetLogicalPath,
                                     long expectedSize, ExperimentContext expectedContext) {
        validateAsset(assets, expectedAssetLogicalPath, expectedSize, null, expectedContext);
    }

    /**
     * Validates that provided list of assets contains asset with provided logical path and this asset conforms to the
     * provided parameters.
     *
     * @param assets                   the list of assets to check against.
     * @param expectedAssetLogicalPath the logical path of asset in question.
     * @param expectedSize             the expected size of the asset.
     * @param expectedMetadata         the expected metadata to be associated with the asset.
     * @param expectedContext          the expected {@link ExperimentContext} to be associated with the asset.
     */
    public static void validateAsset(List<LoggedExperimentAsset> assets, String expectedAssetLogicalPath,
                                     long expectedSize, Map<String, Object> expectedMetadata,
                                     ExperimentContext expectedContext) {
        assertTrue(assets.stream()
                .filter(asset -> expectedAssetLogicalPath.equals(asset.getLogicalPath()))
                .anyMatch(asset -> {
                    ExperimentContext assetContext = ((LoggedExperimentAssetImpl) asset).getContext();
                    boolean res = expectedSize == asset.getSize().orElse(0L)
                            && Objects.equals(expectedContext.getStep(), assetContext.getStep());
                    if (StringUtils.isNotBlank(expectedContext.getContext())) {
                        res = res & expectedContext.getContext().equals(assetContext.getContext());
                    }
                    if (expectedMetadata != null) {
                        assertEquals(expectedMetadata, asset.getMetadata(), "wrong metdata");
                    } else {
                        assertEquals(0, asset.getMetadata().size(), "empty metadata expected");
                    }
                    return res;
                }));
    }
}
