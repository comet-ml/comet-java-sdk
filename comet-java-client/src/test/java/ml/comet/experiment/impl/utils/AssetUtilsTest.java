package ml.comet.experiment.impl.utils;

import ml.comet.experiment.impl.asset.Asset;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static ml.comet.experiment.impl.utils.AssetUtils.REMOTE_FILE_NAME_DEFAULT;
import static ml.comet.experiment.impl.utils.AssetUtils.createRemoteAsset;
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

    @Test
    public void testMapToFileAsset() {
        Path file = subFolderFiles.get(0);
        Asset asset = AssetUtils.mapToFileAsset(
                root.toFile(), file, false, false);
        assertNotNull(asset, "asset expected");
        assertEquals(asset.getFile(), file.toFile(), "wrong asset file");
        assertEquals(asset.getFileName(), file.getFileName().toString(), "wrong asset file name");
        assertEquals(someFileExtension, asset.getFileExtension(), "wrong file extension");
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
        Asset asset = createRemoteAsset(uri, ofNullable(fileName), false, empty());
        assertEquals(expectedFileName, asset.getFileName());
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
        Stream<Asset> assets = AssetUtils.walkFolderAssets(
                root.toFile(), logFilePath, recursive, prefixWithFolderName);
        assertEquals(expected, assets.count(), "wrong assets count");

        // tests that assets has been properly populated
        //
        assets = AssetUtils.walkFolderAssets(root.toFile(), logFilePath, recursive, prefixWithFolderName);
        assertTrue(
                assets.allMatch(
                        asset -> Objects.equals(asset.getFileExtension(), someFileExtension)
                                && StringUtils.isNotBlank(asset.getFileName())
                                && asset.getFile() != null),
                "wrong asset data");

        // tests that correct file names recorded
        //
        assets = AssetUtils.walkFolderAssets(root.toFile(), logFilePath, recursive, prefixWithFolderName);
        assets.forEach(asset -> checkAssetFilename(asset, logFilePath, recursive, prefixWithFolderName));
    }

    void checkAssetFilename(Asset asset, boolean logFilePath, boolean recursive, boolean prefixWithFolderName) {
        String name = asset.getFileName();
        if (logFilePath && prefixWithFolderName) {
            assertTrue(name.startsWith(root.getFileName().toString()), "must have folder name prefix");
        }
        if (subFolderFiles.contains(asset.getFile().toPath()) && logFilePath) {
            assertTrue(name.contains(subDir.getFileName().toString()), "must include relative file path");
        }

        assertTrue(allFolderFiles.contains(asset.getFile().toPath()), "must be in all files list");
        if (!recursive) {
            assertFalse(subFolderFiles.contains(asset.getFile().toPath()), "must be only in top folder files list");
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
