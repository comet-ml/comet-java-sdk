package ml.comet.experiment.builder;

import ml.comet.experiment.ApiExperiment;
import org.slf4j.Logger;

import java.io.File;

public interface ApiExperimentBuilder {
    /**
     * Set the api key matched to the account you wish to use
     * @param apiKey The api key for the user running the experiment
     * @return
     */
    ApiExperimentBuilder withApiKey(String apiKey);

    /**
     * slf4j Logger through which the Experiment object can log its actions
     * @param logger The logger that Experiment should use
     * @return
     */
    ApiExperimentBuilder withLogger(Logger logger);

    /**
     * Provide an override config to changeExperiment parameters being provided by the config
     * @param overrideConfig Config to override default values
     * @return
     */
    ApiExperimentBuilder withConfig(File overrideConfig);

    /**
     * Instantiates the ApiExperiment object and registers it with Comet.  At this point the experiment
     * will show up on Comet
     * @return
     */
    ApiExperiment build();
}