package ml.comet.experiment.builder;

import ml.comet.experiment.OnlineExperiment;

/**
 * Defines the public contract of the {@link OnlineExperiment} builder. This factory is preferred method to create
 * properly initialized instance of the {@link OnlineExperiment}.
 */
public interface OnlineExperimentBuilder extends BaseCometBuilder<OnlineExperiment> {
    /**
     * Set project name for the experiment.
     *
     * @param projectName The project under which the experiment should run
     * @return the builder configured with specified project name.
     */
    OnlineExperimentBuilder withProjectName(String projectName);

    /**
     * Set workspace for the project.
     *
     * @param workspace The workspace under which the experiment should be run.
     * @return the builder configured with specified workspace name.
     */
    OnlineExperimentBuilder withWorkspace(String workspace);

    /**
     * Set the max auth retry attempts.
     *
     * @param maxAuthRetries number of times to try auth calls on experiment creation
     * @return the builder configured with specified maximal number of authenticate retries.
     */
    OnlineExperimentBuilder withMaxAuthRetries(int maxAuthRetries);

    /**
     * Set the URL of your comet installation.
     *
     * @param urlOverride full url of comet installation. Default is https://www.comet.ml
     * @return the builder configured with specified URL of the Comet installation.
     */
    OnlineExperimentBuilder withUrlOverride(String urlOverride);

    /**
     * Sets the name the experiment.
     *
     * @param experimentName name to be applied to the experiment
     * @return the builder configured with specified experiment name.
     */
    OnlineExperimentBuilder withExperimentName(String experimentName);

    /**
     * Allows to continue a previous experiment by providing the key of the existing experiment.
     *
     * @param experimentKey The experiment key for the experiment that is being continued
     * @return the builder configured with specified key of the previous experiment.
     */
    OnlineExperimentBuilder withExistingExperimentKey(String experimentKey);

    /**
     * Turn on intercept of stdout and stderr and the logging of both in Comet.
     *
     * @return the builder configured with flag indicating that stdout and stderr stream should be intercepted.
     */
    OnlineExperimentBuilder interceptStdout();
}
