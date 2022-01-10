package ml.comet.experiment.asset;

import java.net.URI;

/**
 * Defines public contract of the experiment asset which points to the remote resource.
 */
public interface RemoteAsset extends Asset {

    /**
     * Returns the link to the remote resource associated with this asset.
     *
     * @return the link to the remote resource associated with this asset.
     */
    URI getLink();
}
