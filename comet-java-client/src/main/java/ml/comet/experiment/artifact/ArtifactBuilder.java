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
     * Allows specifying version for the artifact. The version string should follow the semantic versioning rules
     * and be in the form: {@code 1.2.3-beta.4+sha899d8g79f87}
     * <ul>
     *     <li>{@code 1} is the major part (required)</li>
     *     <li>{@code 2} is the minor part (required)</li>
     *     <li>{@code 3} is the patch part (required)</li>
     *     <li>{@code beta} and {@code 4} are the version suffixes (optional)</li>
     *     <li>{@code sha899d8g79f87} is the build information (optional)</li>
     * </ul>
     *
     * @param version the version string to be associated with artifact.
     * @return the builder instance.
     */
    ArtifactBuilder withVersion(String version);

    /**
     * Allows specifying list of TAG strings to be associated with artifact version.
     *
     * @param tags the list of TAG strings.
     * @return the builder instance.
     */
    ArtifactBuilder withVersionTags(List<String> tags);

    /**
     * Creates properly initialized {@link Artifact} instance.
     *
     * @return the properly initialized {@link Artifact} instance.
     */
    Artifact build();
}
