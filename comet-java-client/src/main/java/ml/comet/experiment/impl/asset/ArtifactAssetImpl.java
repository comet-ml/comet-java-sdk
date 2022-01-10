package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.asset.AssetType;

import java.net.URI;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * Describes artifact's asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ArtifactAssetImpl extends AssetImpl implements ArtifactAsset {
    String artifactVersionId;
    Long fileSize;
    private URI link;

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
        this.setFile(filePath.toFile());
        this.fileSize = size;
        this.metadata = metadata;
        this.setType(assetType);
    }

    /**
     * The copy constructor.
     *
     * @param asset the {@link AssetImpl} to copy data from.
     */
    public ArtifactAssetImpl(AssetImpl asset) {
        this.setFile(asset.getFile());
        this.setFileLikeData(asset.getFileLikeData());
        this.setFileExtension(asset.getFileExtension());
        this.logicalPath = asset.getLogicalPath();
        this.type = asset.getType();
        this.overwrite = asset.getOverwrite();
        this.metadata = asset.getMetadata();
    }

    /**
     * The copy constructor.
     *
     * @param asset the {@link RemoteAssetImpl} to copy relevant data from.
     */
    public ArtifactAssetImpl(RemoteAssetImpl asset) {
        this.setLink(asset.getLink());
        this.logicalPath = asset.getLogicalPath();
        this.type = asset.getType();
        this.overwrite = asset.getOverwrite();
        this.metadata = asset.getMetadata();
    }


    @Override
    public Optional<Long> getSize() {
        return Optional.ofNullable(this.fileSize);
    }

    @Override
    public boolean isRemote() {
        return this.link != null;
    }

    @Override
    public URI getLink() {
        return this.link;
    }
}
