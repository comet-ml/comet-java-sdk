package ml.comet.experiment.impl;

import lombok.Getter;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import ml.comet.experiment.impl.utils.DataModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the {@link LoggedArtifactAsset}.
 */
public final class LoggedArtifactAssetImpl implements LoggedArtifactAsset {

    private final Logger logger = LoggerFactory.getLogger(LoggedArtifactAsset.class);

    @Getter
    boolean remote;
    @Getter
    String assetType;
    @Getter
    String assetId;

    private Map<String, Object> metadata;
    String metadataJson;
    String fileName;
    Long fileSize;
    String remoteUri;

    private final LoggedArtifactImpl artifact;

    LoggedArtifactAssetImpl(LoggedArtifactImpl artifact) {
        this.artifact = artifact;
    }

    @Override
    public String getArtifactId() {
        return this.artifact.getArtifactId();
    }

    @Override
    public String getArtifactVersionId() {
        return this.artifact.getVersionId();
    }

    @Override
    public Optional<String> getFileName() {
        if (StringUtils.isNotBlank(this.fileName)) {
            return Optional.of(this.fileName);
        }
        return Optional.empty();
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
}
