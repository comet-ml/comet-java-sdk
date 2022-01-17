package ml.comet.experiment.impl.asset;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ml.comet.experiment.asset.RemoteAsset;

import java.net.URI;
import java.util.Optional;

/**
 * Describes remote asset data.
 */
@EqualsAndHashCode(callSuper = true)
public class RemoteAssetImpl extends AssetImpl implements RemoteAsset {
    @Getter
    @Setter
    private URI uri;

    @Override
    public Optional<URI> getLink() {
        return Optional.ofNullable(this.uri);
    }
}
