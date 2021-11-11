package ml.comet.experiment.builder;

import org.slf4j.Logger;

import java.io.File;

/**
 * Defines public contract of base interface for all experiment builders in the Comet Java SDK.
 */
public interface BaseCometBuilder<T> {
    /**
     * Set the api key matched to the account you wish to use.
     *
     * @param apiKey The api key for the user running the experiment
     * @return the builder configured with specified API key.
     */
    BaseCometBuilder<T> withApiKey(String apiKey);

    /**
     * Provide an override config to changeExperiment parameters being provided by the config.
     *
     * @param overrideConfig Config to override default values
     * @return the builder configured with specified configuration file to override default configuration options.
     */
    BaseCometBuilder<T> withConfigOverride(File overrideConfig);

    /**
     * The slf4j Logger through which the instance can log its actions.
     *
     * @param logger The logger that instance should use.
     * @return the builder configured with specified logger to be used for logging.
     */
    BaseCometBuilder<T> withLogger(Logger logger);

    /**
     * Instantiates the Comet experiment instance defined by {@code T}.
     *
     * @return the properly configured instance of the Comet experiment.
     */
    T build();
}
