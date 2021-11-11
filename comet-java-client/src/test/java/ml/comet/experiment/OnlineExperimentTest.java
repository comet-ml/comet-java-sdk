package ml.comet.experiment;

import ml.comet.experiment.model.CreateGitMetadata;
import ml.comet.experiment.model.ExperimentAssetLink;
import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.GitMetadataRest;
import ml.comet.experiment.model.ValueMinMaxDto;
import ml.comet.experiment.utils.TestUtils;
import org.apache.commons.lang3.StringUtils;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static ml.comet.experiment.constants.AssetType.ASSET_TYPE_ALL;
import static ml.comet.experiment.constants.AssetType.ASSET_TYPE_SOURCE_CODE;
import static ml.comet.experiment.constants.AssetType.ASSET_TYPE_UNKNOWN;
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
    public static final String MESSAGE_NAME_UPDATED = "Experiment name updated";
    public static final String MESSAGE_HTML_UPDATED = "Experiment html updated";
    public static final String MESSAGE_RUNNING_STATUS_UPDATED = "Experiment running status updated";

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
        ApiExperiment apiExperiment = ApiExperiment.builder(experimentKey).build();
        awaitForCondition(() -> !apiExperiment.getMetadata().isRunning(),
                MESSAGE_RUNNING_STATUS_UPDATED, 60);
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

        OnlineExperiment updatedExperiment = fetchExperiment(experimentKey);
        updatedExperiment.setExperimentName(SOME_NAME);

        awaitForCondition(
                () -> SOME_NAME.equals(updatedExperiment.getMetadata().getExperimentName()), MESSAGE_NAME_UPDATED);
        updatedExperiment.end();
    }

    @Test
    public void testSetAndGetExperimentName() {
        OnlineExperiment experiment = createOnlineExperiment();

        ExperimentMetadataRest metadata = experiment.getMetadata();
        String generatedExperimentName = metadata.getExperimentName();
        assertTrue(StringUtils.isNoneEmpty(generatedExperimentName));

        experiment.setExperimentName(SOME_NAME);

        awaitForCondition(() -> SOME_NAME.equals(experiment.getMetadata().getExperimentName()), MESSAGE_NAME_UPDATED);

        ExperimentMetadataRest updatedMetadata = experiment.getMetadata();
        assertEquals(experiment.getExperimentKey(), metadata.getExperimentKey());
        assertEquals(SOME_NAME, updatedMetadata.getExperimentName());

        experiment.end();
    }

    @Test
    public void testLogAndGetMetric() {
        OnlineExperiment experiment = createOnlineExperiment();

        testLogParameters(experiment, Experiment::getMetrics, experiment::logMetric);

        experiment.end();
    }

    @Test
    public void testLogAndGetParameter() {
        OnlineExperiment experiment = createOnlineExperiment();

        testLogParameters(experiment, Experiment::getParameters, experiment::logParameter);

        experiment.end();
    }

    @Test
    public void testLogAndGetOther() {
        OnlineExperiment experiment = createOnlineExperiment();

        List<ValueMinMaxDto> parameters = experiment.getLogOther();
        assertEquals(1, parameters.size());
        assertTrue(parameters.stream().anyMatch(p -> "Name".equals(p.getName())));

        Map<String, Object> params = new HashMap<>();
        params.put(SOME_PARAMETER, SOME_PARAMETER_VALUE);
        params.put(ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE);

        params.forEach(experiment::logOther);

        awaitForCondition(() -> experiment.getLogOther().size() == 3, "Experiment parameters added");

        List<ValueMinMaxDto> updatedParameters = experiment.getLogOther();
        params.forEach((k, v) -> validateMetrics(updatedParameters, k, v));

        experiment.end();
    }

    @Test
    public void testLogAndGetHtml() {
        OnlineExperiment experiment = createOnlineExperiment();

        assertFalse(experiment.getHtml().isPresent());

        experiment.logHtml(SOME_HTML, true);

        awaitForCondition(() -> {
            Optional<String> html = experiment.getHtml();
            return html.isPresent() && SOME_HTML.equals(html.get());
        }, MESSAGE_HTML_UPDATED);

        experiment.logHtml(ANOTHER_HTML, true);

        awaitForCondition(() -> {
            Optional<String> html = experiment.getHtml();
            return html.isPresent() && ANOTHER_HTML.equals(html.get());
        }, MESSAGE_HTML_UPDATED);

        experiment.logHtml(SOME_HTML, false);


        awaitForCondition(() -> {
            Optional<String> html = experiment.getHtml();
            return html.isPresent() && JOINED_HTML.equals(html.get());
        }, MESSAGE_HTML_UPDATED);

        experiment.end();
    }

    @Test
    public void testAddAndGetTag() {
        OnlineExperiment experiment = createOnlineExperiment();

        assertTrue(experiment.getTags().isEmpty());

        experiment.addTag(SOME_TEXT);
        experiment.addTag(ANOTHER_TAG);

        awaitForCondition(() -> experiment.getTags().size() == 2, "Experiment tags updated");

        List<String> tags = experiment.getTags();
        assertTrue(tags.contains(SOME_TEXT));
        assertTrue(tags.contains(ANOTHER_TAG));

        experiment.end();
    }

    @Test
    public void testLogAndGetGraph() {
        OnlineExperiment experiment = createOnlineExperiment();

        Optional<String> graph = experiment.getGraph();
        assertTrue(!graph.isPresent() || graph.get().isEmpty());

        experiment.logGraph(SOME_GRAPH);

        awaitForCondition(() -> {
            Optional<String> graphOpt = experiment.getGraph();
            return graphOpt.isPresent() && SOME_GRAPH.equals(graphOpt.get());
        }, "Experiment graph updated");

        experiment.end();
    }

    @Test
    public void testLogAndGetExperimentTime() {
        // create online experiment
        OnlineExperiment experiment = createOnlineExperiment();

        ExperimentMetadataRest metadata = experiment.getMetadata();
        Long startTimeMillis = metadata.getStartTimeMillis();
        Long endTimeMillis = metadata.getEndTimeMillis();
        String experimentKey = experiment.getExperimentKey();

        experiment.end();

        // fetch existing experiment and update time
        OnlineExperiment existingExperiment = fetchExperiment(experimentKey);

        long now = System.currentTimeMillis();
        existingExperiment.logStartTime(now);
        existingExperiment.logEndTime(now);

        awaitForCondition(() -> {
            ExperimentMetadataRest data = existingExperiment.getMetadata();
            return data.getStartTimeMillis() == now && data.getEndTimeMillis() == now;
        }, "Experiment start/stop time updated", 180);

        ExperimentMetadataRest updatedMetadata = existingExperiment.getMetadata();
        assertNotEquals(startTimeMillis, updatedMetadata.getStartTimeMillis());
        assertNotEquals(endTimeMillis, updatedMetadata.getEndTimeMillis());
    }

    @Test
    public void testUploadAndGetAssets() {
        OnlineExperiment experiment = createOnlineExperiment();

        assertTrue(experiment.getAssetList(ASSET_TYPE_ALL).isEmpty());

        experiment.uploadAsset(TestUtils.getFile(IMAGE_FILE_NAME), false);
        experiment.uploadAsset(TestUtils.getFile(SOME_TEXT_FILE_NAME), false);

        awaitForCondition(() -> experiment.getAssetList(ASSET_TYPE_ALL).size() == 2, "Assets uploaded");

        List<ExperimentAssetLink> assets = experiment.getAssetList(ASSET_TYPE_ALL);
        validateAsset(assets, IMAGE_FILE_NAME, IMAGE_FILE_SIZE);
        validateAsset(assets, SOME_TEXT_FILE_NAME, SOME_TEXT_FILE_SIZE);

        experiment.uploadAsset(TestUtils.getFile(ANOTHER_TEXT_FILE_NAME), SOME_TEXT_FILE_NAME, true);

        awaitForCondition(() -> {
            List<ExperimentAssetLink> textFiles = experiment.getAssetList(ASSET_TYPE_UNKNOWN);
            ExperimentAssetLink file = textFiles.get(0);
            return ANOTHER_TEXT_FILE_SIZE == file.getFileSize();
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

        GitMetadataRest gitMetadata = experiment.getGitMetadata();
        assertNull(gitMetadata.getUser());
        assertNull(gitMetadata.getBranch());
        assertNull(gitMetadata.getOrigin());

        CreateGitMetadata request = new CreateGitMetadata(experiment.getExperimentKey(),
                "user", "root", "branch", "parent", "origin");
        experiment.logGitMetadata(request);

        awaitForCondition(() -> request.getUser().equals(experiment.getGitMetadata().getUser()), "Git metadata user updated");

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
        OnlineExperiment experiment = createOnlineExperiment();
        assertTrue(experiment.getAssetList(ASSET_TYPE_ALL).isEmpty());
        experiment.logCode(TestUtils.getFile(CODE_FILE_NAME));
        awaitForCondition(() -> !experiment.getAssetList(ASSET_TYPE_SOURCE_CODE).isEmpty(), "Experiment code from file added");
        List<ExperimentAssetLink> assets = experiment.getAssetList(ASSET_TYPE_SOURCE_CODE);
        validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE);
        experiment.end();
    }

    @Test
    public void testLogAndGetRawCode() {
        OnlineExperiment experiment = createOnlineExperiment();
        assertTrue(experiment.getAssetList(ASSET_TYPE_ALL).isEmpty());
        experiment.logCode(SOME_TEXT, CODE_FILE_NAME);
        awaitForCondition(() -> !experiment.getAssetList(ASSET_TYPE_SOURCE_CODE).isEmpty(), "Experiment raw code added");
        List<ExperimentAssetLink> assets = experiment.getAssetList(ASSET_TYPE_SOURCE_CODE);
        validateAsset(assets, CODE_FILE_NAME, SOME_TEXT_FILE_SIZE);
        experiment.end();
    }

    private OnlineExperiment fetchExperiment(String experimentKey) {
        return OnlineExperimentImpl.builder()
                .withApiKey(API_KEY)
                .withExistingExperimentKey(experimentKey)
                .build();
    }

    private void validateAsset(List<ExperimentAssetLink> assets, String expectedAssetName, long expectedSize) {
        assertTrue(assets.stream()
                .filter(asset -> expectedAssetName.equals(asset.getFileName()))
                .anyMatch(asset -> expectedSize == asset.getFileSize()));
    }

    private void testLogParameters(OnlineExperiment experiment,
                                   Function<Experiment, List<ValueMinMaxDto>> supplierFunction,
                                   BiConsumer<String, Object> updateFunction) {

        List<ValueMinMaxDto> parameters = supplierFunction.apply(experiment);
        assertTrue(parameters.isEmpty());

        Map<String, Object> params = new HashMap<>();
        params.put(SOME_PARAMETER, SOME_PARAMETER_VALUE);
        params.put(ANOTHER_PARAMETER, ANOTHER_PARAMETER_VALUE);

        params.forEach(updateFunction);

        awaitForCondition(() -> supplierFunction.apply(experiment).size() == 2, "Experiment parameters added");

        List<ValueMinMaxDto> updatedParameters = supplierFunction.apply(experiment);
        params.forEach((k, v) -> validateMetrics(updatedParameters, k, v));

    }

    private void validateMetrics(List<ValueMinMaxDto> metrics, String name, Object value) {
        String stringValue = value.toString();
        assertTrue(metrics.stream()
                .filter(m -> name.equals(m.getName()))
                .filter(m -> stringValue.equals(m.getValueMax()))
                .filter(m -> stringValue.equals(m.getValueMin()))
                .anyMatch(m -> stringValue.equals(m.getValueCurrent())));
    }


    private void awaitForCondition(BooleanSupplier booleanSupplier, String conditionAlias) {
        Awaitility.await(conditionAlias).atMost(30, SECONDS)
                .pollInterval(300L, MILLISECONDS)
                .until(booleanSupplier::getAsBoolean);
    }

    private void awaitForCondition(BooleanSupplier booleanSupplier, String conditionAlias, long timeoutSeconds) {
        Awaitility.await(conditionAlias).atMost(timeoutSeconds, SECONDS)
                .pollInterval(300L, MILLISECONDS)
                .until(booleanSupplier::getAsBoolean);
    }
}
