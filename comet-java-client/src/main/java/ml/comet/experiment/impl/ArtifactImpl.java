package ml.comet.experiment.impl;

import com.vdurmont.semver4j.Semver;
import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.ArtifactBuilder;
import ml.comet.experiment.artifact.ConflictingArtifactAssetNameException;
import ml.comet.experiment.impl.asset.ArtifactAssetImpl;
import ml.comet.experiment.impl.asset.AssetImpl;
import ml.comet.experiment.impl.asset.RemoteAssetImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
public class ArtifactImpl extends BaseArtifactImpl implements Artifact {
    @Getter
    final Logger logger = LoggerFactory.getLogger(Artifact.class);

    final HashMap<String, ArtifactAsset> assetsMap;

    private final boolean prefixWithFolderName;

    ArtifactImpl(String name, String type) {
        super(name, type);
        this.assetsMap = new HashMap<>();
        this.prefixWithFolderName = true;
    }

    @Override
    public Collection<ArtifactAsset> getAssets() {
        return this.assetsMap.values();
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
    public void addAsset(File file, String name, boolean overwrite) throws ConflictingArtifactAssetNameException {
        this.addAsset(file, name, overwrite, Optional.empty());
    }

    @Override
    public void addAsset(@NonNull File file, boolean overwrite) {
        this.addAsset(file, file.getName(), false, Optional.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void addAsset(@NonNull File file, @NonNull String name,
                          boolean overwrite, @NonNull Optional<Map<String, Object>> metadata) {
        AssetImpl asset = createAssetFromFile(file, Optional.of(name), overwrite, metadata, empty());
        this.appendAsset(new ArtifactAssetImpl(asset));
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
        AssetImpl asset = createAssetFromData(data, name, overwrite, metadata, empty());
        this.appendAsset(new ArtifactAssetImpl(asset));
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
        RemoteAssetImpl asset = createRemoteAsset(uri, Optional.of(name), overwrite, metadata, empty());
        this.appendAsset(new ArtifactAssetImpl(asset));
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
                .forEach(asset -> this.appendAsset(new ArtifactAssetImpl(asset)));
    }

    <T extends ArtifactAsset> void appendAsset(@NonNull final T asset)
            throws ConflictingArtifactAssetNameException {
        String key = asset.getLogicalPath();
        ArtifactAsset a = this.assetsMap.get(key);
        if (a != null) {
            throw new ConflictingArtifactAssetNameException(
                    getString(CONFLICTING_ARTIFACT_ASSET_NAME, asset, key, a));
        }

        this.assetsMap.put(key, asset);
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
            this.artifact.setMetadata(metadata);
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
