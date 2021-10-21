package ml.comet.experiment.builder;

import ml.comet.experiment.OnlineExperiment;
import org.slf4j.Logger;

import java.io.File;

public interface OnlineExperimentBuilder {
    /**
     * Set project name for the experiment
     * @param projectName The project under which the experiment should run
     * @return
     */
    OnlineExperimentBuilder withProjectName(String projectName);

    /**
     * Set workspace for the project
     * @param workspace
     * @return The workspace under which the experiment should be run
     */
    OnlineExperimentBuilder withWorkspace(String workspace);

    /**
     * Set the api key matched to the account you wish to use
     * @param apiKey The api key for the user running the experiment
     * @return
     */
    OnlineExperimentBuilder withApiKey(String apiKey);

    /**
     * Set the max auth retry attempts.
     * @param maxAuthRetries number of times to try auth calls on experiment creation
     * @return
     */
    OnlineExperimentBuilder withMaxAuthRetries(int maxAuthRetries);

    /**
     * Set the URL of your comet installation.
     * @param urlOverride full url of comet installation. default is https://www.comet.ml
     * @return
     */
    OnlineExperimentBuilder  withUrlOverride(String urlOverride);

    /**
     * Name the experiment
     * @param experimentName name to be applied to the experiment
     * @return
     */
    OnlineExperimentBuilder withExperimentName(String experimentName);

    /**
     * Used to continue a previous experiment
     * @param experimentKey The experiment key for the experiment that is being continued
     * @return
     */
    OnlineExperimentBuilder withExistingExperimentKey(String experimentKey);

    /**
     * slf4j Logger through which the Experiment object can log its actions
     * @param logger The logger that Experiment should use
     * @return
     */
    OnlineExperimentBuilder withLogger(Logger logger);

    /**
     * Provide an override config to changeExperiment parameters being provided by the config
     * @param overrideConfig Config to override default values
     * @return
     */
    OnlineExperimentBuilder withConfig(File overrideConfig);

    /**
     * Turn on intercept of stdout and stderr and the logging of both in Comet
     * @return
     */
    OnlineExperimentBuilder interceptStdout();

    /**
     * Instantiates the Experiment object and registers it with Comet.  At this point the experiment
     * will show up on Comet
     * @return
     */
    OnlineExperiment build();
}
