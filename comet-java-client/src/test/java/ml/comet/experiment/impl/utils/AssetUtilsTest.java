package ml.comet.experiment.impl.utils;

import ml.comet.experiment.impl.asset.Asset;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetUtilsTest {
    private static Path root;
    private static Path subFolderFile;
    private static List<Path> assetFolderFiles;

    private static final String someFileExtension = "txt";

    @BeforeAll
    static void setup() throws IOException {
        assetFolderFiles = new ArrayList<>();
        // create temporary directory tree
        root = Files.createTempDirectory("testAssetUtils");
        assetFolderFiles.add(
                Files.createTempFile(root, "a_file", "." + someFileExtension));
        assetFolderFiles.add(
                Files.createTempFile(root, "b_file", "." + someFileExtension));
        assetFolderFiles.add(
                Files.createTempFile(root, "c_file", "." + someFileExtension));

        Path subDir = Files.createTempDirectory(root, "subDir");
        subFolderFile = Files.createTempFile(subDir, "d_file", "." + someFileExtension);
        assetFolderFiles.add(subFolderFile);
        assetFolderFiles.add(
                Files.createTempFile(subDir, "e_file", "." + someFileExtension));
    }

    @AfterAll
    static void teardown() throws IOException {
        PathUtils.delete(root);
        assertFalse(Files.exists(root), "Directory still exists");
    }

    @Test
    public void testMapToFileAsset() {
        Asset asset = AssetUtils.mapToFileAsset(
                root.toFile(), subFolderFile, false, false);
        assertNotNull(asset, "asset expected");
        assertEquals(asset.getFile(), subFolderFile.toFile(), "wrong asset file");
        assertEquals(asset.getFileName(), subFolderFile.getFileName().toString(), "wrong asset file name");
        assertEquals(someFileExtension, asset.getFileExtension(), "wrong file extension");
    }

    @Test
    public void testWalkFolderAssets() throws IOException {
        // tests that correct number of assets returned
        Stream<Asset> assets = AssetUtils.walkFolderAssets(
                root.toFile(), true, true);
        assertEquals(assetFolderFiles.size(), assets.count(), "wrong assets count");

        // tests that assets has been populated
        assets = AssetUtils.walkFolderAssets(root.toFile(), true, true);
        assertTrue(
                assets.allMatch(
                        asset -> Objects.equals(asset.getFileExtension(), someFileExtension)
                                && !StringUtils.isEmpty(asset.getFileName())
                                && asset.getFile() != null),
                "wrong asset data");

        // tests that known file has correct path recorded
        assets = AssetUtils.walkFolderAssets(root.toFile(), true, true);
        assertTrue(
                assets.anyMatch(asset -> asset.getFile().equals(subFolderFile.toFile())),
                "file match expected"
        );
    }
}
