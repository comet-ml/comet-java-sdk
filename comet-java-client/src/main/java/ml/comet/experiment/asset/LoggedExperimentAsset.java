package ml.comet.experiment.asset;

import java.time.Instant;
import java.util.Optional;

/**
 * Defines the public contract of the asset associated with particular experiment which already logged to the Comet.
 */
public interface LoggedExperimentAsset extends RemoteAsset {
    /**
     * Returns ID of the asset.
     *
     * @return the ID of the asset.
     */
    String getAssetId();

    /**
     * Returns {@code true} if this is remote asset, i.e., providing {@code URI} to the remote location to download
     * content.
     *
     * @return {@code true} if this is remote asset.
     */
    boolean isRemote();

    /**
     * Returns the optional size of this asset if appropriate.
     *
     * @return the optional size of this asset if appropriate.
     */
    Optional<Long> getSize();

    /**
     * Returns optional {@link Instant} when this asset was logged to the Comet.
     *
     * @return the optional {@link Instant} when this asset was logged to the Comet.
     */
    Optional<Instant> getCreatedAt();

    /**
     * Returns optional command which can be used in terminal app to download this asset.
     *
     * @return the optional command which can be used in terminal app to download this asset.
     */
    Optional<String> getCurlDownload();

}
