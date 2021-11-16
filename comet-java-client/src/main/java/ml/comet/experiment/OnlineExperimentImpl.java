package ml.comet.experiment;

/**
 * This is stub to support backward compatibility.
 *
 * @deprecated It would be replaced in the future with new experiment creation API.
 */
public final class OnlineExperimentImpl {
    /**
     * Returns builder to be used to create properly configured instance of this class.
     *
     * @return the builder to be used to create properly configured instance of this class.
     */
    public static ml.comet.experiment.builder.OnlineExperimentBuilder builder() {
        return ml.comet.experiment.impl.OnlineExperimentImpl.builder();
    }
}
