package ml.comet.experiment.impl;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
    static Path emptyAssetFile;
    static List<Path> assetFolderFiles;

    @BeforeAll
    static void setupAssetsFolder() throws IOException {
        assetFolderFiles = new ArrayList<>();
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

        Path subDir = Files.createTempDirectory(assetsFolder, "subDir");
        assetFolderFiles.add(
                PathUtils.copyFileToDirectory(
                        Objects.requireNonNull(TestUtils.getFile(IMAGE_FILE_NAME)).toPath(), subDir));
        assetFolderFiles.add(
                PathUtils.copyFileToDirectory(
                        Objects.requireNonNull(TestUtils.getFile(CODE_FILE_NAME)).toPath(), subDir));
    }

    @AfterAll
    static void tearDownAssetsFolder() throws IOException {
        PathUtils.delete(assetsFolder);
        assertFalse(Files.exists(assetsFolder), "Directory still exists");
    }
}
