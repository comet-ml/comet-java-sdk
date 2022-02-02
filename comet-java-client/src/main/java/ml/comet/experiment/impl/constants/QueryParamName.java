package ml.comet.experiment.impl.constants;

/**
 * The enumeration of all known query parameter names of the REST endpoints.
 */
public enum QueryParamName {

    EXPERIMENT_KEY("experimentKey"), // string
    EXTENSION("extension"), // string
    EPOCH("epoch"), // integer
    STEP("step"), // integer
    SOURCE("source"), // string
    CONTEXT("context"), // string
    TYPE("type"), // string
    METADATA("metadata"), // json string
    FILE_NAME("fileName"), // string
    GROUPING_NAME("groupingName"), // string
    ARTIFACT_VERSION_ID("artifactVersionId"), // string
    IS_REMOTE("isRemote"), // boolean
    OVERWRITE("overwrite"), // boolean
    PROJECT_ID("projectId"), // string
    WORKSPACE_NAME("workspaceName"), // string

    WORKSPACE("workspace"), // string
    PROJECT("project"), // string
    ARTIFACT_NAME("artifactName"), // string
    ARTIFACT_ID("artifactId"), // string
    VERSION_ID("versionId"), // string
    VERSION("version"), // string
    ALIAS("alias"), // string
    VERSION_OR_ALIAS("versionOrAlias"), // string
    CONSUMER_EXPERIMENT_KEY("consumerExperimentKey"), // string

    ASSET_ID("assetId"), // string

    MODEL_NAME("modelName"), // string
    STAGE("stage"); // string


    private final String paramName;

    QueryParamName(String paramName) {
        this.paramName = paramName;
    }

    public String paramName() {
        return this.paramName;
    }

    @Override
    public String toString() {
        return this.paramName;
    }
}
