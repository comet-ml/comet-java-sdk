package ml.comet.experiment.asset;

import java.net.URI;
import java.util.Optional;

/**
 * Defines public contract of the experiment asset which points to the remote resource.
 */
public interface RemoteAsset extends Asset {

    /**
     * Returns the optional link to the remote resource associated with this asset.
     *
     * @return the optional link to the remote resource associated with this asset.
     */
    Optional<URI> getLink();
}
