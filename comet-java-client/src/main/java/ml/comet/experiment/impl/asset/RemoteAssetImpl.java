package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ml.comet.experiment.asset.RemoteAsset;

import java.net.URI;
import java.util.Optional;

/**
 * Describes remote asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RemoteAssetImpl extends AssetImpl implements RemoteAsset {
    private URI uri;

    @Override
    public Optional<URI> getLink() {
        return Optional.ofNullable(this.uri);
    }
}
