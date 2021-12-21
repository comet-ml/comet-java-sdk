package ml.comet.experiment.exception;

import lombok.Getter;

/**
 * Signals that REST API call operation has been failed or returned unexpected result.
 */
public class CometApiException extends CometGeneralException {
    @Getter
    private int sdkErrorCode;
    @Getter
    private int statusCode;
    @Getter
    private String statusMessage;

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
     * Constructs a new {@link CometApiException} with information about error returned by remote enpoint.
     *
     * @param statusCode    the HTTP status code.
     * @param statusMessage the HTTP status message.
     * @param sdkErrorCode  the Comet SDK error code related to this error.
     */
    public CometApiException(int statusCode, String statusMessage, int sdkErrorCode) {
        super(String.format("Remote endpoint returned error status code: %d, message: %s, sdk error code: %d",
                statusCode, statusMessage, sdkErrorCode));
        this.sdkErrorCode = sdkErrorCode;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     *
     * <p>Note that the detail message associated with {@code cause} is <i>not</i> automatically incorporated in
     * this runtime exception's detail message.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    public CometApiException(String message, Throwable cause) {
        super(message, cause);
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

    /**
     * Allows checking if this exception has error code associated.
     *
     * @return {@code true} if this exception has Comet SDK error code associated.
     */
    public boolean hasErrorCode() {
        return this.sdkErrorCode > 0;
    }
}
