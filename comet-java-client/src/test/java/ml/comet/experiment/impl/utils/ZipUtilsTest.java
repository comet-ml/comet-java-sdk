package ml.comet.experiment.impl.utils;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipInputStream;

import static ml.comet.experiment.impl.TestUtils.getFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZipUtilsTest {

    final static String MODEL_ZIP_FILE_NAME = "some-model-example.zip";
    final static List<String> MODEL_PATH_NAMES = Arrays.asList(
            "somemodelexample", "someExampleModelData.dat", "onlineExperimentExample", "empty_file.txt",
            "subDir", "graph.json", "report.html", "chart.png", "model.hd5", "amazing chart.png");
    final static int MODEL_FILES_NUMBER = 15;

    @Test
    public void testUnzipToFolder() throws IOException {
        Path tmpDir = Files.createTempDirectory("testUnzipToFolder");
        File modelFile = getFile(MODEL_ZIP_FILE_NAME);
        assertNotNull(modelFile, "model's ZIP file not found");
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(modelFile))) {
            int files = ZipUtils.unzipToFolder(zis, tmpDir);
            assertEquals(MODEL_FILES_NUMBER, files, "wrong files number");
            // Check that all files/folders from ZIP was properly deflated
            //
            Files.walk(tmpDir).peek(System.out::println).forEach(path -> {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(tmpDir.getFileName().toString())
                        && !fileName.startsWith(".") && !fileName.startsWith("__")) {
                    // skip top folder and hidden system files
                    // process only normal files
                    assertTrue(MODEL_PATH_NAMES.contains(fileName), String.format("file name not found: %s", fileName));
                }
            });
        } finally {
            PathUtils.delete(tmpDir);
        }
    }
}
