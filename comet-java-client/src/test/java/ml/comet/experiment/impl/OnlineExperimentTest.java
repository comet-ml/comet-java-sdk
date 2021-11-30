package ml.comet.experiment.impl;

import io.reactivex.rxjava3.functions.Action;
import ml.comet.experiment.ApiExperiment;
import ml.comet.experiment.Experiment;
import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.utils.TestUtils;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.GitMetadata;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.ValueMinMaxDto;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ml.comet.experiment.impl.asset.AssetType.ASSET_TYPE_ALL;
import static ml.comet.experiment.impl.asset.AssetType.ASSET_TYPE_SOURCE_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OnlineExperimentTest extends BaseApiTest {
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

    private static final String IMAGE_FILE_NAME = "someChart.png";
    private static final String CODE_FILE_NAME = "code_sample.py";
    private static final long IMAGE_FILE_SIZE = 31451L;
    private static final long CODE_FILE_SIZE = 19L;
    private static final String SOME_TEXT_FILE_NAME = "someTextFile.txt";
    private static final String ANOTHER_TEXT_FILE_NAME = "anotherTextFile.txt";
    private static final long SOME_TEXT_FILE_SIZE = 9L;
    private static final long ANOTHER_TEXT_FILE_SIZE = 12L;
    private static final String LOGGED_LINE = "This should end up in Comet ML.";
    private static final String LOGGED_ERROR_LINE = "This error should also get to Comet ML.";
    private static final String NON_LOGGED_LINE = "This should not end up in Comet ML.";

    @Test
    public void testExperimentCreatedAndShutDown() {
        OnlineExperiment experiment = createOnlineExperiment();
        String experimentKey = experiment.getExperimentKey();
        Optional<String> experimentLink = experiment.getExperimentLink();

        assertTrue(StringUtils.isNotBlank(experimentKey));
        assertTrue(experimentLink.isPresent());
        assertTrue(StringUtils.isNotBlank(experimentLink.get()));

        awaitForCondition(() -> experiment.getMetadata().isRunning(), "Experiment must become running");

        ExperimentMetadataRest metadata = experiment.getMetadata();
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

        ExperimentMetadataRest metadata = experiment.getMetadata();
        String generatedExperimentName = metadata.getExperimentName();
        assertTrue(StringUtils.isNoneEmpty(generatedExperimentName));

        experiment.setExperimentName(SOME_NAME);

        awaitForCondition(() -> SOME_NAME.equals(experiment.getMetadata().getExperimentName()),
                "Experiment name update timeout");

        ExperimentMetadataRest updatedMetadata = experiment.getMetadata();
        assertEquals(experiment.getExperimentKey(), metadata.getExperimentKey());
        assertEquals(SOME_NAME, updatedMetadata.getExperimentName());

        experiment.end();
    }

    @Test
    public void testLogAndGetMetric() {
        OnlineExperiment experiment = createOnlineExperiment();

        testLogParameters(experiment, Experiment::getMetrics, (key, value) -> {
            OnCompleteAction onCompleteAction = new OnCompleteAction();
            ((OnlineExperimentImpl) experiment).logMetric(key, value,
                    new ExperimentContext(1, 1), onCompleteAction);
            awaitForCondition(onCompleteAction, "logMetricAsync onComplete timeout");
        });

        experiment.end();
    }

    @Test
    public void testLogAndGetParameter() {
        OnlineExperiment experiment = createOnlineExperiment();

        testLogParameters(experiment, Experiment::getParameters, (key, value) -> {
            OnCompleteAction onCompleteAction = new OnCompleteAction();
            ((OnlineExperimentImpl) experiment).logParameter(key, value, new ExperimentContext(1), onCompleteAction);
            awaitForCondition(onCompleteAction, "logParameterAsync onComplete timeout");
        });

        experiment.end();
    }

    @Test
    public void testLogAndGetOther() {
        OnlineExperiment experiment = createOnlineExperiment();

        // Check that experiment has no extra other data yet
        //
        List<ValueMinMaxDto> parameters = experiment.getLogOther();
        assertEquals(1, parameters.size());
        assertTrue(parameters.stream().anyMatch(p -> "Name".equals(p.getName())));

        // Log some other data
        //
        Map<String, Object> params = new HashMap<>();
        params.put(SOME_PARAMETER, SOME_PARAMETER_VALUE);
        params.put(ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE);

        params.forEach((key, value) -> {
            OnCompleteAction onCompleteAction = new OnCompleteAction();
            ((OnlineExperimentImpl) experiment).logOther(key, value, onCompleteAction);
            awaitForCondition(onCompleteAction, "logOtherAsync onComplete timeout");
        });

        // Get saved other data and check
        //
        awaitForCondition(() -> experiment.getLogOther().size() == 3, "get other timeout");

        List<ValueMinMaxDto> updatedParameters = experiment.getLogOther();
        params.forEach((k, v) -> validateMetrics(updatedParameters, k, v));

        experiment.end();
    }

    @Test
    public void testLogAndGetHtml() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        assertFalse(experiment.getHtml().isPresent());

        // Create first HTML record
        //
        OnCompleteAction onComplete = new OnCompleteAction();
        experiment.logHtml(SOME_HTML, true, onComplete);

        // sleep to make sure the request was sent
        awaitForCondition(onComplete, "onComplete timeout");

        awaitForCondition(() -> {
            Optional<String> html = experiment.getHtml();
            return html.isPresent() && SOME_HTML.equals(html.get());
        }, "Experiment SOME_HTML update timeout", 60);

        // Override first HTML record
        //
        onComplete = new OnCompleteAction();
        experiment.logHtml(ANOTHER_HTML, true, onComplete);

        // sleep to make sure the request was sent
        awaitForCondition(onComplete, "onComplete timeout");

        awaitForCondition(() -> {
            Optional<String> html = experiment.getHtml();
            return html.isPresent() && ANOTHER_HTML.equals(html.get());
        }, "Experiment ANOTHER_HTML update timeout");

        // Check that HTML record was not overridden but appended
        //
        onComplete = new OnCompleteAction();
        experiment.logHtml(SOME_HTML, false, onComplete);

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
        experiment.addTag(SOME_TEXT, onComplete);
        awaitForCondition(onComplete, "onComplete timeout");

        onComplete = new OnCompleteAction();
        experiment.addTag(ANOTHER_TAG, onComplete);
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
        experiment.logGraph(SOME_GRAPH, onComplete);
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
        ExperimentMetadataRest metadata = experiment.getMetadata();
        Long startTimeMillis = metadata.getStartTimeMillis();
        Long endTimeMillis = metadata.getEndTimeMillis();
        String experimentKey = experiment.getExperimentKey();
        experiment.end();

        // fetch existing experiment and update time
        //
        OnlineExperimentImpl existingExperiment = (OnlineExperimentImpl) onlineExperiment(experimentKey);
        long now = System.currentTimeMillis();

        OnCompleteAction onComplete = new OnCompleteAction();
        existingExperiment.logStartTime(now, onComplete);
        awaitForCondition(onComplete, "logStartTime onComplete timeout", 120);

        onComplete = new OnCompleteAction();
        existingExperiment.logEndTime(now, onComplete);
        awaitForCondition(onComplete, "logEndTime onComplete timeout", 120);

        // Get updated experiment metadata and check results
        //
        awaitForCondition(() -> {
            ExperimentMetadataRest data = existingExperiment.getMetadata();
            return data.getStartTimeMillis() == now && data.getEndTimeMillis() == now;
        }, "Experiment get start/stop time timeout", 240);

        ExperimentMetadataRest updatedMetadata = existingExperiment.getMetadata();
        assertNotEquals(startTimeMillis, updatedMetadata.getStartTimeMillis());
        assertNotEquals(endTimeMillis, updatedMetadata.getEndTimeMillis());
    }

    @Test
    public void testUploadAndGetAssets() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // Make sure experiment has no assets
        //
        assertTrue(experiment.getAssetList(ASSET_TYPE_ALL).isEmpty());

        // Upload few assets and wait for completion
        //
        ExperimentContext context = new ExperimentContext(10, 101, "train");
        OnCompleteAction onComplete = new OnCompleteAction();
        experiment.uploadAsset(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)), IMAGE_FILE_NAME,
                false, context, onComplete);
        awaitForCondition(onComplete, "image file onComplete timeout", 30);

        onComplete = new OnCompleteAction();
        experiment.uploadAsset(Objects.requireNonNull(TestUtils.getFile(SOME_TEXT_FILE_NAME)), SOME_TEXT_FILE_NAME,
                false, context, onComplete);
        awaitForCondition(onComplete, "text file onComplete timeout", 30);

        // wait for assets become available and validate results
        //
        awaitForCondition(() -> experiment.getAssetList(ASSET_TYPE_ALL).size() == 2, "Assets was uploaded");

        List<ExperimentAssetLink> assets = experiment.getAssetList(ASSET_TYPE_ALL);
        validateAsset(assets, IMAGE_FILE_NAME, IMAGE_FILE_SIZE, context);
        validateAsset(assets, SOME_TEXT_FILE_NAME, SOME_TEXT_FILE_SIZE, context);

        // update one of the assets and validate
        //
        onComplete = new OnCompleteAction();
        experiment.uploadAsset(Objects.requireNonNull(TestUtils.getFile(ANOTHER_TEXT_FILE_NAME)),
                SOME_TEXT_FILE_NAME, true, context, onComplete);
        awaitForCondition(onComplete, "update text file onComplete timeout", 30);

        awaitForCondition(() -> {
            List<ExperimentAssetLink> assetList = experiment.getAssetList(ASSET_TYPE_ALL);
            return assetList.stream()
                    .filter(asset -> SOME_TEXT_FILE_NAME.equals(asset.getFileName()))
                    .anyMatch(asset ->
                            ANOTHER_TEXT_FILE_SIZE == asset.getFileSize()
                                    && asset.getStep() == context.getStep()
                                    && asset.getRunContext().equals(context.getContext()));
        }, "Asset was updated");

        experiment.end();
    }

    @Test
    public void testSetsContext() {
        OnlineExperiment experiment = createOnlineExperiment();

        assertTrue(experiment.getAssetList(ASSET_TYPE_ALL).isEmpty());

        experiment.setContext(SOME_TEXT);
        experiment.uploadAsset(TestUtils.getFile(SOME_TEXT_FILE_NAME), false);

        awaitForCondition(() -> experiment.getAssetList(ASSET_TYPE_ALL).size() == 1, "Asset uploaded");

        Optional<ExperimentAssetLink> assetOpt = experiment.getAssetList(ASSET_TYPE_ALL)
                .stream()
                .filter(asset -> SOME_TEXT_FILE_NAME.equals(asset.getFileName()))
                .findFirst();
        assertTrue(assetOpt.isPresent());
        assertEquals(SOME_TEXT, assetOpt.get().getRunContext());
    }

    @Test
    public void testLogAndGetGitMetadata() {
        OnlineExperiment experiment = createOnlineExperiment();

        // Get GIT metadata and check that it is not set
        //
        GitMetadataRest gitMetadata = experiment.getGitMetadata();
        assertNull(gitMetadata.getUser());
        assertNull(gitMetadata.getBranch());
        assertNull(gitMetadata.getOrigin());

        // Create and update GIT metadata and wait for response
        //
        OnCompleteAction onComplete = new OnCompleteAction();
        GitMetadata request = new GitMetadata(experiment.getExperimentKey(),
                "user", "root", "branch", "parent", "origin");
        ((OnlineExperimentImpl) experiment).logGitMetadataAsync(request, onComplete);
        awaitForCondition(onComplete, "onComplete timeout");

        // Get GIT metadata and check results
        //
        awaitForCondition(() -> request.getUser().equals(experiment.getGitMetadata().getUser()),
                "Git metadata user timeout");

        GitMetadataRest updatedMetadata = experiment.getGitMetadata();
        assertEquals(updatedMetadata.getOrigin(), request.getOrigin());
        assertEquals(updatedMetadata.getBranch(), request.getBranch());
        assertEquals(updatedMetadata.getRoot(), request.getRoot());
        assertEquals(updatedMetadata.getParent(), request.getParent());

        experiment.end();
    }


    @Test
    public void testCopyStdout() throws IOException {
        OnlineExperiment experiment = createOnlineExperiment();
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

    }

    @Test
    public void testLogAndGetFileCode() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // check that no code was logged
        //
        assertTrue(experiment.getAssetList(ASSET_TYPE_ALL).isEmpty());

        // log code and check results
        //
        ExperimentContext context = new ExperimentContext(10, 101, "test");
        experiment.logCode(Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)), context);

        awaitForCondition(() -> !experiment.getAssetList(ASSET_TYPE_SOURCE_CODE).isEmpty(),
                "Experiment code from file added");
        List<ExperimentAssetLink> assets = experiment.getAssetList(ASSET_TYPE_SOURCE_CODE);
        validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE, context);

        experiment.end();
    }

    @Test
    public void testLogAndGetRawCode() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // check that no code was logged
        //
        assertTrue(experiment.getAssetList(ASSET_TYPE_ALL).isEmpty());

        // log code and check results
        //
        ExperimentContext context = new ExperimentContext(10, 101, "test");
        experiment.logCode(SOME_TEXT, CODE_FILE_NAME, context);

        awaitForCondition(() -> !experiment.getAssetList(ASSET_TYPE_SOURCE_CODE).isEmpty(),
                "Experiment raw code added");
        List<ExperimentAssetLink> assets = experiment.getAssetList(ASSET_TYPE_SOURCE_CODE);
        validateAsset(assets, CODE_FILE_NAME, SOME_TEXT_FILE_SIZE, context);

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

    static void validateAsset(List<ExperimentAssetLink> assets, String expectedAssetName,
                              long expectedSize, ExperimentContext context) {
        assertTrue(assets.stream()
                .filter(asset -> expectedAssetName.equals(asset.getFileName()))
                .anyMatch(asset -> expectedSize == asset.getFileSize()
                        && context.getStep() == asset.getStep()
                        && context.getContext().equals(asset.getRunContext())));
    }

    static void testLogParameters(OnlineExperiment experiment,
                                  Function<Experiment, List<ValueMinMaxDto>> supplierFunction,
                                  BiConsumer<String, Object> updateFunction) {

        List<ValueMinMaxDto> parameters = supplierFunction.apply(experiment);
        assertTrue(parameters.isEmpty(), "no experiment parameters expected");

        Map<String, Object> params = new HashMap<>();
        params.put(SOME_PARAMETER, SOME_PARAMETER_VALUE);
        params.put(ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE);

        params.forEach(updateFunction);

        awaitForCondition(() -> supplierFunction.apply(
                experiment).size() == 2, "experiment parameters get timeout");

        List<ValueMinMaxDto> updatedParameters = supplierFunction.apply(experiment);
        params.forEach((k, v) -> validateMetrics(updatedParameters, k, v));

    }

    static void validateMetrics(List<ValueMinMaxDto> metrics, String name, Object value) {
        String stringValue = value.toString();
        assertTrue(metrics.stream()
                .filter(m -> name.equals(m.getName()))
                .filter(m -> stringValue.equals(m.getValueMax()))
                .filter(m -> stringValue.equals(m.getValueMin()))
                .anyMatch(m -> stringValue.equals(m.getValueCurrent())));
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
