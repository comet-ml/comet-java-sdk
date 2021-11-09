package ml.comet.experiment.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.env.EnvironmentVariableExtractor;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

import static ml.comet.experiment.constants.Constants.BASE_URL_DEFAULT;
import static ml.comet.experiment.constants.Constants.BASE_URL_PLACEHOLDER;
import static ml.comet.experiment.constants.Constants.COMET_API_KEY;
import static ml.comet.experiment.constants.Constants.COMET_PROJECT;
import static ml.comet.experiment.constants.Constants.COMET_WORKSPACE;
import static ml.comet.experiment.constants.Constants.CONNECTION_CLOSE_TIMEOUT_SEC;
import static ml.comet.experiment.constants.Constants.CONNECTION_CLOSE_TIMEOUT_SEC_DEFAULT;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_DEFAULT;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_PLACEHOLDER;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.API_KEY;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.BASE_URL;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.CONNECTION_CLOSE_TIMEOUT_SEC_ENV;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.MAX_AUTH_RETRIES;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.PROJECT_NAME;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.WORKSPACE_NAME;

/**
 * Collection of the configuration utilities.
 */
@UtilityClass
public class ConfigUtils {
    // The name of the properties file bundled as resource with JAVA SDK options, such as current version.
    private static final String SDK_OPTIONS_RESOURCE_FILE = "comet-java-sdk-options.properties";
    // The key in the properties file for current version
    private static final String COMET_SDK_VERSION_KEY = "comet.java.sdk.version";

    /**
     * The Comet Java SDK version.
     */
    public static String COMET_JAVA_SDK_VERSION;

    private static Optional<Config> defaultConfig = Optional.empty();
    private static Optional<Config> overrideConfig = Optional.empty();

    static {
        setDefaultConfig();
        readCometSdkVersion();
    }

    public static void setDefaultConfig() {
        try {
            Config config = ConfigFactory.load().getConfig("comet");
            defaultConfig = Optional.of(config);
        } catch (ConfigException ignored) {
        }
    }

    private static void readCometSdkVersion() {
        try {
            Properties p = ResourceUtils.readProperties(SDK_OPTIONS_RESOURCE_FILE);
            if (p.containsKey(COMET_SDK_VERSION_KEY)) {
                COMET_JAVA_SDK_VERSION = p.getProperty(COMET_SDK_VERSION_KEY);
                // print version
                System.out.println("Comet Java SDK version: " + COMET_JAVA_SDK_VERSION);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void setOverrideConfig(File configFile) {
        Config config = ConfigFactory.parseFile(configFile).getConfig("comet");
        ConfigUtils.overrideConfig = Optional.of(config);
    }

    public static void clearOverrideConfig() {
        ConfigUtils.overrideConfig = Optional.empty();
    }

    public static void setDefaultConfig(File configFile) {
        Config config = ConfigFactory.parseFile(configFile).getConfig("comet");
        ConfigUtils.defaultConfig = Optional.of(config);
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

    public long getConnectionCloseTimeoutSec() {
        return getValueFromSystem(CONNECTION_CLOSE_TIMEOUT_SEC_ENV, CONNECTION_CLOSE_TIMEOUT_SEC)
                .map(Long::parseLong)
                .orElse(CONNECTION_CLOSE_TIMEOUT_SEC_DEFAULT);
    }


    public Config getConfigFromFile(File configFile) {
        return ConfigFactory.parseFile(configFile);
    }

    private String getValueFromSystemOrThrow(String envVarName, String configValueName) {
        return getValueFromSystem(envVarName, configValueName)
                .orElseThrow(() -> new IllegalStateException(String.format(
                        "No parameter with name %s found! Please specify it in env vars or config", configValueName)));
    }

    private Optional<String> getValueFromSystem(String envVarName, String configValueName) {
        Optional<String> envVariable = EnvironmentVariableExtractor.getEnvVariable(envVarName);
        if (envVariable.isPresent()) {
            return envVariable;
        } else if (overrideConfig.isPresent() && overrideConfig.get().hasPath(configValueName)) {
            return overrideConfig.map(x -> x.getString(configValueName));
        } else if (defaultConfig.isPresent() && defaultConfig.get().hasPath(configValueName)) {
            return defaultConfig.map(x -> x.getString(configValueName));
        }
        return Optional.empty();
    }
}
