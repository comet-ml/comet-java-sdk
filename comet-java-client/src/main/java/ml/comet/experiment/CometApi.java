package ml.comet.experiment;

import ml.comet.experiment.impl.model.ExperimentMetadataRest;
import ml.comet.experiment.impl.model.RestProject;

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
    List<RestProject> getAllProjects(String workspaceName);

    /**
     * Gets all experiment DTOs under specified project id.
     *
     * @param projectId Project id
     * @return List of experiment DTOs
     */
    List<ExperimentMetadataRest> getAllExperiments(String projectId);
}
