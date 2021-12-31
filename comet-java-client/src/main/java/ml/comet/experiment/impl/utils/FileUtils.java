package ml.comet.experiment.impl.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;
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
                    if (Files.isRegularFile(path)) {
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
     * Allows resolving of the path to the asset file based on provided parameters. As a result of this method
     * invocation the missing parent directories can be created.
     *
     * @param dir               the path to the parent directory of the asset file.
     * @param file              the relative path to the asset file within {@code dir}.
     * @param overwriteStrategy the overwrite strategy to be applied in case file already exists.
     * @return the optional path to the asset file in the file system or empty if asset file already exists and must be
     * preserved.
     * @throws IOException                if an I/O exception occurred.
     * @throws FileAlreadyExistsException if {@code overwriteStrategy} is to FAIL when file already exists.
     */
    @SuppressWarnings({"checkstyle:MissingSwitchDefault"})
    public static Optional<Path> resolveAssetPath(@NonNull Path dir, @NonNull Path file,
                                                  @NonNull AssetOverwriteStrategy overwriteStrategy)
            throws FileAlreadyExistsException, IOException {
        Path assetPath = dir.resolve(file);
        if (Files.isRegularFile(assetPath)) {
            // the file already exists
            switch (overwriteStrategy) {
                case PRESERVE:
                    // return empty to indicate that file overwrites should be skipped
                    return Optional.empty();
                case OVERWRITE:
                    // remove existing file
                    Files.delete(assetPath);
                    return Optional.of(assetPath);
                case FAIL:
                    throw new FileAlreadyExistsException(assetPath.toString());
            }
        }
        // create parent directories
        Files.createDirectories(assetPath.getParent());

        return Optional.of(assetPath);
    }

    static Path renameWithTimestamp(@NonNull Path file, @NonNull Instant instant) throws IOException {
        String name = instant.toString().replace(":", "-");
        name = String.format("%s_%s", file.getFileName(), name);

        Path target = file.resolveSibling(name);
        Files.move(file, target);
        return target;
    }

    static String resolveAssetFileName(File folder, Path path, boolean logFilePath,
                                       boolean prefixWithFolderName) {
        if (logFilePath) {
            // the path relative to the assets' folder root
            Path filePath = folder.toPath().relativize(path);

            if (prefixWithFolderName) {
                filePath = folder.toPath().getFileName().resolve(filePath);
            }
            return filePath.toString();
        } else {
            // the asset's file name
            return path.getFileName().toString();
        }
    }
}
