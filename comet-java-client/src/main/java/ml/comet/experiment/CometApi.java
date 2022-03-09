package ml.comet.experiment;

import ml.comet.experiment.model.ExperimentMetadata;
import ml.comet.experiment.model.Project;
import ml.comet.experiment.registrymodel.DownloadModelOptions;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelDownloadInfo;
import ml.comet.experiment.registrymodel.ModelNotFoundException;
import ml.comet.experiment.registrymodel.ModelOverview;
import ml.comet.experiment.registrymodel.ModelRegistryRecord;
import ml.comet.experiment.registrymodel.ModelVersionOverview;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

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

    /**
     * Allows querying for details about specific registry model.
     *
     * @param registryName the name of the model as in models' registry.
     * @param workspace    the name of the model's workspace.
     * @return the optional {@link ModelOverview} with model version details.
     */
    Optional<ModelOverview> getRegistryModelDetails(String registryName, String workspace);

    /**
     * Allows querying for details about specific version of the registry model.
     *
     * @param registryName the name of the model as in models' registry.
     * @param workspace    the name of the model's workspace.
     * @param version      the version of the registry model to be returned.
     * @return the optional {@link ModelOverview} with model version details.
     */
    Optional<ModelVersionOverview> getRegistryModelVersion(String registryName, String workspace, String version);

    /**
     * Returns list of the registry model names registered in specified workspace.
     *
     * @param workspace the name of the workspace.
     * @return the list of the registry model names registered in specified workspace.
     */
    List<String> getRegistryModelNames(String workspace);

    /**
     * Returns list of the version strings of the registry model.
     *
     * @param registryName the name of the model as in models' registry.
     * @param workspace    the name of the model's workspace.
     * @return the list of the version strings of the registry model.
     */
    List<String> getRegistryModelVersions(String registryName, String workspace);

    /**
     * Adds or updates notes associated with the registry model.
     *
     * @param notes        the notes to be associated.
     * @param registryName the name of the model as in models' registry.
     * @param workspace    the name of the model's workspace.
     */
    void updateRegistryModelNotes(String notes, String registryName, String workspace);

    /**
     * Returns notes associated with the registry model.
     *
     * @param registryName the name of the model as in models' registry.
     * @param workspace    the name of the model's workspace.
     * @return the notes associated with the registry model or empty.
     */
    Optional<String> getRegistryModelNotes(String registryName, String workspace);

    /**
     * Updates the registry model with new values.
     *
     * @param registryName    the current name of the model.
     * @param workspace       the name of the model's workspace.
     * @param newRegistryName the new name for the model.
     * @param newDescription  the new description of the model.
     * @param isPublic        the new visibility status of the model.
     * @throws ModelNotFoundException if model doesn't exists.
     */
    void updateRegistryModel(String registryName, String workspace,
                             String newRegistryName, String newDescription, boolean isPublic)
            throws ModelNotFoundException;

    void updateRegistryModel(String registryName, String workspace, String newRegistryName, String newDescription)
            throws ModelNotFoundException;

    void updateRegistryModel(String registryName, String workspace, String newRegistryName)
            throws ModelNotFoundException;

    /**
     * Updates the comments and stages of particular version of the registered model.
     *
     * @param registryName the name of the model.
     * @param workspace    the name of the model's workspace.
     * @param version      the version of the registered model to be updated.
     * @param comments     the comment to associate with new version.
     * @param stages       the stages to associate with new version.
     */
    void updateRegistryModelVersion(String registryName, String workspace, String version,
                                    String comments, List<String> stages);

    void updateRegistryModelVersion(String registryName, String workspace, String version, String comments);

    /**
     * Deletes registered model with given name.
     *
     * @param registryName the name of the model.
     * @param workspace    the name of the model's workspace.
     */
    void deleteRegistryModel(String registryName, String workspace);

    /**
     * Deletes specific version of the registered model with given name.
     *
     * @param registryName the name of the model.
     * @param workspace    the name of the model's workspace.
     * @param version      the version of the registered model to be deleted.
     */
    void deleteRegistryModelVersion(String registryName, String workspace, String version);
}
