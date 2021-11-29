package ml.comet.experiment.impl.utils;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest {
    private static Path root;
    private static Path subFolderFile;
    private static Path subDir;

    @BeforeAll
    static void setup() throws IOException {
        // create temporary directory tree
        root = Files.createTempDirectory("testFileUtils");
        Files.createTempFile(root, "a_file", ".txt");
        Files.createTempFile(root, "b_file", ".txt");
        Files.createTempFile(root, "c_file", ".txt");

        subDir = Files.createTempDirectory(root, "subDir");
        subFolderFile = Files.createTempFile(subDir, "d_file", ".txt");
        Files.createTempFile(subDir, "e_file", ".txt");
    }

    @AfterAll
    static void teardown() throws IOException {
        PathUtils.delete(root);
        assertFalse(Files.exists(root), "Directory still exists");
    }

    @Test
    public void testListFilesPlain() throws IOException {
        Stream<Path> files = FileUtils.listFiles(root.toFile(), false);
        assertEquals(3, files.count());

        files = FileUtils.listFiles(root.toFile(), false);
        assertTrue(files.map(Path::getFileName).allMatch(
                path -> path.toString().startsWith("a_file")
                        || path.toString().startsWith("b_file")
                        || path.toString().startsWith("c_file")));
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        Stream<Path> files = FileUtils.listFiles(root.toFile(), true);
        assertEquals(5, files.count());

        files = FileUtils.listFiles(root.toFile(), true);
        assertTrue(files.map(Path::getFileName).allMatch(
                path -> path.toString().startsWith("a_file")
                        || path.toString().startsWith("b_file")
                        || path.toString().startsWith("c_file")
                        || path.toString().startsWith("d_file")
                        || path.toString().startsWith("e_file")));

    }

    @Test
    public void testResolveAssetFileNameSimple() {
        // test only file name
        String expected = subFolderFile.getFileName().toString();
        String name = FileUtils.resolveAssetFileName(root.toFile(), subFolderFile, false, false);
        assertEquals(expected, name, "wrong simple file name");
    }

    @Test
    public void testResolveAssetFileNameRelative() {
        // test relative path
        String expected = subDir.getFileName().toString() + "/" + subFolderFile.getFileName().toString();
        String name = FileUtils.resolveAssetFileName(root.toFile(), subFolderFile, true, false);
        assertEquals(expected, name, "wrong relative file name");
    }

    @Test
    public void testResolveAssetFileNameAbsolute() {
        // test absolute path
        String expected = subFolderFile.toAbsolutePath().toString();
        String name = FileUtils.resolveAssetFileName(root.toFile(), subFolderFile, true, true);
        assertEquals(expected, name, "wrong absolute file name");
    }
}
