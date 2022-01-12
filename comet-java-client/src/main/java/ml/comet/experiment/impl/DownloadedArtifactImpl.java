package ml.comet.experiment.impl;

import com.vdurmont.semver4j.Semver;
import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.ConflictingArtifactAssetNameException;
import ml.comet.experiment.artifact.DownloadedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static ml.comet.experiment.impl.resources.LogMessages.CONFLICTING_ARTIFACT_ASSET_NAME;
import static ml.comet.experiment.impl.resources.LogMessages.FAILED_TO_SET_ARTIFACT_VERSION_LEQ_THAN_CURRENT;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

/**
 * The implementation of the {@link ml.comet.experiment.artifact.Artifact} to represent artifact downloaded from the
 * Comet by {@link ml.comet.experiment.artifact.LoggedArtifact#download(Path, AssetOverwriteStrategy)} method.
 */
public final class DownloadedArtifactImpl extends ArtifactImpl implements DownloadedArtifact {
    @Getter
    private final Logger logger = LoggerFactory.getLogger(DownloadedArtifact.class);

    private String artifactId;
    private String workspace;
    private final Set<String> downloadedAssetsIdentifiers;

    DownloadedArtifactImpl(String name, String type, String version) {
        super(name, type);
        this.downloadedAssetsIdentifiers = new HashSet<>();
        this.semanticVersion = new Semver(version);
    }

    DownloadedArtifactImpl(LoggedArtifactImpl loggedArtifact) {
        this(loggedArtifact.getName(), loggedArtifact.getType(), loggedArtifact.getVersion());
        // fill properties
        this.setMetadata(loggedArtifact.getMetadata());
        this.artifactId = loggedArtifact.getArtifactId();
        this.aliases = loggedArtifact.getAliases();
        this.versionTags = loggedArtifact.getVersionTags();
        this.workspace = loggedArtifact.getWorkspace();
    }

    @Override
    public String getArtifactId() {
        return this.artifactId;
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public String getArtifactType() {
        return super.getType();
    }

    @Override
    public String getWorkspace() {
        return this.workspace;
    }

    @Override
    public String getVersion() {
        return this.semanticVersion.getValue();
    }

    @Override
    public boolean setVersion(String version) {
        Semver newVersion = new Semver(version);
        if (newVersion.isLowerThanOrEqualTo(this.semanticVersion)) {
            this.logger.error(getString(
                    FAILED_TO_SET_ARTIFACT_VERSION_LEQ_THAN_CURRENT, version, this.semanticVersion.getValue()));
            return false;
        } else {
            this.semanticVersion = newVersion;
            return true;
        }
    }

    @Override
    public String incrementMinorVersion() {
        this.semanticVersion = this.semanticVersion.nextMinor();
        return this.getVersion();
    }

    @Override
    public String incrementMajorVersion() {
        this.semanticVersion = this.semanticVersion.nextMajor();
        return this.getVersion();
    }

    @Override
    public String incrementPatchVersion() {
        this.semanticVersion = this.semanticVersion.nextPatch();
        return this.getVersion();
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
