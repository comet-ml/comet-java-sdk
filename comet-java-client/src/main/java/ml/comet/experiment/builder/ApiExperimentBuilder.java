package ml.comet.experiment.builder;

import ml.comet.experiment.ApiExperiment;

/**
 * Defines the public contract of the factory to create initialized instances of the {@link ApiExperiment} allowing
 * to work with Comet API synchronously.
 */
public interface ApiExperimentBuilder extends BaseCometBuilder<ApiExperiment> {

}
