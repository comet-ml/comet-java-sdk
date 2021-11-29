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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static ml.comet.experiment.impl.asset.AssetType.ASSET_TYPE_ASSET;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AssetUtilsTest {
    private static Path root;
    private static Path subFolderFile;

    @BeforeAll
    static void setup() throws IOException {
        // create temporary directory tree
        root = Files.createTempDirectory("testFileUtils");
        Files.createTempFile(root, "a_file", ".txt");
        Files.createTempFile(root, "b_file", ".txt");
        Files.createTempFile(root, "c_file", ".txt");

        Path subDir = Files.createTempDirectory(root, "subDir");
        subFolderFile = Files.createTempFile(subDir, "d_file", ".txt");
        Files.createTempFile(subDir, "e_file", ".txt");
    }

    @AfterAll
    static void teardown() throws IOException {
        PathUtils.delete(root);
        assertFalse(Files.exists(root), "Directory still exists");
    }

    @Test
    public void testMapToFileAssetEmptyID() {
        Map<String, Object> meta = new HashMap<String, Object>() {{
            put("int", 1);
            put("bool", true);
            put("string", "test string");
        }};
        long step = 1001;
        long epoch = 1;
        Asset asset = AssetUtils.mapToFileAsset(
                root.toFile(), subFolderFile, false, false,
                ASSET_TYPE_ASSET, null, meta, step, epoch);
        assertNotNull(asset, "asset expected");
        assertEquals(asset.getFile(), subFolderFile.toFile(), "wrong asset file");
        assertEquals(asset.getFileName(), subFolderFile.getFileName().toString(), "wrong asset file name");
        assertEquals(ASSET_TYPE_ASSET, asset.getType(), "wrong type");
        assertEquals(meta, asset.getMetadata(), "wrong metadata");
        assertEquals(step, asset.getStep(), "wrong step value");
        assertEquals(epoch, asset.getEpoch(), "wrong epoch");
        assertFalse(StringUtils.isEmpty(asset.getAssetId()), "asset ID expected");
        assertEquals("txt", asset.getFileExtension(), "wrong file extension");
    }

    @Test
    public void testMapToFileAssetWithID() {
        Map<String, Object> meta = new HashMap<String, Object>() {{
            put("int", 1);
            put("bool", true);
            put("string", "test string");
        }};
        long step = 1001;
        long epoch = 1;
        String assetID = CometUtils.generateGUID();
        Asset asset = AssetUtils.mapToFileAsset(
                root.toFile(), subFolderFile, false, false,
                ASSET_TYPE_ASSET, assetID, meta, step, epoch);
        assertNotNull(asset, "asset expected");
        assertEquals(asset.getFile(), subFolderFile.toFile(), "wrong asset file");
        assertEquals(asset.getFileName(), subFolderFile.getFileName().toString(), "wrong asset file name");
        assertEquals(ASSET_TYPE_ASSET, asset.getType(), "wrong type");
        assertEquals(meta, asset.getMetadata(), "wrong metadata");
        assertEquals(step, asset.getStep(), "wrong step value");
        assertEquals(epoch, asset.getEpoch(), "wrong epoch");
        assertEquals(assetID, asset.getAssetId(), "wrong asset ID");
        assertEquals("txt", asset.getFileExtension(), "wrong file extension");
    }

    @Test
    public void testWalkFolderAssets() throws IOException {
        Map<String, Object> meta = new HashMap<String, Object>() {{
            put("int", 1);
            put("bool", true);
            put("string", "test string");
        }};
        long step = 1001;
        long epoch = 1;
        // tests that correct number of assets returned
        Stream<Asset> assets = AssetUtils.walkFolderAssets(
                root.toFile(), true, true, meta, step, epoch);
        assertEquals(5, assets.count(), "wrong assets count");

        // tests that assets has been populated
        assets = AssetUtils.walkFolderAssets(
                root.toFile(), true, true, meta, step, epoch);
        assertTrue(
                assets.allMatch(
                        asset -> asset.getMetadata().equals(meta) && asset.getStep() == step &&
                                asset.getEpoch() == epoch && !StringUtils.isEmpty(asset.getAssetId())),
                "wrong asset data");

        // tests that known file has correct path recorded
        assets = AssetUtils.walkFolderAssets(
                root.toFile(), true, true, meta, step, epoch);
        assertTrue(
                assets.anyMatch(asset -> asset.getFile().equals(subFolderFile.toFile())),
                "file match expected"
        );
    }
}
