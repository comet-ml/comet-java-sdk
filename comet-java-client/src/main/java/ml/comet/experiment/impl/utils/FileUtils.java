package ml.comet.experiment.impl.utils;

import lombok.experimental.UtilityClass;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Provides common file system utilities.
 */
@UtilityClass
public class FileUtils {

    /**
     * Lists files under given folder.
     *
     * @param folder    the folder to list files in.
     * @param recursive if {@code true} then subfolder files will be included recursively.
     * @return the list of files under given directory.
     * @throws IOException if an I/O exception occurs.
     */
    public static Stream<Path> listFiles(File folder, boolean recursive) throws IOException {
        ArrayList<Path> res;
        if (recursive) {
            try (Stream<Path> files = Files.walk(folder.toPath())) {
                res = files.collect(ArrayList::new, (paths, path) -> {
                    if (!path.toFile().isDirectory()) {
                        paths.add(path);
                    }
                }, ArrayList::addAll);
            }
        } else {
            res = new ArrayList<>();
            try (DirectoryStream<Path> files = Files.newDirectoryStream(folder.toPath())) {
                files.forEach(path -> {
                    if (!path.toFile().isDirectory()) {
                        res.add(path);
                    }
                });
            }
        }
        return res.stream().sorted(Comparator.naturalOrder());
    }

    /**
     * Resolves the logical file name of the asset.
     *
     * @param folder       the parent folder of the asset
     * @param path         the path to the asset file
     * @param logFilePath  if {@code true} the absolute of relative file path to be used.
     * @param absolutePath if {@code true} the absolute file path going to be used.
     * @return the logical file name of the asset to be logged.
     */
    public static String resolveAssetFileName(File folder, Path path, boolean logFilePath, boolean absolutePath) {
        if (logFilePath) {
            if (absolutePath) {
                return path.toAbsolutePath().toString();
            } else {
                // the path relative to the assets' folder root
                return folder.toPath().relativize(path).toString();
            }
        } else {
            // the asset's file name
            return path.getFileName().toString();
        }
    }
}
