package ml.comet.experiment.impl;

import lombok.NonNull;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.ConflictingArtifactAssetNameException;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static ml.comet.experiment.impl.resources.LogMessages.CONFLICTING_ARTIFACT_ASSET_NAME;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

/**
 * The implementation of the {@link ml.comet.experiment.artifact.Artifact} to represent artifact downloaded from the
 * Comet by {@link ml.comet.experiment.artifact.LoggedArtifact#download(Path, AssetOverwriteStrategy)} method.
 */
public final class DownloadedArtifactImpl extends ArtifactImpl {

    private final Set<String> downloadedAssetsIdentifiers;

    DownloadedArtifactImpl(String name, String type) {
        super(name, type);
        this.downloadedAssetsIdentifiers = new HashSet<>();
    }

    void addLoggedAssets(@NonNull Collection<LoggedArtifactAsset> assets) {
        assets.forEach(this::appendAsset);
    }

    private void appendAsset(@NonNull LoggedArtifactAsset asset) {
        ArtifactAssetImpl artifactAsset = new ArtifactAssetImpl((LoggedArtifactAssetImpl) asset);
        this.appendAsset(artifactAsset);
        this.downloadedAssetsIdentifiers.add(asset.getFileName());
    }

    @Override
    <T extends ArtifactAsset> void appendAsset(@NonNull final T asset)
            throws ConflictingArtifactAssetNameException {
        String key = asset.getLogicalPath();
        ArtifactAsset a = this.assetsMap.get(key);
        if (a != null && !this.downloadedAssetsIdentifiers.contains(key)) {
            throw new ConflictingArtifactAssetNameException(
                    getString(CONFLICTING_ARTIFACT_ASSET_NAME, asset, key, a));
        }

        this.assetsMap.put(key, asset);
    }
}
