package ml.comet.experiment.impl;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.ArtifactDownloadException;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.DownloadedArtifact;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import ml.comet.experiment.impl.utils.ArtifactUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.nio.file.StandardOpenOption.READ;
import static ml.comet.experiment.impl.resources.LogMessages.ARTIFACT_ASSETS_DOWNLOAD_COMPLETED;
import static ml.comet.experiment.impl.resources.LogMessages.ARTIFACT_HAS_NO_ASSETS_TO_DOWNLOAD;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS;
import static ml.comet.experiment.impl.resources.LogMessages.START_DOWNLOAD_ARTIFACT_ASSETS;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

/**
 * The implementation of the {@link LoggedArtifact}.
 */
@ToString(onlyExplicitlyIncluded = true)
public final class LoggedArtifactImpl extends BaseArtifactImpl implements LoggedArtifact {
    @Getter
    private final Logger logger = LoggerFactory.getLogger(LoggedArtifact.class);

    @Setter
    Set<String> artifactTags;
    @Setter
    long sizeInBytes;
    @Setter
    @ToString.Include
    String experimentKey;
    @Setter
    @ToString.Include
    String workspace;
    @Setter
    @ToString.Include
    String artifactVersionId;
    @Setter
    @Getter
    @ToString.Include
    String artifactId;

    final BaseExperiment baseExperiment;

    LoggedArtifactImpl(String name, String type, BaseExperiment baseExperiment) {
        super(name, type);
        this.baseExperiment = baseExperiment;
    }

    @Override
    public Set<String> getArtifactTags() {
        if (this.artifactTags == null) {
            return new HashSet<>();
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
    public String getFullName() {
        return ArtifactUtils.artifactFullName(this.getWorkspace(), this.getName(), this.getVersion());
    }

    @Override
    public Collection<LoggedArtifactAsset> getRemoteAssets() throws ArtifactException {
        return this.baseExperiment.readArtifactAssets(this)
                .stream()
                .filter(LoggedArtifactAsset::isRemote)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public Collection<LoggedArtifactAsset> getAssets() throws ArtifactException {
        return this.baseExperiment.readArtifactAssets(this);
    }

    @Override
    public DownloadedArtifact download(Path folder) {
        return this.download(folder, AssetOverwriteStrategy.FAIL_IF_DIFFERENT);
    }

    @Override
    public DownloadedArtifact download(@NonNull Path folder, @NonNull AssetOverwriteStrategy overwriteStrategy)
            throws ArtifactException {
        // create downloaded artifact
        DownloadedArtifactImpl artifact = new DownloadedArtifactImpl(this);

        // read all assets associated with this artifact
        Collection<LoggedArtifactAsset> assets = this.getAssets();
        artifact.addLoggedAssets(assets);

        // check if there is assets to be downloaded
        int assetsToDownload = assets.stream()
                .filter(loggedArtifactAsset -> !loggedArtifactAsset.isRemote())
                .mapToInt(value -> 1)
                .sum();
        if (assetsToDownload == 0) {
            // show warning and return
            this.logger.warn(getString(ARTIFACT_HAS_NO_ASSETS_TO_DOWNLOAD, this.getFullName()));
            return artifact;
        }

        this.logger.info(getString(START_DOWNLOAD_ARTIFACT_ASSETS, assetsToDownload));

        // create parallel execution flow with errors delaying
        // allowing processing of items even if some of them failed
        Observable<ArtifactAsset> observable = Observable.fromStream(assets.stream())
                .filter(loggedArtifactAsset -> !loggedArtifactAsset.isRemote())
                .flatMap(loggedArtifactAsset ->
                        Observable.just(loggedArtifactAsset)
                                .subscribeOn(Schedulers.io()) // make it parallel on IO scheduler
                                .map(asset -> asset.download(folder, overwriteStrategy)), true);


        // subscribe and wait for processing results
        CompletableFuture<Void> result = new CompletableFuture<>();
        observable
                .doOnNext(artifact::updateAsset) // update artifact asset
                .ignoreElements() // ignore items - we are interested in overall result
                .blockingSubscribe(
                        () -> {
                            logger.info(getString(ARTIFACT_ASSETS_DOWNLOAD_COMPLETED,
                                    this.getFullName(), assetsToDownload, folder));
                            result.complete(null);
                        },
                        throwable -> {
                            logger.error(
                                    getString(FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS, this.getFullName(), folder),
                                    throwable);
                            result.completeExceptionally(throwable);
                        }
                );

        // check if any exception was raised during download and raise ArtifactException
        try {
            result.get();
        } catch (ExecutionException ex) {
            throw new ArtifactException(getString(FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS, this.getFullName(), folder),
                    ex.getCause());
        } catch (InterruptedException ex) {
            throw new ArtifactException(getString(FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS, this.getFullName(), folder), ex);
        }

        return artifact;
    }

    ArtifactAssetImpl downloadAsset(@NonNull LoggedArtifactAssetImpl asset, @NonNull Path dir,
                                    @NonNull Path file, @NonNull AssetOverwriteStrategy overwriteStrategy)
            throws ArtifactException {
        return this.baseExperiment.downloadArtifactAsset(asset, dir, file, overwriteStrategy);
    }

    void writeAssetTo(@NonNull LoggedArtifactAssetImpl asset, @NonNull OutputStream out) throws ArtifactException {
        Path tmpDir = null;
        try {
            tmpDir = Files.createTempDirectory(null);
            Path file = FileSystems.getDefault().getPath(asset.getLogicalPath());

            ArtifactAssetImpl downloaded = this.downloadAsset(asset, tmpDir, file, AssetOverwriteStrategy.OVERWRITE);
            Files.copy(downloaded.getRawFile().toPath(), out);
            out.flush();
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

    InputStream openAssetStream(@NonNull LoggedArtifactAssetImpl asset) throws ArtifactException {
        try {
            Path tmpDir = Files.createTempDirectory(null);
            Path file = FileSystems.getDefault().getPath(asset.getLogicalPath());
            file.toFile().deleteOnExit(); // make sure to delete temporary file

            ArtifactAssetImpl downloaded = this.downloadAsset(asset, tmpDir, file, AssetOverwriteStrategy.OVERWRITE);
            return Files.newInputStream(downloaded.getRawFile().toPath(), READ);
        } catch (IOException e) {
            this.logger.error("Failed to create temporary file to store content of the asset {}.", asset, e);
            throw new ArtifactDownloadException("Failed to create temporary file to store asset's content.", e);
        }
    }
}
