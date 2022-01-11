package ml.comet.experiment.impl.asset;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.asset.AssetType;
import ml.comet.experiment.impl.LoggedArtifactAssetImpl;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Describes artifact's asset data.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class ArtifactAssetImpl extends RemoteAssetImpl implements ArtifactAsset {
    @Getter
    @Setter
    String artifactVersionId;
    Long fileSize;

    /**
     * Creates new instance with specified parameters.
     *
     * @param name      the logical path/name of the asset.
     * @param filePath  the path to the asset file.
     * @param size      the size of the asset file.
     * @param metadata  the meta-data associated with asset.
     * @param assetType the type of the asset.
     */
    public ArtifactAssetImpl(String name, Path filePath, long size, Map<String, Object> metadata, AssetType assetType) {
        this.setLogicalPath(name);
        this.setRawFile(filePath.toFile());
        this.fileSize = size;
        this.metadata = metadata;
        this.setType(assetType);
    }

    /**
     * Creates new instance using values from provided {@link AssetImpl}.
     *
     * @param asset the {@link AssetImpl} to copy data from.
     */
    public ArtifactAssetImpl(AssetImpl asset) {
        this.setRawFile(asset.getRawFile());
        this.setRawFileLikeData(asset.getRawFileLikeData());
        this.setFileExtension(asset.getFileExtension());
        this.logicalPath = asset.getLogicalPath();
        this.type = asset.getType();
        this.overwrite = asset.getOverwrite();
        this.metadata = asset.getMetadata();
    }

    /**
     * Creates new instance using values from provided {@link RemoteAssetImpl}.
     *
     * @param asset the {@link RemoteAssetImpl} to copy relevant data from.
     */
    public ArtifactAssetImpl(RemoteAssetImpl asset) {
        this.setUri(asset.getUri());
        this.logicalPath = asset.getLogicalPath();
        this.type = asset.getType();
        this.overwrite = asset.getOverwrite();
        this.metadata = asset.getMetadata();
    }

    /**
     * Creates new instance using values from provided {@link LoggedArtifactAssetImpl}.
     *
     * @param asset the {@link LoggedArtifactAssetImpl} to get values from.
     */
    public ArtifactAssetImpl(LoggedArtifactAssetImpl asset) {
        if (asset.getLink().isPresent()) {
            this.setUri(asset.getLink().get());
        }
        if (asset.getSize().isPresent()) {
            this.fileSize = asset.getSize().get();
        }

        this.logicalPath = asset.getFileName();
        this.type = asset.getAssetType();
        this.metadata = asset.getMetadata();
        this.artifactVersionId = asset.getArtifactVersionId();
    }

    @Override
    public Optional<Long> getSize() {
        return Optional.ofNullable(this.fileSize);
    }

    @Override
    public boolean isRemote() {
        return this.getLink().isPresent();
    }
}
