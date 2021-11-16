package ml.comet.experiment.config;

import ml.comet.experiment.exception.CometGeneralException;

/**
 * The exception to be thrown if any configuration issues found.
 */
public class ConfigException extends CometGeneralException {

    /**
     * Constructs a new runtime exception with the specified detail message.
     *
     * @param message the detail message.
     */
    ConfigException(String message) {
        super(message);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and
     * cause.
     *
     * @param message the detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   the cause (which is saved for later retrieval by the
     *                {@link #getCause()} method).  (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or
     *                unknown.)
     */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
