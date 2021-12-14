package ml.comet.experiment.artifact;

import ml.comet.experiment.exception.CometGeneralException;

/**
 * Exception to be raised if {@link Artifact} is not ready yet and still being processed by the Comet backend.
 */
public class ArtifactNotReadyException extends CometGeneralException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ArtifactNotReadyException(String message) {
        super(message);
    }
}
