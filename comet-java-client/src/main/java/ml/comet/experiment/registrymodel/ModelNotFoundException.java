package ml.comet.experiment.registrymodel;

import ml.comet.experiment.exception.CometApiException;

/**
 * Raised when experiment model is not found.
 */
public class ModelNotFoundException extends CometApiException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ModelNotFoundException(String message) {
        super(message);
    }
}
