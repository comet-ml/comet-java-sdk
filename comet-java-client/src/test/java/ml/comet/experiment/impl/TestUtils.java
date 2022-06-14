package ml.comet.experiment.impl;

import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.model.Curve;
import ml.comet.experiment.model.DataPoint;
import org.awaitility.Awaitility;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Collection of utilities and test objects to be used across test suites.
 */
public class TestUtils {

    public static final Map<String, Object> SOME_METADATA = new HashMap<String, Object>() {{
        put("someInt", 10);
        put("someString", "test string");
        put("someBoolean", true);
    }};
    public static final String SOME_CONTEXT_ID = "SOME_CONTEXT_ID";

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

    public static Curve createCurve(String name, int pointsCount) {
        DataPoint[] dataPoints = new DataPoint[pointsCount];
        for (int i = 0; i < pointsCount; i++) {
            dataPoints[i] = DataPoint.of(i, i * 10);
        }
        return new Curve(dataPoints, name);
    }
}
