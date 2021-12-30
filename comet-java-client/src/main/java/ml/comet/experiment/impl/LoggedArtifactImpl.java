package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactDownloadException;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.model.FileAsset;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static ml.comet.experiment.impl.resources.LogMessages.ARTIFACT_ASSETS_DOWNLOAD_COMPLETED;
import static ml.comet.experiment.impl.resources.LogMessages.ARTIFACT_HAS_NO_ASSETS_TO_DOWNLOAD;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS;
import static ml.comet.experiment.impl.resources.LogMessages.START_DOWNLOAD_ARTIFACT_ASSETS;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

/**
 * The implementation of the {@link LoggedArtifact}.
 */
@ToString
public final class LoggedArtifactImpl extends BaseArtifactImpl implements LoggedArtifact {
    @Getter
    private final Logger logger = LoggerFactory.getLogger(LoggedArtifact.class);

    @Setter
    Set<String> artifactTags;
    @Setter
    long sizeInBytes;
    @Setter
    String experimentKey;
    @Setter
    String workspace;
    @Setter
    String artifactVersionId;
    @Setter
    @Getter
    String artifactId;

    final BaseExperiment baseExperiment;

    LoggedArtifactImpl(String name, String type, BaseExperiment baseExperiment) {
        super(name, type);
        this.baseExperiment = baseExperiment;
    }

    @Override
    public Set<String> getArtifactTags() {
        if (this.artifactTags == null) {
            return Collections.emptySet();
        }
        return this.artifactTags;
    }

    @Override
    public String getArtifactType() {
        return this.getType();
    }

    @Override
    public long getSize() {
        return this.sizeInBytes;
    }

    @Override
    public String getSourceExperimentKey() {
        return this.experimentKey;
    }

    @Override
    public String getVersion() {
        if (this.semanticVersion != null) {
            return this.semanticVersion.getValue();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String getWorkspace() {
        return this.workspace;
    }

    @Override
    public String getVersionId() {
        return this.artifactVersionId;
    }

    @Override
    public Collection<LoggedArtifactAsset> readAssets() throws ArtifactException {
        return this.baseExperiment.readArtifactAssets(this);
    }

    @Override
    public Collection<LoggedArtifactAsset> download(Path folder) {
        return this.download(folder, AssetOverwriteStrategy.FAIL);
    }

    @Override
    public Collection<LoggedArtifactAsset> download(
            @NonNull Path folder, @NonNull AssetOverwriteStrategy overwriteStrategy) throws ArtifactException {
        // read all assets associated with this artifact
        Collection<LoggedArtifactAsset> assets = this.readAssets();
        int assetsToDownload = assets.stream()
                .filter(loggedArtifactAsset -> !loggedArtifactAsset.isRemote())
                .mapToInt(value -> 1)
                .sum();
        if (assetsToDownload == 0) {
            // show warning and return
            this.logger.warn(getString(
                    ARTIFACT_HAS_NO_ASSETS_TO_DOWNLOAD, this.getWorkspace(), this.getName(), this.getVersion()));
            return assets;
        }

        this.logger.info(getString(START_DOWNLOAD_ARTIFACT_ASSETS, assetsToDownload));

        // create parallel execution flow with errors delaying
        // allowing processing of items even if some of them failed
        Observable<FileAsset> observable = Observable.fromStream(assets.stream())
                .filter(loggedArtifactAsset -> !loggedArtifactAsset.isRemote())
                .flatMap(loggedArtifactAsset ->
                        Observable.just(loggedArtifactAsset)
                                .subscribeOn(Schedulers.io()) // make it parallel on IO scheduler
                                .map(asset -> asset.download(folder, overwriteStrategy)), true);

        // subscribe and wait for processing results
        observable
                .ignoreElements() // ignore items - we are only interested in overall result
                .blockingSubscribe(
                        () -> logger.info(
                                getString(ARTIFACT_ASSETS_DOWNLOAD_COMPLETED,
                                        this.getWorkspace(), this.getName(), this.getVersion(),
                                        assetsToDownload, folder)),
                        throwable -> logger.error(
                                getString(FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS,
                                        this.getWorkspace(), this.getName(), this.getVersion(), folder),
                                throwable
                        )
                );

        return assets;
    }

    FileAsset downloadAsset(@NonNull LoggedArtifactAssetImpl asset, @NonNull Path dir,
                            @NonNull Path file, @NonNull AssetOverwriteStrategy overwriteStrategy)
            throws ArtifactException {
        return this.baseExperiment.downloadArtifactAsset(asset, dir, file, overwriteStrategy);
    }

    /**
     * The maximum size of array to allocate.
     * Some VMs reserve some header words in an array.
     * Attempts to allocate larger arrays may result in
     * OutOfMemoryError: Requested array size exceeds VM limit
     */
    private static final int MAX_BUFFER_SIZE = Integer.MAX_VALUE - 8;

    ByteBuffer load(@NonNull LoggedArtifactAssetImpl asset) throws ArtifactException, OutOfMemoryError {
        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory(null);
            Path file = FileSystems.getDefault().getPath(asset.getFileName());

            FileAsset downloaded = this.downloadAsset(asset, tmpDir, file, AssetOverwriteStrategy.OVERWRITE);
            if (downloaded.getSize() > (long) MAX_BUFFER_SIZE) {
                throw new OutOfMemoryError("The asset file size is too large to be loaded as byte array");
            }
            return ByteBuffer.wrap(FileUtils.readFileToByteArray(downloaded.getPath().toFile()));

        } catch (IOException e) {
            this.logger.error("Failed to create temporary file to store content of the asset {}.", asset, e);
            throw new ArtifactDownloadException("Failed to create temporary file to store asset's content.", e);
        } finally {
            // delete temporary file
            if (tmpDir != null) {
                try {
                    FileUtils.deleteDirectory(tmpDir.toFile());
                } catch (IOException e) {
                    this.logger.warn("Failed to clean the temporary directory while loading asset's content.", e);
                }
            }
        }
    }
}
