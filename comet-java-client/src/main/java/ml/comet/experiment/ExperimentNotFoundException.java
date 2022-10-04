package ml.comet.experiment;

import ml.comet.experiment.exception.CometApiException;

/**
 * Exception to be thrown when Comet experiment was not found.
 */
public class ExperimentNotFoundException extends CometApiException {

    /**
     * Constructs a new ExperimentNotFoundException exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ExperimentNotFoundException(String message) {
        super(message);
    }
}
