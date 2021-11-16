package ml.comet.experiment.impl.constants;

/**
 * The enumeration of all known query parameter names.
 */
public enum QueryParamName {

    EXPERIMENT_KEY("experimentKey"),
    FILE_NAME("fileName"),
    CONTEXT("context"),
    TYPE("type"),
    OVERWRITE("overwrite"),
    STEP("step"),
    EPOCH("epoch"),
    PROJECT_ID("projectId"),
    WORKSPACE_NAME("workspaceName");

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
