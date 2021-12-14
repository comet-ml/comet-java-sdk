package ml.comet.experiment.artifact;

import lombok.Getter;

/**
 * Holds options used to query for Comet artifact.
 */
@Getter
public final class GetArtifactOptions {
    private String workspace;
    private String project;
    private String artifactName;
    private String artifactId;
    private String versionId;
    private String version;
    private String alias;
    private String versionOrAlias;
    private String consumerExperimentKey;

    GetArtifactOptions() {
    }

    /**
     * Factory to create default empty options.
     *
     * @return the initialized {@link GetArtifactOptions} instance.
     */
    @SuppressWarnings("checkstyle:MethodName")
    public static GetArtifactOptions Op() {
        return new GetArtifactOptions();
    }

    /**
     * Creates option with workspace name/id.
     *
     * @param workspace the workspace name/id.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions workspaceName(String workspace) {
        this.workspace = workspace;
        return this;
    }

    /**
     * Creates option with project name/id.
     *
     * @param project the project name/id.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions projectName(String project) {
        this.project = project;
        return this;
    }

    /**
     * Creates option with artifact name.
     *
     * @param artifactName the artifact name.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions name(String artifactName) {
        this.artifactName = artifactName;
        return this;
    }

    /**
     * Creates option with artifact ID.
     *
     * @param artifactId the ID of the artifact.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions artifactId(String artifactId) {
        this.artifactId = artifactId;
        return this;
    }

    /**
     * Creates option with specific version of the artifact to be retrieved.
     *
     * @param version the version of the artifact.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions version(String version) {
        this.version = version;
        return this;
    }

    /**
     * Creates option with specific ID of the version of the artifact to be retrieved.
     *
     * @param versionId the ID of specific version of the artifact.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions versionId(String versionId) {
        this.versionId = versionId;
        return this;
    }

    /**
     * Creates option to get artifact version with specific alias.
     *
     * @param alias the artifact version alias.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions alias(String alias) {
        this.alias = alias;
        return this;
    }

    /**
     * Creates option to get artifact version either by alias or version string.
     *
     * @param versionOrAlias the alias or version string.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions versionOrAlias(String versionOrAlias) {
        this.versionOrAlias = versionOrAlias;
        return this;
    }

    /**
     * Creates option allowing to override the experiment key associated with artifact.
     *
     * @param consumerExperimentKey the new experiment key to be associated.
     * @return the {@link GetArtifactOptions} instance with option set.
     */
    public GetArtifactOptions consumerExperimentKey(String consumerExperimentKey) {
        this.consumerExperimentKey = consumerExperimentKey;
        return this;
    }
}
