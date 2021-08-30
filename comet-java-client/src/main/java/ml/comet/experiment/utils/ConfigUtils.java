package ml.comet.experiment.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.constants.Constants;
import ml.comet.experiment.env.EnvironmentVariableExtractor;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.util.Optional;

import static ml.comet.experiment.constants.Constants.BASE_URL_DEFAULT;
import static ml.comet.experiment.constants.Constants.BASE_URL_PLACEHOLDER;
import static ml.comet.experiment.constants.Constants.COMET_API_KEY;
import static ml.comet.experiment.constants.Constants.COMET_PROJECT;
import static ml.comet.experiment.constants.Constants.COMET_WORKSPACE;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_DEFAULT;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_PLACEHOLDER;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.API_KEY;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.BASE_URL;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.MAX_AUTH_RETRIES;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.PROJECT_NAME;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.WORKSPACE_NAME;

@UtilityClass
public class ConfigUtils {

    public String getApiKeyOrThrow() {
        return getValueFromSystemOrThrow(API_KEY, COMET_API_KEY);
    }

    public Optional<String> getApiKey() {
        return getValueFromSystem(API_KEY, COMET_API_KEY);
    }

    public String getProjectNameOrThrow() {
        return getValueFromSystemOrThrow(PROJECT_NAME, COMET_PROJECT);
    }

    public Optional<String> getProjectName() {
        return getValueFromSystem(PROJECT_NAME, COMET_PROJECT);
    }

    public String getWorkspaceNameOrThrow() {
        return getValueFromSystemOrThrow(WORKSPACE_NAME, COMET_WORKSPACE);
    }

    public Optional<String> getWorkspaceName() {
        return getValueFromSystem(WORKSPACE_NAME, COMET_WORKSPACE);
    }

    public String getBaseUrlOrDefault() {
        return getValueFromSystem(BASE_URL, BASE_URL_PLACEHOLDER)
                .orElse(BASE_URL_DEFAULT);
    }

    public int getMaxAuthRetriesOrDefault() {
        return getValueFromSystem(MAX_AUTH_RETRIES, MAX_AUTH_RETRIES_PLACEHOLDER)
                .map(Integer::parseInt)
                .orElse(MAX_AUTH_RETRIES_DEFAULT);
    }


    public Config getConfigFromFile(File configFile) {
        return ConfigFactory.parseFile(configFile);
    }

    private Config getDefaultConfigFromClassPath() {
        URL resource = getContextClassLoader().getResource(Constants.DEFAULTS_CONF);
        if (resource == null) {
            return null;
        }
        return ConfigFactory.parseFile(
                new File(resource.getFile()));
    }

    private String getValueFromSystemOrThrow(String envVarName, String configValueName) {
        return getValueFromSystem(envVarName, configValueName)
                .orElseThrow(() -> new IllegalStateException("No parameter with name " + configValueName + "found! Please specify it in env vars or config"));
    }

    private Optional<String> getValueFromSystem(String envVarName, String configValueName) {
        Optional<String> envVariable = EnvironmentVariableExtractor.getEnvVariable(envVarName);
        if (envVariable.isPresent()) {
            return envVariable;
        }
        Config defaultConfig = getDefaultConfigFromClassPath();
        if (defaultConfig == null) {
            return Optional.empty();
        }
        String configValue = defaultConfig.getString(configValueName);
        if (StringUtils.isEmpty(configValue)) {
            return Optional.empty();
        }
        return Optional.of(configValue);
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
