package ml.comet.experiment;

import org.slf4j.Logger;

import java.io.File;

interface ExperimentBuilder {
    /**
     * Set project name for the experiment
     * @param projectName The project under which the experiment should run
     * @return
     */
    ExperimentBuilder withProjectName(String projectName);

    /**
     * Set workspace for the project
     * @param workspace
     * @return The workspace under which the experiment should be run
     */
    ExperimentBuilder withWorkspace(String workspace);

    /**
     * Set the api key matched to the account you wish to use
     * @param apiKey The api key for the user running the experiment
     * @return
     */
    ExperimentBuilder withApiKey(String apiKey);

    /**
     * Name the experiment
     * @param experimentName name to be applied to the experiment
     * @return
     */
    ExperimentBuilder withExperimentName(String experimentName);

    /**
     * Used to continue a previous experiment
     * @param experimentKey The experiment key for the experiment that is being continued
     * @return
     */
    ExperimentBuilder withExistingExperimentKey(String experimentKey);

    /**
     * slf4j Logger through which the Experiment object can log its actions
     * @param logger The logger that Experiment should use
     * @return
     */
    ExperimentBuilder withLogger(Logger logger);

    /**
     * Provide an override config to changeExperiment parameters being provided by the config
     * @param overrideConfig Config to override default values
     * @return
     */
    ExperimentBuilder withConfig(File overrideConfig);

    /**
     * Turn on intercept of stdout and stderr and the logging of both in Comet
     * @return
     */
    ExperimentBuilder interceptStdout();

    /**
     * Instantiates the Experiment object and registers it with Comet.  At this point the experiment
     * will show up on Comet
     * @return
     */
    Experiment build();
}
