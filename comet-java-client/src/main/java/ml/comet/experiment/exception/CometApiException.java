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

    /**
     * Constructs a new runtime exception with detail message that can be build using provided
     * format string and arguments.
     *
     * @param format the format string, see {@link String#format(String, Object...)} for more details.
     * @param args   the arguments.
     */
    public CometApiException(String format, Object... args) {
        super(String.format(format, args));
    }
}
