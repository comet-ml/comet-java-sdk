package ml.comet.experiment;

import lombok.NonNull;
import ml.comet.experiment.builder.ApiExperimentBuilder;
import ml.comet.experiment.impl.ApiExperimentImpl;

/**
 * This is stub to support backward compatibility.
 *
 * @deprecated It would be replaced in the future with new experiment creation API.
 */
public final class ApiExperiment {
    /**
     * Returns builder to create {@link Experiment} instance.
     *
     * @param experimentKey the unique identifier of the existing experiment.
     * @return the initialized ApiExperiment instance.
     */
    public static ApiExperimentBuilder builder(@NonNull final String experimentKey) {
        return ApiExperimentImpl.builder(experimentKey);
    }
}
