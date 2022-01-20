package ml.comet.experiment.impl;

import ml.comet.experiment.asset.LoggedExperimentAsset;
import ml.comet.experiment.context.ExperimentContext;
import ml.comet.experiment.impl.asset.LoggedExperimentAssetImpl;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The base superclass for test cases related to the assets processing
 */
public class AssetsBaseTest {
    static final String IMAGE_FILE_NAME = "someChart.png";
    static final String CODE_FILE_NAME = "code_sample.py";
    static final long IMAGE_FILE_SIZE = 31451L;
    static final long CODE_FILE_SIZE = 19L;
    static final String SOME_TEXT_FILE_NAME = "someTextFile.txt";
    static final String ANOTHER_TEXT_FILE_NAME = "anotherTextFile.txt";
    static final long SOME_TEXT_FILE_SIZE = 9L;
    static final long ANOTHER_TEXT_FILE_SIZE = 12L;

    static Path assetsFolder;
    static Path assetsSubFolder;
    static Path emptyAssetFile;
    static List<Path> assetFolderFiles;
    static List<Path> assetSubFolderFiles;

    @BeforeAll
    static void setupAssetsFolder() throws IOException {
        assetFolderFiles = new ArrayList<>();
        assetSubFolderFiles = new ArrayList<>();
        // create temporary directory tree
        assetsFolder = Files.createTempDirectory("assetsBaseTest");
        assetFolderFiles.add(
                PathUtils.copyFileToDirectory(
                        Objects.requireNonNull(TestUtils.getFile(SOME_TEXT_FILE_NAME)).toPath(), assetsFolder));
        assetFolderFiles.add(
                PathUtils.copyFileToDirectory(
                        Objects.requireNonNull(TestUtils.getFile(ANOTHER_TEXT_FILE_NAME)).toPath(), assetsFolder));
        emptyAssetFile = Files.createTempFile(assetsFolder, "c_file", ".txt");
        assetFolderFiles.add(emptyAssetFile);

        assetsSubFolder = Files.createTempDirectory(assetsFolder, "subDir");
        assetSubFolderFiles.add(
                PathUtils.copyFileToDirectory(
                        Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)).toPath(), assetsSubFolder));
        assetSubFolderFiles.add(
                PathUtils.copyFileToDirectory(
                        Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)).toPath(), assetsSubFolder));
        assetFolderFiles.addAll(assetSubFolderFiles);
    }

    @AfterAll
    static void tearDownAssetsFolder() throws IOException {
        PathUtils.delete(assetsFolder);
        assertFalse(Files.exists(assetsFolder), "Directory still exists");
    }

    /**
     * Validates that provided list of {@link LoggedExperimentAsset} is the same as in the local assets' folder.
     *
     * @param assets           the list of {@link LoggedExperimentAsset} to be validated.
     * @param expectedContext  the {@link ExperimentContext} for use.
     * @param expectedMetadata the expected metadata to be associated with the asset.
     * @param flat             if {@code true} the flat file structure will be evaluated, otherwise the actual tree
     *                         structure will be considered.
     * @param recursive        if {@code true} the subfolder files also included.
     */
    static void validateAllAssetsFromFolder(List<LoggedExperimentAsset> assets, ExperimentContext expectedContext,
                                            Map<String, Object> expectedMetadata, boolean flat, boolean recursive) throws IOException {
        int expectedSize = recursive ? assetFolderFiles.size() : assetFolderFiles.size() - assetSubFolderFiles.size();
        assertEquals(expectedSize, assets.size(), "wrong number of assets");

        if (flat) {
            validateAsset(assets, SOME_TEXT_FILE_NAME, SOME_TEXT_FILE_SIZE, expectedMetadata, expectedContext);
            validateAsset(assets, ANOTHER_TEXT_FILE_NAME, ANOTHER_TEXT_FILE_SIZE, expectedMetadata, expectedContext);
            validateAsset(assets, emptyAssetFile.getFileName().toString(), 0, expectedMetadata, expectedContext);
            if (recursive) {
                validateAsset(assets, IMAGE_FILE_NAME, IMAGE_FILE_SIZE, expectedMetadata, expectedContext);
                validateAsset(assets, CODE_FILE_NAME, CODE_FILE_SIZE, expectedMetadata, expectedContext);
            }
        } else {
            validateAsset(assets, relativePath(assetsFolder, SOME_TEXT_FILE_NAME),
                    SOME_TEXT_FILE_SIZE, expectedMetadata, expectedContext);
            validateAsset(assets, relativePath(assetsFolder, ANOTHER_TEXT_FILE_NAME),
                    ANOTHER_TEXT_FILE_SIZE, expectedMetadata, expectedContext);
            validateAsset(assets, relativePath(assetsFolder, emptyAssetFile.getFileName().toString()),
                    0, expectedMetadata, expectedContext);
            if (recursive) {
                validateAsset(assets, relativePath(assetsFolder, IMAGE_FILE_NAME),
                        IMAGE_FILE_SIZE, expectedMetadata, expectedContext);
                validateAsset(assets, relativePath(assetsFolder, CODE_FILE_NAME),
                        CODE_FILE_SIZE, expectedMetadata, expectedContext);
            }
        }
    }

    /**
     * Creates relative path to the file.
     *
     * @param folder the root folder.
     * @param file   the file name.
     * @return the relative path to the file.
     */
    static String relativePath(Path folder, String file) throws IOException {
        try (Stream<Path> files = Files.walk(folder)) {
            Path res = files.filter(path -> path.getFileName().toString().equals(file))
                    .findFirst().orElse(null);
            if (res != null) {
                res = folder.getFileName().resolve(folder.relativize(res));
                return res.toString();
            } else {
                return StringUtils.EMPTY;
            }
        }
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
    static void validateAsset(List<LoggedExperimentAsset> assets, String expectedAssetLogicalPath,
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
    static void validateAsset(List<LoggedExperimentAsset> assets, String expectedAssetLogicalPath,
                              long expectedSize, Map<String, Object> expectedMetadata,
                              ExperimentContext expectedContext) {
        LoggedExperimentAsset asset = assets.stream()
                .filter(loggedExperimentAsset -> expectedAssetLogicalPath.equals(loggedExperimentAsset.getLogicalPath()))
                .findFirst().orElse(null);

        assertNotNull(asset, String.format("asset expected for path: %s", expectedAssetLogicalPath));
        assertEquals(expectedSize, asset.getSize().orElse(0L),
                String.format("wrong asset size: %s", expectedAssetLogicalPath));

        ExperimentContext assetContext = ((LoggedExperimentAssetImpl) asset).getContext();
        assertNotNull(assetContext, String.format("asset context expected: %s", expectedAssetLogicalPath));
        assertEquals(expectedContext.getStep(), assetContext.getStep(),
                String.format("wrong asset's context step: %s", expectedAssetLogicalPath));

        if (StringUtils.isNotBlank(expectedContext.getContext())) {
            assertEquals(expectedContext.getContext(), assetContext.getContext(),
                    String.format("wrong asset's context ID: %s", expectedAssetLogicalPath));
        }

        if (expectedMetadata != null) {
            assertEquals(expectedMetadata, asset.getMetadata(),
                    String.format("wrong metadata: %s", expectedAssetLogicalPath));
        } else {
            assertEquals(0, asset.getMetadata().size(),
                    String.format("empty metadata expected: %s", expectedAssetLogicalPath));
        }
    }
}
