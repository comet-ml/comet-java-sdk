package ml.comet.experiment.impl.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.NonNull;
import ml.comet.experiment.exception.ConfigException;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * <p>The configuration holder of the Comet Java SDK.</p>
 *
 * <p>The default configuration values are specified in the {@code resources/reference.conf} file. The values in this
 * file can be overridden by the {@code application.conf} file if found in the class path. Additionally, it is possible
 * to explicitly override configuration options from particular file
 * using {@link #applyConfigOverride(java.io.File)}. Furthermore, all configuration options defined via
 * <i>system properties</i> or <i>environment variables</i> precedes all previous configuration sources.</p>
 *
 * <p>The configuration search order as following (first-listed are higher priority):</p>
 * <ul>
 *     <li>system properties or environment variables</li>
 *     <li>configuration file set by call to {@link #applyConfigOverride(java.io.File)}</li>
 *     <li>{@code application.conf} (all resources on classpath with this name)</li>
 *     <li>{@code reference.conf} (all resources on classpath with this name)</li>
 * </ul>
 *
 * <p>To get specific configuration value you should use any static field with name starting with
 * {@code COMET_}, e.g. {@link #COMET_API_KEY}.</p>
 */
public final class CometConfig {
    // The configuration instance
    private static final CometConfig instance = new CometConfig();

    static {
        // Loads default configuration when class loads by VM.
        loadDefaultConfig();
    }

    /**
     * The Comet API key.
     */
    public static final ConfigItem COMET_API_KEY =
            new ConfigItem("apiKey", "COMET_API_KEY", instance);
    /**
     * The Comet project name.
     */
    public static final ConfigItem COMET_PROJECT_NAME =
            new ConfigItem("project", "COMET_PROJECT_NAME", instance);
    /**
     * The key of the existing Comet experiment.
     */
    public static final ConfigItem COMET_EXPERIMENT_KEY =
            new ConfigItem("experimentKey", "COMET_EXPERIMENT_KEY", instance);
    /**
     * The Comet workspace name.
     */
    public static final ConfigItem COMET_WORKSPACE_NAME =
            new ConfigItem("workspace", "COMET_WORKSPACE_NAME", instance);
    /**
     * The Comet's servers base URL.
     */
    public static final ConfigItem COMET_BASE_URL =
            new ConfigItem("baseUrl", "COMET_BASE_URL", instance);
    /**
     * The maximal number of authentication retries against Comet.
     */
    public static final ConfigItem COMET_MAX_AUTH_RETRIES =
            new ConfigItem("maxAuthRetries", "COMET_MAX_AUTH_RETRIES", instance);
    /**
     * The timeout to clean up all waiting uploads and close connection to the server (seconds).
     */
    public static final ConfigItem COMET_TIMEOUT_CLEANING_SECONDS =
            new ConfigItem("cleaningTimeoutSeconds", "COMET_TIMEOUT_CLEANING", instance);

    private static final String ERR_MISSING_FORMAT = "No configuration parameter [%s] found! "
            + "Please specify it in the environment variables or configuration file";

    // The default configuration tree holder
    Config defaultConfig;
    // The current configuration tree holder
    Config config;

    /**
     * Uses provided configuration file to override some/all default configuration tree values.
     *
     * @param configFile the configuration file to take configuration parameters from.
     */
    public static void applyConfigOverride(@NonNull File configFile) {
        Config overrideConfig = ConfigFactory.parseFile(configFile).getConfig("comet");
        overrideConfig.withFallback(instance.defaultConfig);
        instance.config = overrideConfig;
    }

    /**
     * Allows overriding default configuration tree with values parsed from provided {@link java.net.URL} pointing
     * to the configuration file.
     *
     * @param configFileUrl the {@link java.net.URL} of the configuration file to get overrides from.
     */
    public static void applyConfigOverride(@NonNull URL configFileUrl) {
        Config overrideConfig = ConfigFactory.parseURL(configFileUrl).getConfig("comet");
        overrideConfig.withFallback(instance.defaultConfig);
        instance.config = overrideConfig;
    }

    /**
     * Restores default configuration values if it was overridden previously.
     */
    public static void clearConfigOverride() {
        assert instance.defaultConfig != null;
        instance.config = instance.defaultConfig;
    }

    /**
     * Loads default configuration from the {@code resources/reference.conf} file bundled with SDK.
     */
    static void loadDefaultConfig() {
        try {
            instance.defaultConfig = ConfigFactory.load().getConfig("comet");
            instance.config = instance.defaultConfig;
        } catch (com.typesafe.config.ConfigException e) {
            e.printStackTrace();
        }
    }

    /**
     * Return configuration as string value for the specified {@link ConfigItem}.
     *
     * @param item the configuration item.
     * @return the configuration value as string.
     * @throws ConfigException if value is missing or of the wrong type.
     */
    String getString(@NonNull ConfigItem item) throws ConfigException {
        return instance.readValue(item)
                .orElseThrow(() -> new ConfigException(String.format(ERR_MISSING_FORMAT, item)));
    }

    /**
     * Return optional configuration as string value for the specified {@link ConfigItem}.
     *
     * @param item the configuration item.
     * @return the optional configuration value as string.
     */
    Optional<String> getOptionalString(@NonNull ConfigItem item) {
        return instance.readValue(item);
    }

    /**
     * Returns the value for specified {@link ConfigItem} as int.
     *
     * @param item the configuration item.
     * @return the configuration value as int.
     * @throws ConfigException if value is missing or of the wrong type.
     */
    int getInt(@NonNull ConfigItem item) throws ConfigException {
        try {
            return instance.readValue(item)
                    .map(Integer::parseInt)
                    .orElseThrow(() -> new ConfigException(String.format(ERR_MISSING_FORMAT, item)));
        } catch (NumberFormatException e) {
            throw new ConfigException("failed to parse integer parameter value for the config item: " + item, e);
        }
    }

    /**
     * Returns the value for specified {@link ConfigItem} as {@link java.time.Duration}.
     *
     * @param item the configuration item.
     * @return the configuration value as {@link java.time.Duration}.
     * @throws ConfigException if value is missing or of the wrong type.
     */
    Duration getDuration(@NonNull ConfigItem item) throws ConfigException {
        try {
            int duration = instance.readValue(item)
                    .map(Integer::parseInt)
                    .orElseThrow(() -> new ConfigException(String.format(ERR_MISSING_FORMAT, item)));
            return Duration.of(duration, ChronoUnit.SECONDS);
        } catch (NumberFormatException e) {
            throw new ConfigException("failed to parse integer parameter value for the config item: " + item, e);
        }
    }

    private Optional<String> readValue(@NonNull ConfigItem item) {
        Optional<String> envVariable = EnvironmentConfig.getEnvVariable(item.getEnvironmentKey());
        if (envVariable.isPresent()) {
            return envVariable;
        } else if (instance.config.hasPath(item.getConfigKey())) {
            return Optional.of(instance.config.getString(item.getConfigKey()));
        } else {
            return Optional.empty();
        }
    }
}
