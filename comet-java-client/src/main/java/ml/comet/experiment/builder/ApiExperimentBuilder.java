package ml.comet.experiment.builder;

import ml.comet.experiment.ApiExperiment;

/**
 * Defines the public contract of the factory to create initialized instances of the {@link ApiExperiment} allowing
 * to work with Comet API synchronously.
 */
public interface ApiExperimentBuilder extends BaseCometBuilder<ApiExperiment> {
    /**
     * Allows to continue a previous experiment by providing the key of the existing experiment.
     *
     * @param experimentKey The experiment key for the experiment that is being continued
     * @return the builder configured with specified key of the previous experiment.
     */
    ApiExperimentBuilder withExistingExperimentKey(String experimentKey);

    /**
     * Set project name for the experiment.
     *
     * @param projectName The project under which the experiment should run
     * @return the builder configured with specified project name.
     */
    ApiExperimentBuilder withProjectName(String projectName);

    /**
     * Set workspace for the project.
     *
     * @param workspace The workspace under which the experiment should be run.
     * @return the builder configured with specified workspace name.
     */
    ApiExperimentBuilder withWorkspace(String workspace);
}
