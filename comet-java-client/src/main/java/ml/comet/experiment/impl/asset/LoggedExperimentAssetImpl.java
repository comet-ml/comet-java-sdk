package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import ml.comet.experiment.asset.LoggedExperimentAsset;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * Implementation of the {@link LoggedExperimentAsset}.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LoggedExperimentAssetImpl extends RemoteAssetImpl implements LoggedExperimentAsset {
    private String assetId;
    private boolean remote;
    private Instant createdAt;
    private Long fileSize;
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
}
