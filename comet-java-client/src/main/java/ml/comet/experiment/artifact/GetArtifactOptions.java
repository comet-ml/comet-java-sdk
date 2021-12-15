package ml.comet.experiment.artifact;

import lombok.Getter;
import lombok.ToString;

/**
 * Holds options used to query for Comet artifact.
 */
@Getter
@ToString
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
     * Factory to create {@link GetArtifactOptionsBuilder} which can be used to create properly initialized
     * instance of the {@link GetArtifactOptions}.
     *
     * @return the initialized {@link GetArtifactOptionsBuilder} instance.
     */
    @SuppressWarnings("checkstyle:MethodName")
    public static GetArtifactOptionsBuilder Op() {
        return new GetArtifactOptionsBuilder();
    }

    /**
     * The builder to create properly initialized instances of the {@link GetArtifactOptions}.
     */
    static final class GetArtifactOptionsBuilder {
        final GetArtifactOptions options;

        GetArtifactOptionsBuilder() {
            this.options = new GetArtifactOptions();
        }

        /**
         * Creates option with workspace name/id.
         *
         * @param workspace the workspace name/id.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder workspaceName(String workspace) {
            this.options.workspace = workspace;
            return this;
        }

        /**
         * Creates option with project name/id.
         *
         * @param project the project name/id.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder projectName(String project) {
            this.options.project = project;
            return this;
        }

        /**
         * Creates option with artifact name.
         *
         * @param artifactName the artifact name.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder name(String artifactName) {
            this.options.artifactName = artifactName;
            return this;
        }

        /**
         * Creates option with artifact ID.
         *
         * @param artifactId the ID of the artifact.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder artifactId(String artifactId) {
            this.options.artifactId = artifactId;
            return this;
        }

        /**
         * Creates option with specific version of the artifact to be retrieved.
         *
         * @param version the version of the artifact.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder version(String version) {
            this.options.version = version;
            return this;
        }

        /**
         * Creates option with specific ID of the version of the artifact to be retrieved.
         *
         * @param versionId the ID of specific version of the artifact.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder versionId(String versionId) {
            this.options.versionId = versionId;
            return this;
        }

        /**
         * Creates option to get artifact version with specific alias.
         *
         * @param alias the artifact version alias.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder alias(String alias) {
            this.options.alias = alias;
            return this;
        }

        /**
         * Creates option to get artifact version either by alias or version string.
         *
         * @param versionOrAlias the alias or version string.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder versionOrAlias(String versionOrAlias) {
            this.options.versionOrAlias = versionOrAlias;
            return this;
        }

        /**
         * Creates option allowing to override the experiment key associated with artifact.
         *
         * @param consumerExperimentKey the new experiment key to be associated.
         * @return the {@link GetArtifactOptionsBuilder} instance with option set.
         */
        public GetArtifactOptionsBuilder consumerExperimentKey(String consumerExperimentKey) {
            this.options.consumerExperimentKey = consumerExperimentKey;
            return this;
        }

        /**
         * Creates properly initialized instance of the {@link GetArtifactOptions}.
         *
         * @return the properly initialized instance of the {@link GetArtifactOptions}.
         */
        public GetArtifactOptions build() {
            return this.options;
        }
    }
}
