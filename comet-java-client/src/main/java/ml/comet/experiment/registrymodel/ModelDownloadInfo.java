package ml.comet.experiment.registrymodel;

import java.nio.file.Path;

/**
 * Holds information about model download.
 */
public class ModelDownloadInfo {
    private final Path downloadPath;
    private final DownloadModelOptions downloadOptions;

    /**
     * Creates new instance with specified parameters.
     *
     * @param downloadPath    the path where model was saved.
     * @param downloadOptions the options used to download registry model's assets.
     */
    public ModelDownloadInfo(Path downloadPath, DownloadModelOptions downloadOptions) {
        this.downloadPath = downloadPath;
        this.downloadOptions = downloadOptions;
    }

    /**
     * Returns path where model was saved, either directory or file. If {@link DownloadModelOptions#isExpand()} set
     * to {@code true} it would be the path to the directory where model files was extracted, otherwise it would be
     * the path to the ZIP file with model's assets.
     *
     * @return the path where model was saved, either directory or file.
     */
    public Path getDownloadPath() {
        return this.downloadPath;
    }

    /**
     * Returns options which was used to download registry model's assets.
     *
     * @return the options which was used to download registry model's assets.
     */
    public DownloadModelOptions getDownloadOptions() {
        return this.downloadOptions;
    }
}
