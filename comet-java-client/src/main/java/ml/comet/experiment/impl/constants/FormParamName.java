package ml.comet.experiment.impl.constants;

/**
 * Enumeration of all known form parameter names of the REST endpoints.
 */
public enum FormParamName {

    TAGS("tags"), // string list
    LINK("link"), // string
    METADATA("metadata"), // json string
    FILE("file"); // InputStream or FormDataContentDisposition

    private final String paramName;

    FormParamName(String paramName) {
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
