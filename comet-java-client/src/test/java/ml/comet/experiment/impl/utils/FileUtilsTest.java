package ml.comet.experiment.impl.utils;

import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import org.apache.commons.io.file.PathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileUtilsTest {
    private final static String someExtension = ".txt";

    private Path root;
    private Path subFolderFile;
    private Path subDir;
    private List<Path> allFiles;
    private List<Path> topFiles;

    @BeforeEach
    public void setup() throws IOException {
        allFiles = new ArrayList<>();
        topFiles = new ArrayList<>();
        // create temporary directory tree
        root = Files.createTempDirectory("testFileUtils");
        topFiles.add(
                Files.createTempFile(root, "a_file", someExtension));
        topFiles.add(
                Files.createTempFile(root, "b_file", someExtension));
        topFiles.add(
                Files.createTempFile(root, "c_file", someExtension));

        allFiles.addAll(topFiles);

        subDir = Files.createTempDirectory(root, "subDir");
        subFolderFile = Files.createTempFile(subDir, "d_file", someExtension);
        allFiles.add(subFolderFile);
        allFiles.add(
                Files.createTempFile(subDir, "e_file", someExtension));
    }

    @AfterEach
    public void teardown() throws IOException {
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

    @Test
    public void testRenameWithTimestamp() throws IOException {
        Path file = Files.createTempFile(root, "file_to_rename", ".txt");
        Instant now = Instant.now();
        Path renamed = FileUtils.renameWithTimestamp(file, now);
        assertTrue(Files.isRegularFile(renamed));
        assertFalse(Files.isRegularFile(file));
    }

    @Test
    public void testResolveAssetPath() throws IOException {
        String fileName = "not_existing_file" + someExtension;
        Path subSubDir = subDir.resolve("not_existing_dir");
        Path file = subSubDir.resolve(fileName);
        Path resolved = FileUtils.resolveAssetPath(subDir, file, AssetOverwriteStrategy.FAIL);
        assertEquals(file.getFileName(), resolved.getFileName());

        assertTrue(Files.isDirectory(subSubDir), "parent dir expected");
    }

    @Test
    public void testResolveAssetPath_OVERWRITE() throws IOException {
        String prefix = "file_to_overwrite";
        Path file = Files.createTempFile(subDir, prefix, someExtension);
        Path resolved = FileUtils.resolveAssetPath(subDir, file, AssetOverwriteStrategy.OVERWRITE);
        assertEquals(file.getFileName(), resolved.getFileName());

        // check that resolved file doesn't exist
        assertFalse(Files.isRegularFile(resolved));

        // check that original file doesn't exist either
        try (Stream<Path> files = Files.walk(subDir)) {
            assertFalse(files
                    .peek(System.out::println)
                    .anyMatch(path -> path.getFileName().toString().startsWith(prefix)));
        }
    }

    @Test
    public void testResolveAssetPath_PRESERVE() throws IOException {
        String prefix = "file_to_preserve";
        Path file = Files.createTempFile(subDir, prefix, someExtension);
        Path resolved = FileUtils.resolveAssetPath(subDir, file, AssetOverwriteStrategy.PRESERVE);
        assertEquals(file.getFileName(), resolved.getFileName());

        // check that resolved file doesn't exist yet
        assertFalse(Files.isRegularFile(resolved));

        // check that original file was renamed
        try (Stream<Path> files = Files.walk(subDir)) {
            assertTrue(files
                    .peek(System.out::println)
                    .anyMatch(path -> path.getFileName().toString().startsWith(prefix)));
        }
    }

    @Test
    public void testResolveAssetPath_FAIL() {
        assertThrows(FileAlreadyExistsException.class, () ->
                FileUtils.resolveAssetPath(subDir, subFolderFile, AssetOverwriteStrategy.FAIL));
    }
}
