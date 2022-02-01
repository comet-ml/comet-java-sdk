package ml.comet.experiment;

import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.ModelDownloadInfo;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelRegistryRecord;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
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
     * @return the {@link ModelRegistryRecord} instance holding information about model registry record.
     */
    ModelRegistryRecord registerModel(Model model, String experimentKey);

    /**
     * Downloads and saves all files from the registered model.
     *
     * @param outputPath   the output directory to save files.
     * @param registryName the name of the model as in models' registry.
     * @param workspace    the name of the workspace.
     * @param options      the {@link DownloadModelOptions} defining additional download options.
     * @return the {@link ModelDownloadInfo} with download details.
     * @throws IOException if an I/O exception occurs while saving model files.
     */
    ModelDownloadInfo downloadRegistryModel(Path outputPath, String registryName, String workspace,
                                            DownloadModelOptions options) throws IOException;

    ModelDownloadInfo downloadRegistryModel(Path outputPath, String registryName, String workspace) throws IOException;
}
