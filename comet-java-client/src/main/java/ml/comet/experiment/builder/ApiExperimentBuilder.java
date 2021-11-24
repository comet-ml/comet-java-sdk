package ml.comet.experiment.builder;

import ml.comet.experiment.Experiment;

/**
 * Defines the public contract of the factory to create initialized instances of the {@link Experiment} allowing
 * to work with Comet API synchronously.
 */
public interface ApiExperimentBuilder extends BaseCometBuilder<Experiment> {
    /**
     * Allows to continue a previous experiment by providing the key of the existing experiment.
     *
     * @param experimentKey The experiment key for the experiment that is being continued
     * @return the builder configured with specified key of the previous experiment.
     */
    ApiExperimentBuilder withExistingExperimentKey(String experimentKey);
}
