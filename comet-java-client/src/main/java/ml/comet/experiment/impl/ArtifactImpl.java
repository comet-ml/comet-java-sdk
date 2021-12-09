package ml.comet.experiment.impl;

import lombok.Getter;
import lombok.NonNull;
import ml.comet.experiment.artifact.Artifact;
import ml.comet.experiment.artifact.ArtifactBuilder;
import ml.comet.experiment.impl.asset.Asset;
import ml.comet.experiment.impl.asset.RemoteAsset;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Optional.empty;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromData;
import static ml.comet.experiment.impl.utils.AssetUtils.createAssetFromFile;
import static ml.comet.experiment.impl.utils.AssetUtils.createRemoteAsset;

/**
 * The implementation of the {@link Artifact}.
 */
public final class ArtifactImpl implements Artifact {
    @Getter
    private final String name;
    @Getter
    private final String type;
    @Getter
    private Set<String> artifactAliases;
    @Getter
    private Map<String, Object> artifactMetadata;

    private final List<Asset> assets;

    ArtifactImpl(String name, String type) {
        this.name = name;
        this.type = type;
        this.assets = new ArrayList<>();
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
        this.assets.add(asset);
    }

    @Override
    public void addAsset(byte[] data, String name, boolean overwrite, @NonNull Map<String, Object> metadata) {
        this.addAsset(data, name, overwrite, Optional.of(metadata));
    }

    @Override
    public void addAsset(byte[] data, String name, boolean overwrite) {
        this.addAsset(data, name, overwrite, Optional.empty());
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void addAsset(byte[] data, @NonNull String name,
                          boolean overwrite, @NonNull Optional<Map<String, Object>> metadata) {
        Asset asset = createAssetFromData(data, name, overwrite, metadata, empty());
        this.assets.add(asset);
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

        public ArtifactBuilderImpl withAliases(@NonNull Set<String> aliases) {
            this.artifact.artifactAliases = aliases;
            return this;
        }

        public ArtifactBuilderImpl withMetadata(@NonNull Map<String, Object> metadata) {
            this.artifact.artifactMetadata = metadata;
            return this;
        }

        public Artifact build() {
            return this.artifact;
        }
    }
}
