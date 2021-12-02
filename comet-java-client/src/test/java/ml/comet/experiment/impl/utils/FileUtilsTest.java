package ml.comet.experiment.impl.utils;

import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest {
    private static Path root;
    private static Path subFolderFile;
    private static Path subDir;
    private static List<Path> allFiles;
    private static List<Path> topFiles;

    @BeforeAll
    static void setup() throws IOException {
        allFiles = new ArrayList<>();
        topFiles = new ArrayList<>();
        // create temporary directory tree
        root = Files.createTempDirectory("testFileUtils");
        topFiles.add(
                Files.createTempFile(root, "a_file", ".txt"));
        topFiles.add(
                Files.createTempFile(root, "b_file", ".txt"));
        topFiles.add(
                Files.createTempFile(root, "c_file", ".txt"));

        allFiles.addAll(topFiles);

        subDir = Files.createTempDirectory(root, "subDir");
        subFolderFile = Files.createTempFile(subDir, "d_file", ".txt");
        allFiles.add(subFolderFile);
        allFiles.add(
                Files.createTempFile(subDir, "e_file", ".txt"));
    }

    @AfterAll
    static void teardown() throws IOException {
        PathUtils.delete(root);
        assertFalse(Files.exists(root), "Directory still exists");
    }

    @Test
    public void testListFilesPlain() throws IOException {
        Stream<Path> files = FileUtils.listFiles(root.toFile(), false);
        assertEquals(topFiles.size(), files.count());

        files = FileUtils.listFiles(root.toFile(), false);
        assertTrue(files.peek(System.out::println)
                .allMatch(path -> topFiles.contains(path)));
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        Stream<Path> files = FileUtils.listFiles(root.toFile(), true);
        assertEquals(allFiles.size(), files.count());

        files = FileUtils.listFiles(root.toFile(), true);
        assertTrue(files.peek(System.out::println)
                .allMatch(path -> allFiles.contains(path)));
    }

    @Test
    public void testResolveAssetFileNameSimple() {
        // test only file name
        String expected = subFolderFile.getFileName().toString();
        String name = FileUtils.resolveAssetFileName(root.toFile(), subFolderFile, false, false);
        System.out.println(name);
        assertEquals(expected, name, "wrong simple file name");
    }

    @Test
    public void testResolveAssetFileNameRelative() {
        // test relative path
        String expected = subDir.getFileName().resolve(
                subFolderFile.getFileName()).toString();
        String name = FileUtils.resolveAssetFileName(root.toFile(), subFolderFile, true, false);
        System.out.println(name);
        assertEquals(expected, name, "wrong relative file name");
    }

    @Test
    public void testResolveAssetFileNameWithPrefix() {
        // test absolute path
        String expected = root.getFileName().resolve(
                        subDir.getFileName()).resolve(
                        subFolderFile.getFileName())
                .toString();
        String name = FileUtils.resolveAssetFileName(root.toFile(), subFolderFile, true, true);
        System.out.println(name);
        assertEquals(expected, name, "wrong absolute file name");
    }
}
