package ml.comet.experiment.impl.utils;

import ml.comet.experiment.asset.Asset;
import ml.comet.experiment.asset.RemoteAsset;
import ml.comet.experiment.impl.TestUtils;
import ml.comet.experiment.impl.asset.AssetImpl;
import ml.comet.experiment.impl.asset.AssetType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static ml.comet.experiment.impl.asset.AssetType.ASSET;
import static ml.comet.experiment.impl.asset.AssetType.NOTEBOOK;
import static ml.comet.experiment.impl.asset.AssetType.SOURCE_CODE;
import static ml.comet.experiment.impl.utils.AssetUtils.REMOTE_FILE_NAME_DEFAULT;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromData;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromFile;
import static ml.comet.experiment.impl.utils.AssetUtils.createRemoteAsset;
import static ml.comet.experiment.impl.utils.AssetUtils.updateAsset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class AssetUtilsTest {
    private static Path root;
    private static Path subDir;
    private static List<Path> allFolderFiles;
    private static List<Path> subFolderFiles;

    private static final String someFileExtension = "txt";

    @BeforeAll
    static void setup() throws IOException {
        allFolderFiles = new ArrayList<>();
        subFolderFiles = new ArrayList<>();
        // create temporary directory tree
        root = Files.createTempDirectory("testAssetUtils");
        allFolderFiles.add(
                Files.createTempFile(root, "a_file", "." + someFileExtension));
        allFolderFiles.add(
                Files.createTempFile(root, "b_file", "." + someFileExtension));
        allFolderFiles.add(
                Files.createTempFile(root, "c_file", "." + someFileExtension));

        subDir = Files.createTempDirectory(root, "subDir");
        subFolderFiles.add(
                Files.createTempFile(subDir, "d_file", "." + someFileExtension));
        subFolderFiles.add(
                Files.createTempFile(subDir, "e_file", "." + someFileExtension));
        allFolderFiles.addAll(subFolderFiles);
    }

    @AfterAll
    static void teardown() throws IOException {
        PathUtils.delete(root);
        assertFalse(Files.exists(root), "Directory still exists");
    }

    @ParameterizedTest
    @CsvSource({
            "CODE,",
            ",someGroup",
            "CODE,someGroup",
            ","
    })
    public void testMapToFileAsset(String type, String grouping) {
        Path file = subFolderFiles.get(0);
        AssetImpl asset = AssetUtils.mapToFileAsset(
                root.toFile(), file, false, false,
                Optional.of(TestUtils.SOME_METADATA), Optional.ofNullable(type), Optional.ofNullable(grouping));
        assertNotNull(asset, "asset expected");
        assertEquals(asset.getRawFile(), file.toFile(), "wrong asset file");
        assertEquals(asset.getLogicalPath(), file.getFileName().toString(), "wrong asset file name");
        assertEquals(someFileExtension, asset.getFileExtension(), "wrong file extension");

        if (StringUtils.isNotBlank(type)) {
            assertEquals(type, asset.getType(), "wrong asset type");
        } else {
            assertEquals(ASSET.type(), asset.getType(), "wrong asset type");
        }

        if (StringUtils.isNotBlank(grouping)) {
            assertTrue(asset.getGroupingName().isPresent(), "grouping expected");
            assertEquals(grouping, asset.getGroupingName().get(), "wrong grouping");
        } else {
            assertFalse(asset.getGroupingName().isPresent(), "no grouping expected");
        }

        assertEquals(TestUtils.SOME_METADATA, asset.getMetadata(), "wrong metadata");
    }

    @ParameterizedTest
    @CsvSource({
            "s3://bucket/folder/file, file",
            "s3://bucket/folder/file.extension, file.extension",
            "https://some.cloud.com/someFile?someQuery=7896, someFile",
            "file:///someDir/someFile.extension, someFile.extension",
            "file:///someDir/subDir, subDir",
            "s3://bucket, " + REMOTE_FILE_NAME_DEFAULT,
    })
    public void testRemoteAssetFileName(URI uri, String expected) {
        String fileName = AssetUtils.remoteAssetFileName(uri);
        assertEquals(expected, fileName);
    }

    @ParameterizedTest
    @CsvSource({
            "s3://bucket/folder/file.extension, someFileName, someFileName",
            "s3://bucket/folder/file.extension,, file.extension",
            "s3://bucket,, " + REMOTE_FILE_NAME_DEFAULT
    })
    public void testCreateRemoteAsset_fileNameSelection(URI uri, String fileName, String expectedFileName) {
        RemoteAsset asset = createRemoteAsset(uri, ofNullable(fileName), false, empty(), empty());
        assertNotNull(asset);
        assertEquals(expectedFileName, asset.getLogicalPath());
    }

    @Test
    public void testCreateRemoteAsset_correctTypeCheck() throws URISyntaxException {
        URI uri = new URI("s3://bucket/folder/someFile");

        RemoteAsset asset = createRemoteAsset(uri, empty(), false, empty(), empty());
        assertNotNull(asset);
        assertEquals(ASSET.type(), asset.getType());

        String expected = NOTEBOOK.type();
        asset = createRemoteAsset(uri, empty(), false, empty(), Optional.of(expected));
        assertNotNull(asset);
        assertEquals(expected, asset.getType());
    }

    @Test
    public void testCreateAssetFromFile() {
        // test with defined logical file name
        //
        String extension = "someExtension";
        String fileName = "someFileName." + extension;
        File file = allFolderFiles.get(0).toFile();
        AssetImpl asset = createAssetFromFile(file, Optional.of(fileName),
                false, empty(), empty());
        assertNotNull(asset);
        assertEquals(file, asset.getRawFile(), "wrong file");
        assertEquals(fileName, asset.getLogicalPath(), "wrong file name");
        assertEquals(extension, asset.getFileExtension(), "wrong file extension");
        assertEquals(ASSET.type(), asset.getType());

        // test with empty logical file name
        //
        asset = createAssetFromFile(file, empty(),
                false, empty(), empty());
        fileName = file.getName();
        extension = FilenameUtils.getExtension(fileName);
        assertNotNull(asset);
        assertEquals(file, asset.getRawFile(), "wrong file");
        assertEquals(fileName, asset.getLogicalPath(), "wrong file name");
        assertEquals(extension, asset.getFileExtension(), "wrong file extension");
        assertEquals(ASSET.type(), asset.getType());
    }

    @Test
    public void testCreateAssetFromFile_correctTypeCheck() {
        File file = allFolderFiles.get(0).toFile();
        Asset asset = createAssetFromFile(file, empty(), false, empty(), empty());
        assertNotNull(asset);
        assertEquals(ASSET.type(), asset.getType());

        String expected = NOTEBOOK.type();
        asset = createAssetFromFile(file, empty(), false, empty(), Optional.of(expected));
        assertNotNull(asset);
        assertEquals(expected, asset.getType());
    }

    @Test
    public void testCreateAssetFromData() {
        byte[] data = "some data string".getBytes(StandardCharsets.UTF_8);
        String fileName = "someFileName." + someFileExtension;

        Asset asset = createAssetFromData(data, fileName, false, empty(), empty());
        assertNotNull(asset);
        assertEquals(data, asset.getFileLikeData().orElse(null));
        assertEquals(fileName, asset.getLogicalPath());
        assertEquals(ASSET.type(), asset.getType());

        String expected = SOURCE_CODE.type();
        asset = createAssetFromData(data, fileName, false, empty(), Optional.of(expected));
        assertNotNull(asset);
        assertEquals(data, asset.getFileLikeData().orElse(null));
        assertEquals(fileName, asset.getLogicalPath());
        assertEquals(expected, asset.getType());
    }

    @Test
    public void testUpdateAsset() {
        boolean overwrite = true;
        Map<String, Object> metadata = new HashMap<String, Object>() {{
            put("someInt", 10);
            put("someString", "test string");
            put("someBoolean", true);
        }};
        String type = NOTEBOOK.type();

        AssetImpl asset = new AssetImpl();
        updateAsset(asset, overwrite, Optional.of(metadata), Optional.of(type));

        assertEquals(overwrite, asset.getOverwrite());
        assertEquals(metadata, asset.getMetadata());
        assertEquals(type, asset.getType());
    }

    @Test
    public void testUpdateAsset_defaultType() {
        AssetImpl asset = new AssetImpl();
        updateAsset(asset, false, empty(), empty());

        assertEquals(ASSET.type(), asset.getType());
    }

    @ParameterizedTest(name = "[{index}] logFilePath: {0}, recursive: {1}, prefixWithFolderName: {2}")
    @MethodSource("walkFolderAssetsModifiers")
    void testWalkFolderAssets(boolean logFilePath, boolean recursive, boolean prefixWithFolderName) throws IOException {
        // tests that correct number of assets returned
        //
        int expected = allFolderFiles.size();
        if (!recursive) {
            expected -= subFolderFiles.size();
        }
        Stream<AssetImpl> assets = AssetUtils.walkFolderAssets(
                root.toFile(), logFilePath, recursive, prefixWithFolderName,
                Optional.of(TestUtils.SOME_METADATA), empty(), empty());
        assertEquals(expected, assets.count(), "wrong assets count");

        // tests that assets has been properly populated
        //
        assets = AssetUtils.walkFolderAssets(root.toFile(), logFilePath, recursive, prefixWithFolderName,
                Optional.of(TestUtils.SOME_METADATA), empty(), empty());
        assertTrue(
                assets.allMatch(
                        asset -> Objects.equals(asset.getFileExtension(), someFileExtension)
                                && StringUtils.isNotBlank(asset.getLogicalPath())
                                && asset.getRawFile() != null),
                "wrong asset data");

        // tests that correct file names recorded
        //
        assets = AssetUtils.walkFolderAssets(root.toFile(), logFilePath, recursive, prefixWithFolderName,
                Optional.of(TestUtils.SOME_METADATA), empty(), empty());
        assets.forEach(asset -> checkAssetFilename(asset, logFilePath, recursive, prefixWithFolderName));
    }

    @ParameterizedTest
    @CsvSource({
            "all, ALL",
            "unknown, UNKNOWN",
            "asset, ASSET",
            "source_code, SOURCE_CODE",
            "3d-points, POINTS_3D",
            "embeddings, EMBEDDINGS",
            "dataframe, DATAFRAME",
            "dataframe-profile, DATAFRAME_PROFILE",
            "histogram3d, HISTOGRAM3D",
            "confusion-matrix, CONFUSION_MATRIX",
            "curve, CURVE",
            "notebook, NOTEBOOK",
            "model-element, MODEL_ELEMENT",
            "text-sample, TEXT_SAMPLE",
            "not-existing-type, UNKNOWN"
    })
    public void testToAssetType(String typeName, String expectedName) {
        AssetType assetType = AssetUtils.toAssetType(typeName);
        AssetType expected = Enum.valueOf(AssetType.class, expectedName);
        assertEquals(expected, assetType, String.format("wrong type parsed for name: %s", typeName));
    }

    void checkAssetFilename(AssetImpl asset, boolean logFilePath, boolean recursive, boolean prefixWithFolderName) {
        String name = asset.getLogicalPath();
        if (logFilePath && prefixWithFolderName) {
            assertTrue(name.startsWith(root.getFileName().toString()), "must have folder name prefix");
        }
        if (subFolderFiles.contains(asset.getRawFile().toPath()) && logFilePath) {
            assertTrue(name.contains(subDir.getFileName().toString()), "must include relative file path");
        }

        assertTrue(allFolderFiles.contains(asset.getRawFile().toPath()), "must be in all files list");
        if (!recursive) {
            assertFalse(subFolderFiles.contains(asset.getRawFile().toPath()), "must be only in top folder files list");
        }
    }

    static Stream<Arguments> walkFolderAssetsModifiers() {
        // create matrix of all possible combinations: 2^3 = 8
        // the order: logFilePath, recursive, prefixWithFolderName
        return Stream.of(
                arguments(false, false, false),
                arguments(true, false, false),
                arguments(true, true, false),
                arguments(true, true, true),
                arguments(false, false, true),
                arguments(false, true, true),
                arguments(true, false, true),
                arguments(false, true, false)
        );
    }
}
