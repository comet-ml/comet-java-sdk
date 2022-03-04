package ml.comet.experiment.registrymodel;

import ml.comet.experiment.exception.CometApiException;

/**
 * Exception to be thrown if specific version of the registered Comet model was not found.
 */
public class ModelVersionNotFoundException extends CometApiException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ModelVersionNotFoundException(String message) {
        super(message);
    }
}
