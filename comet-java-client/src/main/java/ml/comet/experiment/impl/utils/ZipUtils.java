package ml.comet.experiment.impl.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * The collection of utilities to work with ZIP files and streams.
 */
public class ZipUtils {

    /**
     * Allows unzipping content of the {@link ZipInputStream} into provided folder.
     *
     * @param zis          the {@link ZipInputStream} to be deflated.
     * @param targetFolder the path to folder where to deflate.
     * @return the number of files extracted.
     * @throws IOException thrown if operation failed due to I/O errors.
     */
    public static int unzipToFolder(ZipInputStream zis, Path targetFolder) throws IOException {
        int filesCount = 0;
        // list files in zip
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {

            Path targetDirResolved = targetFolder.resolve(zipEntry.getName());

            // make sure normalized file still has targetFolder as its prefix
            // else throws exception
            Path normalizedPath = targetDirResolved.normalize();
            if (!normalizedPath.startsWith(targetFolder)) {
                throw new IOException("Bad zip entry: " + zipEntry.getName());
            }

            boolean isFolder = zipEntry.getName().endsWith(File.separator);
            if (isFolder) {
                // some zip store files and folders separately, needs to create folder
                // e.g. data/
                //      data/folder/
                Files.createDirectories(normalizedPath);
            } else {
                // some zip stored file path only, needs to create parent directories
                // e.g. data/folder/file.txt
                if (normalizedPath.getParent() != null) {
                    if (Files.notExists(normalizedPath.getParent())) {
                        Files.createDirectories(normalizedPath.getParent());
                    }
                }

                // copy file data into the path
                Files.copy(zis, normalizedPath, StandardCopyOption.REPLACE_EXISTING);

                filesCount++;
            }
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();

        return filesCount;
    }
}
