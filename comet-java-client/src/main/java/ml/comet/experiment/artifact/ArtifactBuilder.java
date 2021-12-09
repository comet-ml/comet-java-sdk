package ml.comet.experiment.artifact;

import java.util.List;
import java.util.Map;

/**
 * The factory to properly create initialized {@link Artifact} instances.
 */
public interface ArtifactBuilder {

    /**
     * Allows to attach the provided list of aliases to the {@link Artifact}.
     *
     * @param aliases the list of aliases.
     * @return the builder instance.
     */
    ArtifactBuilder withAliases(List<String> aliases);

    /**
     * Allows linking the metadata with the {@link Artifact} instance.
     *
     * @param metadata the metadata to link with artifact.
     * @return the builder instance.
     */
    ArtifactBuilder withMetadata(Map<String, Object> metadata);

    /**
     * Creates properly initialized {@link Artifact} instance.
     *
     * @return the properly initialized {@link Artifact} instance.
     */
    Artifact build();
}
