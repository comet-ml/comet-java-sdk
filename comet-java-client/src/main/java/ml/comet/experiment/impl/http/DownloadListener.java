package ml.comet.experiment.impl.http;

import java.io.IOException;

/**
 * A simple interface an application can implement to received bytes download information.
 */
public interface DownloadListener extends TransferCompletedListener {

    /**
     * Invoked every time response's chunk are received.
     *
     * @param bytes the array with received data bytes.
     * @throws IOException if an I/O exception occurred while writing to the file.
     */
    void onBytesReceived(byte[] bytes) throws IOException;
}
