package ml.comet.experiment;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.builder.ApiExperimentBuilder;

/**
 * This is stub to support backward compatibility.
 *
 * @deprecated It would be replaced in the future with new experiment creation API.
 */
@UtilityClass
public final class ApiExperimentImpl {
    /**
     * Returns builder to create {@link Experiment} instance.
     *
     * @param experimentKey the unique identifier of the existing experiment.
     * @return the initialized ApiExperiment instance.
     */
    public static ApiExperimentBuilder builder(@NonNull final String experimentKey) {
        return ml.comet.experiment.impl.ApiExperimentImpl.builder(experimentKey);
    }
}
