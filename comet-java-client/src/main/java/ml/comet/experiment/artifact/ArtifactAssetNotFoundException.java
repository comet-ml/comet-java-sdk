package ml.comet.experiment.artifact;

/**
 * Exception to be raised when specific asset of the Comet artifact was not found.
 */
public class ArtifactAssetNotFoundException extends ArtifactException {
    /**
     * Constructs a new runtime exception with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public ArtifactAssetNotFoundException(String message) {
        super(message);
    }
}
