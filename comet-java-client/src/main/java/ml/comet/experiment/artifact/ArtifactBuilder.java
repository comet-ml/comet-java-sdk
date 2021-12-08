package ml.comet.experiment.artifact;

import java.util.Map;
import java.util.Set;

/**
 * The factory to properly create initialized {@link Artifact} instances.
 */
public interface ArtifactBuilder {

    /**
     * Allows providing set of aliases to attach to the {@link Artifact}.
     *
     * @param aliases the set of aliases.
     * @return the builder instance.
     */
    ArtifactBuilder withAliases(Set<String> aliases);

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
