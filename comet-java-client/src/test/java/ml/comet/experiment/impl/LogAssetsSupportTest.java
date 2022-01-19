package ml.comet.experiment.impl;

import ml.comet.experiment.OnlineExperiment;
import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.asset.LoggedExperimentAssetImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.empty;
import static ml.comet.experiment.impl.ExperimentTestFactory.createOnlineExperiment;
import static ml.comet.experiment.impl.TestUtils.awaitForCondition;
import static ml.comet.experiment.impl.TestUtils.validateAsset;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The integration tests to test {@link OnlineExperiment} implementation related to log assets.
 */
@DisplayName("LogAssetsSupportTest INTEGRATION")
@Tag("integration")
public class LogAssetsSupportTest extends AssetsBaseTest {
    @Test
    public void testLogAndGetRemoteAssets() {
        try (OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment()) {
            // Make sure experiment has no assets
            //
            assertTrue(experiment.getAllAssetList().isEmpty());

            // Log remote assets and wait for completion
            //
            OnlineExperimentTest.OnCompleteAction onComplete = new OnlineExperimentTest.OnCompleteAction();

            URI firstAssetLink = new URI("s3://bucket/folder/firstAssetFile.extension");
            String firstAssetFileName = "firstAssetFileName";
            experiment.logRemoteAsset(
                    firstAssetLink, Optional.of(firstAssetFileName), false, Optional.of(TestUtils.SOME_METADATA),
                    TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));

            awaitForCondition(onComplete, "first remote asset onComplete timeout", 30);

            String secondAssetExpectedFileName = "secondAssetFile.extension";
            URI secondAssetLink = new URI("s3://bucket/folder/" + secondAssetExpectedFileName);
            experiment.logRemoteAsset(secondAssetLink, empty(), false, empty(),
                    TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));

            awaitForCondition(onComplete, "second remote asset onComplete timeout", 30);

            // wait for assets become available and validate results
            //
            awaitForCondition(() -> experiment.getAllAssetList().size() == 2, "Assets was uploaded");
            List<LoggedExperimentAsset> assets = experiment.getAllAssetList();

            validateRemoteAssetLink(assets, firstAssetLink, firstAssetFileName, TestUtils.SOME_METADATA);
            validateRemoteAssetLink(assets, secondAssetLink, secondAssetExpectedFileName, null);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void testLogAndGetAssets() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // Make sure experiment has no assets
        //
        assertTrue(experiment.getAllAssetList().isEmpty());

        // Upload few assets and wait for completion
        //
        OnlineExperimentTest.OnCompleteAction onComplete = new OnlineExperimentTest.OnCompleteAction();
        experiment.logAssetFileAsync(Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)), IMAGE_FILE_NAME,
                false, TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));
        awaitForCondition(onComplete, "image file onComplete timeout", 30);

        onComplete = new OnlineExperimentTest.OnCompleteAction();
        experiment.logAssetFileAsync(Objects.requireNonNull(TestUtils.getFile(SOME_TEXT_FILE_NAME)), SOME_TEXT_FILE_NAME,
                false, TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));
        awaitForCondition(onComplete, "text file onComplete timeout", 30);

        // wait for assets become available and validate results
        //
        awaitForCondition(() -> experiment.getAllAssetList().size() == 2, "Assets was uploaded");

        List<LoggedExperimentAsset> assets = experiment.getAllAssetList();
        validateAsset(assets, IMAGE_FILE_NAME, IMAGE_FILE_SIZE, TestUtils.SOME_FULL_CONTEXT);
        validateAsset(assets, SOME_TEXT_FILE_NAME, SOME_TEXT_FILE_SIZE, TestUtils.SOME_FULL_CONTEXT);

        // update one of the assets and validate
        //
        onComplete = new OnlineExperimentTest.OnCompleteAction();
        experiment.logAssetFileAsync(Objects.requireNonNull(TestUtils.getFile(ANOTHER_TEXT_FILE_NAME)),
                SOME_TEXT_FILE_NAME, true, TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));
        awaitForCondition(onComplete, "update text file onComplete timeout", 30);

        awaitForCondition(() -> {
            List<LoggedExperimentAsset> assetList = experiment.getAllAssetList();
            return assetList.stream()
                    .filter(asset -> SOME_TEXT_FILE_NAME.equals(asset.getLogicalPath()))
                    .anyMatch(asset -> {
                        ExperimentContext context = ((LoggedExperimentAssetImpl) asset).getContext();
                        return ANOTHER_TEXT_FILE_SIZE == asset.getSize().orElse(0L)
                                && Objects.equals(context.getStep(), TestUtils.SOME_FULL_CONTEXT.getStep())
                                && context.getContext().equals(TestUtils.SOME_FULL_CONTEXT.getContext());
                    });

        }, "Asset was updated");

        experiment.end();
    }

    @Test
    public void testLogAndGetAssetsFolder() {
        OnlineExperimentImpl experiment = (OnlineExperimentImpl) createOnlineExperiment();

        // Make sure experiment has no assets
        //
        assertTrue(experiment.getAllAssetList().isEmpty());

        // Log assets folder and wait for completion
        //
        OnlineExperimentTest.OnCompleteAction onComplete = new OnlineExperimentTest.OnCompleteAction();
        experiment.logAssetFolder(
                assetsFolder.toFile(), false, true, false,
                TestUtils.SOME_FULL_CONTEXT, Optional.of(onComplete));

        awaitForCondition(onComplete, "log assets' folder timeout", 60);

        // wait for assets become available and validate results
        //
        awaitForCondition(() ->
                experiment.getAllAssetList().size() == assetFolderFiles.size(), "Assets was uploaded");

        List<LoggedExperimentAsset> assets = experiment.getAllAssetList();

        validateAsset(assets, SOME_TEXT_FILE_NAME, SOME_TEXT_FILE_SIZE, TestUtils.SOME_FULL_CONTEXT);
        validateAsset(assets, ANOTHER_TEXT_FILE_NAME, ANOTHER_TEXT_FILE_SIZE, TestUtils.SOME_FULL_CONTEXT);
        validateAsset(assets, emptyAssetFile.getFileName().toString(), 0, TestUtils.SOME_FULL_CONTEXT);
        validateAsset(assets, IMAGE_FILE_NAME, IMAGE_FILE_SIZE, TestUtils.SOME_FULL_CONTEXT);
        validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE, TestUtils.SOME_FULL_CONTEXT);

        experiment.end();
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
}
