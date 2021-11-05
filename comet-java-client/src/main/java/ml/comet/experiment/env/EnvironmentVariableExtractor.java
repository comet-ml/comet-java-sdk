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
    public static final String CONNECTION_CLOSE_TIMEOUT_SEC_ENV = "COMET_CONNECTION_CLOSE_TIMEOUT_SEC";

    public Optional<String> getEnvVariable(String variableName) {
        String res = System.getenv(variableName);
        if (StringUtils.isEmpty(res)) {
            return Optional.empty();
        }
        return Optional.of(res);
    }

}
