package ml.comet.experiment;

import lombok.experimental.UtilityClass;

/**
 * This is stub to support backward compatibility.
 *
 * @deprecated It would be replaced in the future with new experiment creation API.
 */
@UtilityClass
public final class OnlineExperimentImpl {
    /**
     * Returns builder to be used to create properly configured instance of {@link OnlineExperiment}.
     *
     * @return the builder to be used to create properly configured instance of {@link OnlineExperiment}.
     */
    public static ml.comet.experiment.builder.OnlineExperimentBuilder builder() {
        return ml.comet.experiment.impl.OnlineExperimentImpl.builder();
    }
}
