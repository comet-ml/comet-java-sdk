package ml.comet.experiment.impl.http;

/**
 * A simple interface an application can implement to received byte transfer information.
 */
public interface UploadListener extends TransferCompletedListener {
    /**
     * Invoked every time request's chunk are sent.
     *
     * @param amount  The amount of bytes to transfer
     * @param current The amount of bytes transferred
     * @param total   The total number of bytes transferred
     */
    void onBytesSent(long amount, long current, long total);
}
