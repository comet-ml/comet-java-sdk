package ml.comet.experiment.impl.http;

/**
 * The contract of interface an application can implement to receive notifications about transfer completion.
 */
public interface TransferCompletedListener {

    /**
     * Invoked when the response bytes has been fully processed.
     */
    void onRequestResponseCompleted();

    /**
     * Invoked when there is an unexpected issue.
     *
     * @param t a {@link Throwable}
     */
    void onThrowable(Throwable t);
}
