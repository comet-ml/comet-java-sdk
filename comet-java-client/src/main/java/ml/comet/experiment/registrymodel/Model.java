package ml.comet.experiment.registrymodel;

import ml.comet.experiment.impl.RegistryModelImpl;

/**
 * Defines public contract of the registered ML model.
 */
public interface Model {
    /**
     * Returns the name of the experiment model.
     *
     * @return the name of the experiment model
     */
    String getName();

    /**
     * Returns the version of the model.
     *
     * @return the version of the model.
     */
    String getVersion();

    /**
     * Returns the name of the workspace where model is registered.
     *
     * @return the name of the workspace where model is registered.
     */
    String getWorkspace();

    /**
     * Returns the registry record name for this model in the workspace.
     *
     * @return the registry record name for this model in the workspace.
     */
    String getRegistryName();

    /**
     * Returns {@code true} if model registration is in public domain.
     *
     * @return the {@code true} if model registration is in public domain.
     */
    boolean isPublic();

    /**
     * Creates new model for the registry with given name and version.
     *
     * @param name the name of the experiment model.
     * @return the {@link ModelBuilder} which can be used to properly configure and build the {@link Model} instance.
     */
    static ModelBuilder newModel(String name) {
        return new RegistryModelImpl.RegistryModelBuilderImpl(name);
    }
}
