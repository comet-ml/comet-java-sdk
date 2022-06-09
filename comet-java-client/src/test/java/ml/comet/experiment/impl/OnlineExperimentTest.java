package ml.comet.experiment.impl;

import io.reactivex.rxjava3.functions.Action;
import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.GitMetaData;
import ml.comet.experiment.model.Value;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static ml.comet.experiment.impl.ExperimentTestFactory.API_KEY;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.impl.TestUtils.SOME_FULL_CONTEXT;
import static ml.comet.experiment.impl.TestUtils.awaitForCondition;
import static ml.comet.experiment.impl.asset.AssetType.SOURCE_CODE;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_REGISTER_EXPERIMENT;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.CometUtils.fullMetricName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The integration tests to test {@link OnlineExperiment} implementation by sending/retrieving data from the backend.
 */
@DisplayName("OnlineExperimentTest INTEGRATION")
@Tag("integration")
public class OnlineExperimentTest extends AssetsBaseTest {
    private static final String SOME_NAME = "someName";
    private static final String SOME_PARAMETER = "someParameter";
    private static final String SOME_PARAMETER_VALUE = "122.0";
    private static final String ANOTHER_PARAMETER = "anotherParameter";
    private static final double ANOTHER_PARAMETER_VALUE = 123.0;
    private static final String SOME_HTML = "<!DOCTYPE html><html lang=\"en\"><body><p>some html</p></body></html>";
    private static final String ANOTHER_HTML = "<!DOCTYPE html><html lang=\"en\"><body><p>another html</p></body></html>";
    private static final String JOINED_HTML = ANOTHER_HTML + SOME_HTML;
    private static final String SOME_TEXT = "Some text";
    private static final String ANOTHER_TAG = "Another tag";
    private static final String SOME_GRAPH = "{\"keras_version\": \"2.1.2\",\"backend\": \"tensorflow\"}";

    private static final String LOGGED_LINE = "This should end up in Comet ML.";
    private static final String LOGGED_ERROR_LINE = "This error should also get to Comet ML.";
    private static final String NON_LOGGED_LINE = "This should not end up in Comet ML.";

    private static final ExperimentContext SOME_PARTIAL_CONTEXT =
            new ExperimentContext(10, 101);

    @Test
    public void testExperimentCreatedAndShutDown() throws Exception {
        String experimentKey;
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            experimentKey = experiment.getExperimentKey();
            Optional<String> experimentLink = experiment.getExperimentLink();

            assertTrue(StringUtils.isNotBlank(experimentKey));
            assertTrue(experimentLink.isPresent());
            assertTrue(StringUtils.isNotBlank(experimentLink.get()));

            awaitForCondition(() -> experiment.getMetadata().isRunning(), "Experiment must become running");

            ExperimentMetadata metadata = experiment.getMetadata();
            assertEquals(experiment.getExperimentKey(), metadata.getExperimentKey());
            assertEquals(experiment.getWorkspaceName(), metadata.getWorkspaceName());
            assertEquals(experiment.getProjectName(), metadata.getProjectName());
        }

