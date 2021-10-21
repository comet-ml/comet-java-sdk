package ml.comet.experiment.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.env.EnvironmentVariableExtractor;

import java.io.File;
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

    private static Optional<Config> defaultConfig = getConfig();
    private static Optional<Config> overrideConfig = Optional.empty();

    private static Optional<Config> getConfig() {
        try {
            Config config = ConfigFactory.load().getConfig("comet");
            return Optional.of(config);
        } catch (ConfigException e) {
            return Optional.empty();
        }
    }

    public static void setOverrideConfig(File configFile) {
        Config config = ConfigFactory.parseFile(configFile).getConfig("comet");
        ConfigUtils.overrideConfig = Optional.of(config);
    }

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

    private String getValueFromSystemOrThrow(String envVarName, String configValueName) {
        return getValueFromSystem(envVarName, configValueName)
                .orElseThrow(() -> new IllegalStateException("No parameter with name " + configValueName + "found! Please specify it in env vars or config"));
    }

    private Optional<String> getValueFromSystem(String envVarName, String configValueName) {
        Optional<String> envVariable = EnvironmentVariableExtractor.getEnvVariable(envVarName);
        if (envVariable.isPresent()) {
            return envVariable;
        } else if (overrideConfig.isPresent() && overrideConfig.get().hasPath(configValueName)){
            return overrideConfig.map(x -> x.getString(configValueName));
        } else {
            return defaultConfig.map(x -> x.getString(configValueName));
        }
    }
}
