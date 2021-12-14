package ml.comet.experiment.impl;

import com.vdurmont.semver4j.Semver;
import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactBuilder;
import ml.comet.experiment.artifact.ConflictingArtifactAssetNameException;
import ml.comet.experiment.impl.asset.Asset;
import ml.comet.experiment.impl.asset.RemoteAsset;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Optional.empty;
import static ml.comet.experiment.impl.resources.LogMessages.CONFLICTING_ARTIFACT_ASSET_NAME;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromData;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromFile;
import static ml.comet.experiment.impl.utils.AssetUtils.createRemoteAsset;
import static ml.comet.experiment.impl.utils.AssetUtils.walkFolderAssets;

/**
 * The implementation of the {@link Artifact}.
 */
public final class ArtifactImpl extends BaseArtifactImpl implements Artifact {

    @Getter
    private final List<Asset> assets;

    private final boolean prefixWithFolderName;

    ArtifactImpl(String name, String type) {
        super(name, type);
        this.assets = new ArrayList<>();
        this.prefixWithFolderName = true;
    }

    @Override
    public void addAsset(@NonNull File file, @NonNull String name, boolean overwrite, Map<String, Object> metadata) {
        this.addAsset(file, name, overwrite, Optional.of(metadata));
    }

    @Override
    public void addAsset(@NonNull File file, boolean overwrite, @NonNull Map<String, Object> metadata) {
        this.addAsset(file, file.getName(), overwrite, Optional.of(metadata));
    }

    @Override
    public void addAsset(@NonNull File file, boolean overwrite) {
        this.addAsset(file, file.getName(), false, Optional.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void addAsset(@NonNull File file, @NonNull String name,
                          boolean overwrite, @NonNull Optional<Map<String, Object>> metadata) {
        Asset asset = createAssetFromFile(file, Optional.of(name), overwrite, metadata, empty());
        this.appendAsset(asset);
    }

    @Override
    public void addAsset(byte[] data, String name, boolean overwrite, @NonNull Map<String, Object> metadata) {
        this.addAsset(data, name, overwrite, Optional.of(metadata));
    }

    @Override
    public void addAsset(byte[] data, String name, boolean overwrite) {
        this.addAsset(data, name, overwrite, Optional.empty());
    }

    @Override
    public void addAsset(byte[] data, String name) throws ConflictingArtifactAssetNameException {
        this.addAsset(data, name, false);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void addAsset(byte[] data, @NonNull String name,
                          boolean overwrite, @NonNull Optional<Map<String, Object>> metadata) {
        Asset asset = createAssetFromData(data, name, overwrite, metadata, empty());
        this.appendAsset(asset);
    }

    @Override
    public void addRemoteAsset(@NonNull URI uri, @NonNull String name,
                               boolean overwrite, @NonNull Map<String, Object> metadata) {
        this.addRemoteAsset(uri, name, overwrite, Optional.of(metadata));
    }

    @Override
    public void addRemoteAsset(@NonNull URI uri, @NonNull String name, boolean overwrite) {
        this.addRemoteAsset(uri, name, overwrite, empty());
    }

    @Override
    public void addRemoteAsset(@NonNull URI uri, @NonNull String name) {
        this.addRemoteAsset(uri, name, false, empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void addRemoteAsset(@NonNull URI uri, @NonNull String name,
                                boolean overwrite, @NonNull Optional<Map<String, Object>> metadata) {
        RemoteAsset asset = createRemoteAsset(uri, Optional.of(name), overwrite, metadata, empty());
        this.appendAsset(asset);
    }

    @Override
    public void addAssetFolder(@NonNull File folder, boolean logFilePath,
                               boolean recursive, @NonNull Map<String, Object> metadata)
            throws ConflictingArtifactAssetNameException, IOException {
        this.addAssetFolder(folder, logFilePath, recursive, Optional.of(metadata));
    }

    @Override
    public void addAssetFolder(@NonNull File folder, boolean logFilePath, boolean recursive)
            throws ConflictingArtifactAssetNameException, IOException {
        this.addAssetFolder(folder, logFilePath, recursive, empty());
    }

    @Override
    public void addAssetFolder(@NonNull File folder, boolean logFilePath)
            throws ConflictingArtifactAssetNameException, IOException {
        this.addAssetFolder(folder, false, false);
    }

    @Override
    public void addAssetFolder(@NonNull File folder) throws ConflictingArtifactAssetNameException, IOException {
        this.addAssetFolder(folder, false);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void addAssetFolder(@NonNull File folder, boolean logFilePath,
                                boolean recursive, Optional<Map<String, Object>> metadata)
            throws ConflictingArtifactAssetNameException, IOException {

        walkFolderAssets(folder, logFilePath, recursive, this.prefixWithFolderName)
                .peek(asset -> asset.setMetadata(metadata.orElse(null)))
                .forEach(this::appendAsset);
    }

    private void appendAsset(Asset asset) throws ConflictingArtifactAssetNameException {
        this.assets.forEach(a -> {
            if (Objects.equals(a.getFileName(), asset.getFileName())) {
                throw new ConflictingArtifactAssetNameException(
                        getString(CONFLICTING_ARTIFACT_ASSET_NAME, asset, asset.getFileName(), a));
            }
        });

        this.assets.add(asset);
    }

    /**
     * Returns {@link ArtifactBuilder} instance which can be used to create {@link Artifact} instances.
     *
     * @param name the name of the artifact.
     * @param type the type of the artifact.
     * @return the {@link ArtifactBuilder} instance to create properly initialized instances of the {@link Artifact}.
     */
    public static ArtifactBuilder builder(@NonNull String name, @NonNull String type) {
        return new ArtifactBuilderImpl(name, type);
    }

    static final class ArtifactBuilderImpl implements ArtifactBuilder {
        private final ArtifactImpl artifact;

        ArtifactBuilderImpl(String name, String type) {
            this.artifact = new ArtifactImpl(name, type);
        }

        @Override
        public ArtifactBuilderImpl withAliases(@NonNull List<String> aliases) {
            this.artifact.aliases = new HashSet<>(aliases);
            return this;
        }

        @Override
        public ArtifactBuilderImpl withMetadata(@NonNull Map<String, Object> metadata) {
            this.artifact.artifactMetadata = metadata;
            return this;
        }

        @Override
        public ArtifactBuilderImpl withVersion(String version) {
            this.artifact.semanticVersion = new Semver(version);
            return this;
        }

        @Override
        public ArtifactBuilderImpl withVersionTags(@NonNull List<String> tags) {
            this.artifact.versionTags = new HashSet<>(tags);
            return this;
        }

        @Override
        public Artifact build() {
            return this.artifact;
        }
    }
}
