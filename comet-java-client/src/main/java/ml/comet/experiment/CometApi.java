package ml.comet.experiment;

import ml.comet.experiment.model.ExperimentMetadataRest;
import ml.comet.experiment.model.RestProject;

import java.io.Closeable;
import java.util.List;

/**
 * The utility providing direct access to the Comet REST API.
 *
 * Make sure to call CometApi.close() after finished with usage to release underlying resources.
 */
public interface CometApi extends Closeable {

    /**
     * Gets all workspaces available for current API key
     *
     * @return List of workspace names
     */
    List<String> getAllWorkspaces();

    /**
     * Gets all project dtos under specified workspace name
     *
     * @param workspaceName workspace name
     * @return List of project dtos
     */
    List<RestProject> getAllProjects(String workspaceName);

    /**
     * Gets all experiment dto under specified project id
     *
     * @param projectId Project id
     * @return List of experiment dtos
     */
    List<ExperimentMetadataRest> getAllExperiments(String projectId);
}
