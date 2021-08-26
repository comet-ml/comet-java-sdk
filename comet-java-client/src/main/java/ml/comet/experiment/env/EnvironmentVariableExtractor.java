package ml.comet.experiment.env;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

@UtilityClass
public class EnvironmentVariableExtractor {

    public static final String API_KEY = "COMET_API_KEY";
    public static final String PROJECT_NAME = "COMET_PROJECT_NAME";
    public static final String WORKSPACE_NAME = "COMET_WORKSPACE_NAME";
    public static final String BASE_URL = "COMET_BASE_URL";
    public static final String MAX_AUTH_RETRIES = "COMET_MAX_AUTH_RETRIES";

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
        return getEnvVariable(variableName)
                .orElseThrow(() -> new IllegalStateException("No environment variable with name " + variableName));
    }

    public Optional<String> getEnvVariable(String variableName) {
        String res = System.getenv(variableName);
        if (StringUtils.isEmpty(res)) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

}
