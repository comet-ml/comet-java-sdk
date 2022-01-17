package ml.comet.experiment.impl;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactAsset;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.AssetOverwriteStrategy;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.utils.DataModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static ml.comet.experiment.impl.resources.LogMessages.REMOTE_ASSET_CANNOT_BE_DOWNLOADED;
import static ml.comet.experiment.impl.resources.LogMessages.getString;

/**
 * Implementation of the {@link LoggedArtifactAsset}.
 */
@ToString(onlyExplicitlyIncluded = true)
public final class LoggedArtifactAssetImpl implements LoggedArtifactAsset {

    private final Logger logger = LoggerFactory.getLogger(LoggedArtifactAsset.class);

    @Getter
    @Setter
    @ToString.Include
    private boolean remote;
    @Getter
    @Setter
    @ToString.Include
    private String assetType;
    @Getter
    @Setter
    @ToString.Include
    private String assetId;
    @Setter
    private String metadataJson;
    @Setter
    @Getter
    @ToString.Include
    private String logicalPath;
    @Setter
    private Long fileSize;
    @Setter
    @ToString.Include
    private String remoteUri;
    @Setter
    private String artifactId;
    @Setter
    private String artifactVersionId;
    @Setter
    private Map<String, Object> metadata;

    final LoggedArtifactImpl artifact;

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
    public Optional<Long> getSize() {
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
    public ArtifactAsset download(Path dir) throws ArtifactException {
        return this.download(dir, AssetOverwriteStrategy.FAIL_IF_DIFFERENT);
    }

    @Override
    public ArtifactAsset download(Path dir, AssetOverwriteStrategy overwriteStrategy) throws ArtifactException {
        return this.download(dir, FileSystems.getDefault().getPath(this.logicalPath), overwriteStrategy);
    }

    @Override
    public ArtifactAsset download(@NonNull Path dir, @NonNull Path file,
                                  @NonNull AssetOverwriteStrategy overwriteStrategy) throws ArtifactException {
        this.validateNotRemote();
        return this.artifact.downloadAsset(this, dir, file, overwriteStrategy);
    }

    @Override
    public void writeTo(OutputStream out) throws ArtifactException {
        this.validateNotRemote();
        this.artifact.writeAssetTo(this, out);
    }

    @Override
    public InputStream openStream() throws ArtifactException {
        this.validateNotRemote();
        return this.artifact.openAssetStream(this);
    }

    @ToString.Include
    String artifactFullName() {
        return this.artifact.getFullName();
    }

    void validateNotRemote() {
        if (this.isRemote()) {
            throw new ArtifactException(getString(REMOTE_ASSET_CANNOT_BE_DOWNLOADED, this));
        }
    }
}