        // use REST API to check experiment status
        try (ApiExperiment apiExperiment = ApiExperimentImpl.builder(experimentKey).build()) {
            awaitForCondition(() -> !apiExperiment.getMetadata().isRunning(),
                    "Experiment running status updated", 60);
            assertFalse(apiExperiment.getMetadata().isRunning(), "Experiment must have status not running");

        }
    }

    @Test
    public void testInitAndUpdateExistingExperiment() throws Exception {
        // create dummy experiment and make sure it has default name assigned by backend
        String experimentKey;
        String experimentName;
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            experiment.end();
            assertNotNull(experiment.getExperimentName());

            experimentKey = experiment.getExperimentKey();
            experimentName = experiment.getExperimentName();
        }

        // get previous experiment by key and check that update is working
        try (OnlineExperiment updatedExperiment = onlineExperiment(experimentKey)) {
            updatedExperiment.setExperimentName(SOME_NAME);

            awaitForCondition(
                    () -> SOME_NAME.equals(updatedExperiment.getMetadata().getExperimentName()),
                    "Experiment name updated timeout");
            assertNotEquals(experimentName, updatedExperiment.getExperimentName(),
                    "experiment name should be different");
        }
    }

    @Test
    public void testSetAndGetExperimentName() throws Exception {
        try (OnlineExperiment experiment = createOnlineExperiment()) {

            ExperimentMetadata metadata = experiment.getMetadata();
            String generatedExperimentName = metadata.getExperimentName();
            assertTrue(StringUtils.isNoneEmpty(generatedExperimentName));

            experiment.setExperimentName(SOME_NAME);

            awaitForCondition(() -> SOME_NAME.equals(experiment.getMetadata().getExperimentName()),
                    "Experiment name update timeout");

            ExperimentMetadata updatedMetadata = experiment.getMetadata();
            assertEquals(experiment.getExperimentKey(), metadata.getExperimentKey());
            assertEquals(SOME_NAME, updatedMetadata.getExperimentName());
        }
    }

    @Test
    public void testLogAndGetMetric() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            // Test metric with full context (including context ID)
            //
            OnCompleteAction onComplete = new OnCompleteAction();
            experiment.logMetric(
                    SOME_PARAMETER, SOME_PARAMETER_VALUE, TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));
            awaitForCondition(onComplete, "logMetricAsync onComplete timeout");

            // Test metric with partial context (without context ID)
            //
            experiment.setContext(StringUtils.EMPTY);
            onComplete = new OnCompleteAction();
            experiment.logMetric(
                    ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE, SOME_PARTIAL_CONTEXT, Optional.of(onComplete));
            awaitForCondition(onComplete, "logMetricAsync onComplete timeout");

            // Wait for metrics to become available and check results
            //
            awaitForCondition(() -> experiment.getMetrics().size() == 2, "experiment metrics get timeout");

            List<Value> metrics = experiment.getMetrics();
            validateValues(metrics, fullMetricName(SOME_PARAMETER, TestUtils.SOME_FULL_CONTEXT), SOME_PARAMETER_VALUE);
            validateValues(metrics, fullMetricName(ANOTHER_PARAMETER, SOME_PARTIAL_CONTEXT), ANOTHER_PARAMETER_VALUE);
        } catch (Throwable throwable) {
            fail(throwable);
        }
    }

    @Test
    public void testLogAndGetParameter() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {

            // Test log parameter with full context
            //
            OnCompleteAction onComplete = new OnCompleteAction();
            experiment.logParameter(
                    SOME_PARAMETER, SOME_PARAMETER_VALUE, TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));
            awaitForCondition(onComplete, "logParameterAsync onComplete timeout");

            // Test log parameter with partial context
            //
            onComplete = new OnCompleteAction();
            experiment.logParameter(
                    ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE, SOME_PARTIAL_CONTEXT, Optional.of(onComplete));
            awaitForCondition(onComplete, "logParameterAsync onComplete timeout");

            // Wait for parameters to become available and check results
            //
            awaitForCondition(() -> experiment.getParameters().size() == 2, "experiment parameters get timeout");
            List<Value> params = experiment.getParameters();
            validateValues(params, SOME_PARAMETER, SOME_PARAMETER_VALUE);
            validateValues(params, ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE);

        } catch (Throwable throwable) {
            fail(throwable);
        }
    }

    @Test
    public void testLogAndGetOther() throws Exception {
        try (OnlineExperiment experiment = createOnlineExperiment()) {

            // Check that experiment has no extra other data yet
            //
            List<Value> parameters = experiment.getLogOther();
            assertEquals(1, parameters.size());
            assertTrue(parameters.stream().anyMatch(p -> "Name".equals(p.getName())));

            // Log some other data
            //
            Map<String, Object> params = new HashMap<>();
            params.put(SOME_PARAMETER, SOME_PARAMETER_VALUE);
            params.put(ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE);

            params.forEach((key, value) -> {
                OnCompleteAction onComplete = new OnCompleteAction();
                ((OnlineExperimentImpl) experiment).logOther(key, value, Optional.of(onComplete));
                awaitForCondition(onComplete, "logOtherAsync onComplete timeout");
            });

            // Get saved other data and check
            //
            awaitForCondition(() -> experiment.getLogOther().size() == 3, "get other timeout");

            List<Value> updatedParameters = experiment.getLogOther();
            params.forEach((k, v) -> validateValues(updatedParameters, k, v));

        }
    }

    @Test
    public void testLogAndGetHtml() throws Exception {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {

            assertFalse(experiment.getHtml().isPresent());

            // Create first HTML record
            //
            OnCompleteAction onComplete = new OnCompleteAction();
            experiment.logHtml(SOME_HTML, true, Optional.of(onComplete));

            // sleep to make sure the request was sent
            awaitForCondition(onComplete, "onComplete timeout");

            awaitForCondition(() -> {
                Optional<String> html = experiment.getHtml();
                return html.isPresent() && SOME_HTML.equals(html.get());
            }, "Experiment SOME_HTML update timeout", 60);

            // Override first HTML record
            //
            onComplete = new OnCompleteAction();
            experiment.logHtml(ANOTHER_HTML, true, Optional.of(onComplete));

            // sleep to make sure the request was sent
            awaitForCondition(onComplete, "onComplete timeout");

            awaitForCondition(() -> {
                Optional<String> html = experiment.getHtml();
                return html.isPresent() && ANOTHER_HTML.equals(html.get());
            }, "Experiment ANOTHER_HTML update timeout");

            // Check that HTML record was not overridden but appended
            //
            onComplete = new OnCompleteAction();
            experiment.logHtml(SOME_HTML, false, Optional.of(onComplete));

            // sleep to make sure the request was sent
            awaitForCondition(onComplete, "onComplete timeout");

            awaitForCondition(() -> {
                Optional<String> html = experiment.getHtml();
                return html.isPresent() && JOINED_HTML.equals(html.get());
            }, "Experiment JOINED_HTML update timeout");

        }
    }

    @Test
    public void testAddAndGetTag() throws Exception {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {

            // Check that experiment has no TAGs
            assertTrue(experiment.getTags().isEmpty());

            // Add TAGs and wait for response
            //
            OnCompleteAction onComplete = new OnCompleteAction();
            experiment.addTag(SOME_TEXT, Optional.of(onComplete));
            awaitForCondition(onComplete, "onComplete timeout");

            onComplete = new OnCompleteAction();
            experiment.addTag(ANOTHER_TAG, Optional.of(onComplete));
            awaitForCondition(onComplete, "onComplete timeout");

            // Get new TAGs and check
            //
            awaitForCondition(() -> experiment.getTags().size() == 2, "Experiment get tags timeout");

            List<String> tags = experiment.getTags();
            assertTrue(tags.contains(SOME_TEXT));
            assertTrue(tags.contains(ANOTHER_TAG));

        }
    }

    @Test
    public void testLogAndGetGraph() throws Exception {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {

            // Check that experiment has no Graph
            //
            Optional<String> graph = experiment.getGraph();
            assertTrue(!graph.isPresent() || graph.get().isEmpty());

            // Log Graph and wait for response
            //
            OnCompleteAction onComplete = new OnCompleteAction();
            experiment.logGraph(SOME_GRAPH, Optional.of(onComplete));
            awaitForCondition(onComplete, "onComplete timeout");

            // Get graph and check result
            //
            awaitForCondition(() -> {
                Optional<String> graphOpt = experiment.getGraph();
                return graphOpt.isPresent() && SOME_GRAPH.equals(graphOpt.get());
            }, "Experiment get graph timeout");

        }
    }

    @Test
    public void testLogAndGetExperimentTime() {
        String experimentKey = null;
        Instant startTimeMillis = null;
        Instant endTimeMillis = null;
        // Create experiment and get experiment metadata
        //
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            ExperimentMetadata metadata = experiment.getMetadata();
            startTimeMillis = metadata.getStartTime();
            endTimeMillis = metadata.getEndTime();
            experimentKey = experiment.getExperimentKey();
        } catch (Throwable t) {
            fail(t);
        }

        assertNotNull(experimentKey, "experiment key expected");
        assertNotNull(startTimeMillis, "start time expected");
        assertNotNull(endTimeMillis, "end time expected");

        // fetch existing experiment and update time
        //
        try (OnlineExperimentImpl existingExperiment = (OnlineExperimentImpl) onlineExperiment(experimentKey)) {
            long now = System.currentTimeMillis();

            OnCompleteAction onComplete = new OnCompleteAction();
            existingExperiment.logStartTime(now, Optional.of(onComplete));
            awaitForCondition(onComplete, "logStartTime onComplete timeout", 120);

            onComplete = new OnCompleteAction();
            existingExperiment.logEndTime(now, Optional.of(onComplete));
            awaitForCondition(onComplete, "logEndTime onComplete timeout", 120);

            // Get updated experiment metadata and check results
            //
            awaitForCondition(() -> {
                ExperimentMetadata data = existingExperiment.getMetadata();
                return data.getStartTime().toEpochMilli() == now && data.getEndTime().toEpochMilli() == now;
            }, "Experiment get start/stop time timeout", 240);

            ExperimentMetadata updatedMetadata = existingExperiment.getMetadata();
            assertNotEquals(startTimeMillis, updatedMetadata.getStartTime());
            assertNotEquals(endTimeMillis, updatedMetadata.getEndTime());
        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    public void testSetsContext() {
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            assertTrue(experiment.getAllAssetList().isEmpty());

            experiment.setContext(SOME_TEXT);
            experiment.uploadAsset(TestUtils.getFile(SOME_TEXT_FILE_NAME), false);

            awaitForCondition(() -> experiment.getAllAssetList().size() == 1, "Asset uploaded");

            Optional<LoggedExperimentAsset> assetOpt = experiment.getAllAssetList()
                    .stream()
                    .filter(asset -> SOME_TEXT_FILE_NAME.equals(asset.getLogicalPath()))
                    .findFirst();
            assertTrue(assetOpt.isPresent());
            assertTrue(assetOpt.get().getExperimentContext().isPresent(), "experiment context expected");
            assertEquals(SOME_TEXT, assetOpt.get().getExperimentContext().get().getContext());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testLogAndGetGitMetadata() throws Exception {
        try (OnlineExperiment experiment = createOnlineExperiment()) {

            // Get GIT metadata and check that it is not set
            //
            GitMetaData gitMetadata = experiment.getGitMetadata();
            assertNull(gitMetadata.getUser());
            assertNull(gitMetadata.getBranch());
            assertNull(gitMetadata.getOrigin());

            // Create and update GIT metadata and wait for response
            //
            OnCompleteAction onComplete = new OnCompleteAction();
            GitMetaData request = new GitMetaData(
                    "user", "root", "branch", "parent", "origin");
            ((OnlineExperimentImpl) experiment).logGitMetadataAsync(request, Optional.of(onComplete));
            awaitForCondition(onComplete, "onComplete timeout");

            // Get GIT metadata and check results
            //
            awaitForCondition(() -> request.getUser().equals(experiment.getGitMetadata().getUser()),
                    "Git metadata user timeout");

            GitMetaData updatedMetadata = experiment.getGitMetadata();
            assertEquals(updatedMetadata.getOrigin(), request.getOrigin());
            assertEquals(updatedMetadata.getBranch(), request.getBranch());
            assertEquals(updatedMetadata.getRoot(), request.getRoot());
            assertEquals(updatedMetadata.getParent(), request.getParent());

        }
    }


    @Test
    public void testCopyStdout() {
        try (OnlineExperiment experiment = createOnlineExperiment()) {
            experiment.setInterceptStdout();

            System.out.println(experiment.getExperimentKey());
            System.out.println(experiment.getProjectName());
            System.out.println(LOGGED_LINE);
            System.err.println(LOGGED_ERROR_LINE);
            System.out.flush();
            System.err.flush();

            // wait for flush to complete before stopping interception
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            experiment.stopInterceptStdout();

            System.out.println(NON_LOGGED_LINE);
            System.out.flush();

            awaitForCondition(() -> experiment.getOutput()
                    .filter(log -> log.contains(experiment.getExperimentKey()))
                    .filter(log -> log.contains(experiment.getProjectName()))
                    .filter(log -> log.contains(LOGGED_LINE.concat("\n")))
                    .filter(log -> log.contains(LOGGED_ERROR_LINE.concat("\n")))
                    .filter(log -> !log.contains(NON_LOGGED_LINE))
                    .isPresent(), "Experiment logs added");
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testLogAndGetFileCode() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {

            // check that no code was logged
            //
            assertTrue(experiment.getAllAssetList().isEmpty());

            // log code and check results
            //
            experiment.logCode(Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)), TestUtils.SOME_FULL_CONTEXT);

            awaitForCondition(() -> !experiment.getAssetList(SOURCE_CODE.type()).isEmpty(),
                    "Experiment code from file added");
            List<LoggedExperimentAsset> assets = experiment.getAssetList(SOURCE_CODE.type());
            validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE, TestUtils.SOME_FULL_CONTEXT);

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    public void testLogAndGetRawCode() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {

            // check that no code was logged
            //
            assertTrue(experiment.getAllAssetList().isEmpty());

            // log code and check results
            //
            experiment.logCode(SOME_TEXT, CODE_FILE_NAME, SOME_FULL_CONTEXT);

            awaitForCondition(() -> !experiment.getAssetList(SOURCE_CODE.type()).isEmpty(),
                    "Experiment raw code added");
            List<LoggedExperimentAsset> assets = experiment.getAssetList(SOURCE_CODE.type());
            validateAsset(assets, CODE_FILE_NAME, SOME_TEXT_FILE_SIZE, SOME_FULL_CONTEXT);

        } catch (Throwable t) {
            fail(t);
        }
    }

    @Test
    @Timeout(60)
    public void testCreateExperiment_wrongApiKey() {
        String wrongApiKey = "not existing API key";
        CometGeneralException ex = assertThrows(CometGeneralException.class, () ->
                OnlineExperimentImpl.builder().withApiKey(wrongApiKey).build());
        assertEquals(getString(FAILED_REGISTER_EXPERIMENT), ex.getMessage());
    }

    static final class OnCompleteAction implements Action, BooleanSupplier {
        boolean completed;

        @Override
        public void run() {
            this.completed = true;
        }

        @Override
        public boolean getAsBoolean() {
            return this.completed;
        }
    }

    static OnlineExperiment onlineExperiment(String experimentKey) {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withExistingExperimentKey(experimentKey)
                .build();
    }

    static void validateValues(List<Value> valueList, String name, Object value) {
        String stringValue = value.toString();
        System.out.println(name + ":" + value);
        assertTrue(valueList.stream()
                .peek(System.out::println)
                .filter(m -> name.equals(m.getName()))
                .filter(m -> stringValue.equals(m.getMax()))
                .filter(m -> stringValue.equals(m.getMin()))
                .anyMatch(m -> stringValue.equals(m.getCurrent())));
    }
}
