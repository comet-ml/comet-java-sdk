package ml.comet.experiment;

import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelRegistry;

import java.io.Closeable;
import java.util.List;

/**
 * The utility providing direct access to the Comet REST API.
 *
 * <p>Make sure to call CometApi.close() after finished with usage to release underlying resources.</p>
 */
public interface CometApi extends Closeable {

    /**
     * Gets all workspaces available for current API key.
     *
     * @return the list of workspace names
     */
    List<String> getAllWorkspaces();

    /**
     * Gets all projects under specified workspace name.
     *
     * @param workspaceName workspace name
     * @return the list of projects
     */
    List<Project> getAllProjects(String workspaceName);

    /**
     * Gets metadata of all experiments created under specified project id.
     *
     * @param projectId the ID of the project.
     * @return the list of experiments' metadata objects.
     */
    List<ExperimentMetadata> getAllExperiments(String projectId);

    /**
     * Register model defined in the specified experiment in the Comet's model registry.
     *
     * @param model         the {@link Model} to be registered.
     * @param experimentKey the identifier of the experiment where model assets was logged.
     * @return the {@link ModelRegistry} instance holding information about model registry record.
     */
    ModelRegistry registerModel(Model model, String experimentKey);
}
