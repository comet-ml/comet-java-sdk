package ml.comet.experiment.impl;

import io.reactivex.rxjava3.functions.Action;
import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.asset.LoggedExperimentAssetImpl;
import ml.comet.experiment.impl.utils.TestUtils;
import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.GitMetaData;
import ml.comet.experiment.model.Value;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ml.comet.experiment.impl.ExperimentTestFactory.API_KEY;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.impl.asset.AssetType.SOURCE_CODE;
import static ml.comet.experiment.impl.utils.CometUtils.fullMetricName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private static final ExperimentContext SOME_FULL_CONTEXT =
            new ExperimentContext(10, 101, "train");
    private static final ExperimentContext SOME_PARTIAL_CONTEXT =
            new ExperimentContext(10, 101);

    private static final Map<String, Object> SOME_METADATA = new HashMap<String, Object>() {{
        put("someInt", 10);
        put("someString", "test string");
        put("someBoolean", true);
    }};

    @Test
    public void testLogAndGetAssetsFolder() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // Make sure experiment has no assets
        //
        assertTrue(experiment.getAllAssetList().isEmpty());

        // Log assets folder and wait for completion
        //
        OnCompleteAction onComplete = new OnCompleteAction();
        experiment.logAssetFolder(
                assetsFolder.toFile(), false, true, false,
                SOME_FULL_CONTEXT, Optional.of(onComplete));

        awaitForCondition(onComplete, "log assets' folder timeout", 60);

        // wait for assets become available and validate results
        //
        awaitForCondition(() ->
                experiment.getAllAssetList().size() == assetFolderFiles.size(), "Assets was uploaded");

        List<LoggedExperimentAsset> assets = experiment.getAllAssetList();

        validateAsset(assets, SOME_TEXT_FILE_NAME, SOME_TEXT_FILE_SIZE, SOME_FULL_CONTEXT);
        validateAsset(assets, ANOTHER_TEXT_FILE_NAME, ANOTHER_TEXT_FILE_SIZE, SOME_FULL_CONTEXT);
        validateAsset(assets, emptyAssetFile.getFileName().toString(), 0, SOME_FULL_CONTEXT);
        validateAsset(assets, IMAGE_FILE_NAME, IMAGE_FILE_SIZE, SOME_FULL_CONTEXT);
        validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE, SOME_FULL_CONTEXT);

        experiment.end();
    }

    @Test
    public void testExperimentCreatedAndShutDown() {
        OnlineExperiment experiment = createOnlineExperiment();
        String experimentKey = experiment.getExperimentKey();
        Optional<String> experimentLink = experiment.getExperimentLink();

        assertTrue(StringUtils.isNotBlank(experimentKey));
        assertTrue(experimentLink.isPresent());
        assertTrue(StringUtils.isNotBlank(experimentLink.get()));

        awaitForCondition(() -> experiment.getMetadata().isRunning(), "Experiment must become running");

        ExperimentMetadata metadata = experiment.getMetadata();
        assertEquals(experiment.getExperimentKey(), metadata.getExperimentKey());
        assertEquals(experiment.getWorkspaceName(), metadata.getWorkspaceName());
        assertEquals(experiment.getProjectName(), metadata.getProjectName());

        experiment.end();

        // use REST API to check experiment status
        ApiExperiment apiExperiment = ApiExperimentImpl.builder(experimentKey).build();
        awaitForCondition(() -> !apiExperiment.getMetadata().isRunning(),
                "Experiment running status updated", 60);
        assertFalse(apiExperiment.getMetadata().isRunning(), "Experiment must have status not running");

        apiExperiment.end();
    }

    @Test
    public void testInitAndUpdateExistingExperiment() {
        // create dummy experiment and make sure it has no name
        OnlineExperiment experiment = createOnlineExperiment();
        experiment.end();
        assertNull(experiment.getExperimentName());

        // get previous experiment by key and check that update is working
        String experimentKey = experiment.getExperimentKey();

        OnlineExperiment updatedExperiment = onlineExperiment(experimentKey);
        updatedExperiment.setExperimentName(SOME_NAME);

        awaitForCondition(
                () -> SOME_NAME.equals(updatedExperiment.getMetadata().getExperimentName()),
                "Experiment name updated timeout");
        updatedExperiment.end();
    }

    @Test
    public void testSetAndGetExperimentName() {
        OnlineExperiment experiment = createOnlineExperiment();

        ExperimentMetadata metadata = experiment.getMetadata();
        String generatedExperimentName = metadata.getExperimentName();
        assertTrue(StringUtils.isNoneEmpty(generatedExperimentName));

        experiment.setExperimentName(SOME_NAME);

        awaitForCondition(() -> SOME_NAME.equals(experiment.getMetadata().getExperimentName()),
                "Experiment name update timeout");

        ExperimentMetadata updatedMetadata = experiment.getMetadata();
        assertEquals(experiment.getExperimentKey(), metadata.getExperimentKey());
        assertEquals(SOME_NAME, updatedMetadata.getExperimentName());

        experiment.end();
    }

    @Test
    public void testLogAndGetMetric() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            // Test metric with full context (including context ID)
            //
            OnCompleteAction onComplete = new OnCompleteAction();
            experiment.logMetric(
                    SOME_PARAMETER, SOME_PARAMETER_VALUE, SOME_FULL_CONTEXT, Optional.of(onComplete));
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
            validateValues(metrics, fullMetricName(SOME_PARAMETER, SOME_FULL_CONTEXT), SOME_PARAMETER_VALUE);
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
                    SOME_PARAMETER, SOME_PARAMETER_VALUE, SOME_FULL_CONTEXT, Optional.of(onComplete));
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
    public void testLogAndGetOther() {
        OnlineExperiment experiment = createOnlineExperiment();

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

        experiment.end();
    }

    @Test
    public void testLogAndGetHtml() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

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

        experiment.end();
    }

    @Test
    public void testAddAndGetTag() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

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

        experiment.end();
    }

    @Test
    public void testLogAndGetGraph() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

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

        experiment.end();
    }

    @Test
    public void testLogAndGetExperimentTime() {
        OnlineExperiment experiment = createOnlineExperiment();

        // Get experiment metadata
        //
        ExperimentMetadata metadata = experiment.getMetadata();
        Instant startTimeMillis = metadata.getStartTime();
        Instant endTimeMillis = metadata.getEndTime();
        String experimentKey = experiment.getExperimentKey();
        experiment.end();

        // fetch existing experiment and update time
        //
        OnlineExperimentImpl existingExperiment = (OnlineExperimentImpl) onlineExperiment(experimentKey);
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
    }

    @Test
    public void testUploadAndGetAssets() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // Make sure experiment has no assets
        //
        assertTrue(experiment.getAllAssetList().isEmpty());

        // Upload few assets and wait for completion
        //
        OnCompleteAction onComplete = new OnCompleteAction();
        experiment.logAssetFileAsync(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)), IMAGE_FILE_NAME,
                false, SOME_FULL_CONTEXT, Optional.of(onComplete));
        awaitForCondition(onComplete, "image file onComplete timeout", 30);

        onComplete = new OnCompleteAction();
        experiment.logAssetFileAsync(Objects.requireNonNull(TestUtils.getFile(SOME_TEXT_FILE_NAME)), SOME_TEXT_FILE_NAME,
                false, SOME_FULL_CONTEXT, Optional.of(onComplete));
        awaitForCondition(onComplete, "text file onComplete timeout", 30);

        // wait for assets become available and validate results
        //
        awaitForCondition(() -> experiment.getAllAssetList().size() == 2, "Assets was uploaded");

        List<LoggedExperimentAsset> assets = experiment.getAllAssetList();
        validateAsset(assets, IMAGE_FILE_NAME, IMAGE_FILE_SIZE, SOME_FULL_CONTEXT);
        validateAsset(assets, SOME_TEXT_FILE_NAME, SOME_TEXT_FILE_SIZE, SOME_FULL_CONTEXT);

        // update one of the assets and validate
        //
        onComplete = new OnCompleteAction();
        experiment.logAssetFileAsync(Objects.requireNonNull(TestUtils.getFile(ANOTHER_TEXT_FILE_NAME)),
                SOME_TEXT_FILE_NAME, true, SOME_FULL_CONTEXT, Optional.of(onComplete));
        awaitForCondition(onComplete, "update text file onComplete timeout", 30);

        awaitForCondition(() -> {
            List<LoggedExperimentAsset> assetList = experiment.getAllAssetList();
            return assetList.stream()
                    .filter(asset -> SOME_TEXT_FILE_NAME.equals(asset.getLogicalPath()))
                    .anyMatch(asset -> {
                        ExperimentContext context = ((LoggedExperimentAssetImpl) asset).getContext();
                        return ANOTHER_TEXT_FILE_SIZE == asset.getSize().orElse(0L)
                                && Objects.equals(context.getStep(), SOME_FULL_CONTEXT.getStep())
                                && context.getContext().equals(SOME_FULL_CONTEXT.getContext());
                    });

        }, "Asset was updated");

        experiment.end();
    }

    @Test
    public void testLogAndGetRemoteAssets() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            // Make sure experiment has no assets
            //
            assertTrue(experiment.getAllAssetList().isEmpty());

            // Log remote assets and wait for completion
            //
            OnCompleteAction onComplete = new OnCompleteAction();

            URI firstAssetLink = new URI("s3://bucket/folder/firstAssetFile.extension");
            String firstAssetFileName = "firstAssetFileName";
            experiment.logRemoteAsset(
                    firstAssetLink, Optional.of(firstAssetFileName), false, Optional.of(SOME_METADATA),
                    SOME_FULL_CONTEXT, Optional.of(onComplete));

            awaitForCondition(onComplete, "first remote asset onComplete timeout", 30);

            String secondAssetExpectedFileName = "secondAssetFile.extension";
            URI secondAssetLink = new URI("s3://bucket/folder/" + secondAssetExpectedFileName);
            experiment.logRemoteAsset(secondAssetLink, empty(), false, empty(),
                    SOME_FULL_CONTEXT, Optional.of(onComplete));

            awaitForCondition(onComplete, "second remote asset onComplete timeout", 30);

            // wait for assets become available and validate results
            //
            awaitForCondition(() -> experiment.getAllAssetList().size() == 2, "Assets was uploaded");
            List<LoggedExperimentAsset> assets = experiment.getAllAssetList();

            validateRemoteAssetLink(assets, firstAssetLink, firstAssetFileName, SOME_METADATA);
            validateRemoteAssetLink(assets, secondAssetLink, secondAssetExpectedFileName, null);
        } catch (Exception e) {
            fail(e);
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
    public void testLogAndGetGitMetadata() {
        OnlineExperiment experiment = createOnlineExperiment();

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

        experiment.end();
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
                    .filter(log -> log.contains(LOGGED_LINE))
                    .filter(log -> log.contains(LOGGED_ERROR_LINE))
                    .filter(log -> !log.contains(NON_LOGGED_LINE))
                    .isPresent(), "Experiment logs added");
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testLogAndGetFileCode() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // check that no code was logged
        //
        assertTrue(experiment.getAllAssetList().isEmpty());

        // log code and check results
        //
        experiment.logCode(Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)), SOME_FULL_CONTEXT);

        awaitForCondition(() -> !experiment.getAssetList(SOURCE_CODE.type()).isEmpty(),
                "Experiment code from file added");
        List<LoggedExperimentAsset> assets = experiment.getAssetList(SOURCE_CODE.type());
        validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE, SOME_FULL_CONTEXT);

        experiment.end();
    }

    @Test
    public void testLogAndGetRawCode() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // check that no code was logged
        //
        assertTrue(experiment.getAllAssetList().isEmpty());

        // log code and check results
        //
        experiment.logCode(SOME_TEXT, CODE_FILE_NAME, SOME_PARTIAL_CONTEXT);

        awaitForCondition(() -> !experiment.getAssetList(SOURCE_CODE.type()).isEmpty(),
                "Experiment raw code added");
        List<LoggedExperimentAsset> assets = experiment.getAssetList(SOURCE_CODE.type());
        validateAsset(assets, CODE_FILE_NAME, SOME_TEXT_FILE_SIZE, SOME_PARTIAL_CONTEXT);

        experiment.end();
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

    static void validateRemoteAssetLink(List<LoggedExperimentAsset> assets, URI uri,
                                        String fileName, Map<String, Object> metadata) {
        if (Objects.nonNull(metadata)) {
            assertTrue(assets.stream()
                    .filter(asset -> Objects.equals(uri, asset.getLink().orElse(null)))
                    .allMatch(asset -> asset.isRemote()
                            && Objects.equals(asset.getLogicalPath(), fileName)
                            && Objects.equals(asset.getMetadata(), metadata)));
        } else {
            assertTrue(assets.stream()
                    .filter(asset -> Objects.equals(uri, asset.getLink().orElse(null)))
                    .allMatch(asset -> asset.isRemote()
                            && Objects.equals(asset.getLogicalPath(), fileName)
                            && asset.getMetadata().isEmpty()));
        }
    }

    static void validateAsset(List<LoggedExperimentAsset> assets, String expectedAssetName,
                              long expectedSize, ExperimentContext context) {
        assertTrue(assets.stream()
                .filter(asset -> expectedAssetName.equals(asset.getLogicalPath()))
                .anyMatch(asset -> {
                    ExperimentContext assetContext = ((LoggedExperimentAssetImpl) asset).getContext();
                    boolean res = expectedSize == asset.getSize().orElse(0L)
                            && Objects.equals(context.getStep(), assetContext.getStep());
                    if (StringUtils.isNotBlank(context.getContext())) {
                        res = res & context.getContext().equals(assetContext.getContext());
                    }
                    return res;
                }));
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

    static void awaitForCondition(BooleanSupplier booleanSupplier, String conditionAlias) {
        Awaitility.await(conditionAlias).atMost(30, SECONDS)
                .pollDelay(1, SECONDS)
                .pollInterval(300L, MILLISECONDS)
                .until(booleanSupplier::getAsBoolean);
    }

    static void awaitForCondition(BooleanSupplier booleanSupplier, String conditionAlias, long timeoutSeconds) {
        Awaitility.await(conditionAlias).atMost(timeoutSeconds, SECONDS)
                .pollDelay(1, SECONDS)
                .pollInterval(300L, MILLISECONDS)
                .until(booleanSupplier::getAsBoolean);
    }
}
