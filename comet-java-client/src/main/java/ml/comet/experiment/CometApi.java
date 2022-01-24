package ml.comet.experiment;

import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.Model;

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
     * @return List of workspace names
     */
    List<String> getAllWorkspaces();

    /**
     * Gets all project DTOs under specified workspace name.
     *
     * @param workspaceName workspace name
     * @return List of project DTOs
     */
    List<Project> getAllProjects(String workspaceName);

    /**
     * Gets all experiment DTOs under specified project id.
     *
     * @param projectId Project id
     * @return List of experiment DTOs
     */
    List<ExperimentMetadata> getAllExperiments(String projectId);

    /**
     * Register model defined in the specified experiment in the Comet's model registry.
     *
     * @param model         the {@link Model} to be registered.
     * @param experimentKey the identifier of the experiment where model assets was logged.
     */
    void registerModel(Model model, String experimentKey);
}
