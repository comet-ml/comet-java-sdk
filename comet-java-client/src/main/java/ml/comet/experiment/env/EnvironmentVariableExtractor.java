package ml.comet.experiment.env;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

@UtilityClass
public class EnvironmentVariableExtractor {

    private static final String API_KEY = "COMET_API_KEY";
    private static final String PROJECT_NAME = "COMET_PROJECT_NAME";
    private static final String WORKSPACE_NAME = "COMET_WORKSPACE_NAME";

    public String getApiKeyOrThrow() {
        return getEnvVariableOrThrow(API_KEY);
    }

    public String getProjectNameOrThrow() {
        return getEnvVariableOrThrow(PROJECT_NAME);
    }

    public String getWorkspaceNameOrThrow() {
        return getEnvVariableOrThrow(WORKSPACE_NAME);
    }

    private String getEnvVariableOrThrow(String variableName) {
        String res = System.getenv(variableName);
        if (StringUtils.isEmpty(res)) {
            throw new IllegalStateException("No environment variable with name " + variableName);
        }
        return res;
    }

}
