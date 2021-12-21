package ml.comet.experiment.impl.asset;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.net.URI;

/**
 * Describes remote asset data.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RemoteAssetImpl extends AssetImpl implements RemoteAsset {
    private URI link;
}
