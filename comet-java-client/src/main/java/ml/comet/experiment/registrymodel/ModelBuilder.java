package ml.comet.experiment.registrymodel;

import java.util.List;

/**
 * The {@link ModelBuilder} can be used to create properly initialized instance of the {@link Model}.
 */
public interface ModelBuilder {

    /**
     * Updates the version of the model. The version string should follow the semantic versioning rules
     * and be in the form: {@code 1.2.3-beta.4+sha899d8g79f87}
     * <ul>
     *    <li>{@code 1} is the major part (required)</li>
     *     <li>{@code 2} is the minor part (required)</li>
     *     <li>{@code 3} is the patch part (required)</li>
     *     <li>{@code beta} and {@code 4} are the version suffixes (optional)</li>
     *     <li>{@code sha899d8g79f87} is the build information (optional)</li>
     * </ul>
     *
     * @param version the version of the model to be applied.
     * @return the {@link ModelBuilder} instance.
     */
    ModelBuilder withVersion(String version);

    /**
     * Updates the workspace name of the model.
     *
     * @param workspace the name of the workspace.
     * @return the {@link ModelBuilder} instance.
     */
    ModelBuilder withWorkspace(String workspace);

    /**
     * Updates the name of the model as defined in the Comet registry.
     *
     * @param registryModelName the name of the mode in the Comet registry
     * @return the {@link ModelBuilder} instance.
     */
    ModelBuilder withRegistryName(String registryModelName);

    /**
     * Updates the visibility of this model.
     *
     * @param isPublic if {@code true} the model registry will be listed as public.
     * @return the {@link ModelBuilder} instance.
     */
    ModelBuilder asPublic(boolean isPublic);

    /**
     * Updates the long description associated with this model's record.
     *
     * @param description the long description of the model.
     * @return the {@link ModelBuilder} instance.
     */
    ModelBuilder withDescription(String description);

    /**
     * Updates the short comment associated with model's record.
     *
     * @param comment the short comment about the model.
     * @return the {@link ModelBuilder} instance.
     */
    ModelBuilder withComment(String comment);

    /**
     * Updates the TAGs associated with the model.
     *
     * @param tags the list of TAGs to associate with the model.
     * @return the {@link ModelBuilder} instance.
     */
    ModelBuilder withStages(List<String> tags);

    /**
     * Creates properly initialized {@link Model} instance using defined by this builder configuration options.
     *
     * @return the properly initialized {@link Model} instance.
     */
    Model build();
}
