package ml.comet.experiment.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.utils.DataModelUtils;
import ml.comet.experiment.model.FileAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the {@link LoggedArtifactAsset}.
 */
@ToString
public final class LoggedArtifactAssetImpl implements LoggedArtifactAsset {

    private final Logger logger = LoggerFactory.getLogger(LoggedArtifactAsset.class);

    @Getter
    @Setter
    private boolean remote;
    @Getter
    @Setter
    private String assetType;
    @Getter
    @Setter
    private String assetId;
    @Setter
    private String metadataJson;
    @Setter
    @Getter
    private String fileName;
    @Setter
    private Long fileSize;
    @Setter
    private String remoteUri;
    @Setter
    private String artifactId;
    @Setter
    private String artifactVersionId;

    private Map<String, Object> metadata;
    private final LoggedArtifactImpl artifact;

    LoggedArtifactAssetImpl(LoggedArtifactImpl artifact) {
        this.artifact = artifact;
    }

    @Override
    public String getArtifactId() {
        return this.artifactId;
    }

    @Override
    public String getArtifactVersionId() {
        return this.artifactVersionId;
    }

    @Override
    public Optional<Long> getFileSize() {
        if (this.fileSize != null) {
            return Optional.of(this.fileSize);
        }
        return Optional.empty();
    }

    @Override
    public Optional<URI> getLink() {
        if (StringUtils.isNotBlank(this.remoteUri)) {
            try {
                return Optional.of(new URI(this.remoteUri));
            } catch (URISyntaxException e) {
                this.logger.error("Failed to parse remote asset URI: {}", this.remoteUri, e);
            }
        }
        return Optional.empty();
    }

    @Override
    public Map<String, Object> getMetadata() {
        if (this.metadata != null) {
            return this.metadata;
        }
        if (StringUtils.isNotBlank(this.metadataJson)) {
            try {
                this.metadata = DataModelUtils.metadataFromJson(this.metadataJson);
                return this.metadata;
            } catch (Throwable e) {
                this.logger.error("Failed to parse artifact asset metadata from JSON {}", this.metadataJson, e);
            }
        }
        return Collections.emptyMap();
    }

    @Override
    public FileAsset download(Path dir) throws ArtifactException {
        return this.download(dir, AssetOverwriteStrategy.FAIL);
    }

    @Override
    public FileAsset download(Path dir, AssetOverwriteStrategy overwriteStrategy) throws ArtifactException {
        return this.download(dir, FileSystems.getDefault().getPath(this.fileName), overwriteStrategy);
    }

    @Override
    public FileAsset download(@NonNull Path dir, @NonNull Path file, @NonNull AssetOverwriteStrategy overwriteStrategy)
            throws ArtifactException {
        if (this.isRemote()) {
            throw new ArtifactException("Can not download remote asset. Please use its URI to download directly!");
        }
        return this.artifact.downloadAsset(this, dir, file, overwriteStrategy);
    }

    @Override
    public ByteBuffer load() throws ArtifactException {
        return this.artifact.load(this);
    }
}
