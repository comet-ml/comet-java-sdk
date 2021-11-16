package ml.comet.experiment.builder;

import ml.comet.experiment.Experiment;

/**
 * Defines the public contract of the factory to create initialized instances of the {@link Experiment} allowing
 * to work with Comet API synchronously.
 */
public interface ApiExperimentBuilder extends BaseCometBuilder<Experiment> {

}
