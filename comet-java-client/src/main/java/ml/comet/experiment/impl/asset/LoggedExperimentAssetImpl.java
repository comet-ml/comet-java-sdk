package ml.comet.experiment.impl.asset;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.asset.LoggedExperimentAsset;

import java.time.Instant;
import java.util.Optional;

/**
 * Implementation of the {@link LoggedExperimentAsset}.
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public final class LoggedExperimentAssetImpl extends RemoteAssetImpl implements LoggedExperimentAsset {
    @Setter
    private String assetId;
    @Setter
    private boolean remote;
    @Setter
    private Instant createdAt;
    @Getter
    @Setter
    private Long fileSize;
    @Setter
    private String curlDownload;

    @Override
    public Optional<Long> getSize() {
        return Optional.ofNullable(this.fileSize);
    }

    @Override
    public boolean isRemote() {
        return this.remote;
    }

    @Override
    public Optional<Instant> getCreatedAt() {
        return Optional.ofNullable(this.createdAt);
    }

    @Override
    public Optional<String> getCurlDownload() {
        return Optional.ofNullable(this.curlDownload);
    }

    @Override
    public String getAssetId() {
        return this.assetId;
    }
}
