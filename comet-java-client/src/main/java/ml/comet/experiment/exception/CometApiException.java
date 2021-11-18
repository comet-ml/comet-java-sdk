package ml.comet.experiment.exception;

/**
 * Signals that REST API call operation has been failed or returned unexpected result.
 */
public class CometApiException extends CometGeneralException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public CometApiException(String message) {
        super(message);
    }
}
