package ml.comet.experiment.impl.config;

import lombok.Value;
import lombok.experimental.PackagePrivate;
import ml.comet.experiment.exception.ConfigException;

import java.time.Duration;
import java.util.Optional;

/**
 * Represents specific configuration item holding information about key in the configuration file and environment
 * variable key.
 */
@Value
public class ConfigItem {
    /**
     * The key to lookup for this item in the configuration file.
     */
    String configKey;
    /**
     * The key to lookup for this item in the environment variables.
     */
    String environmentKey;
    /**
     * The underlying configuration data holder.
     */
    @PackagePrivate
    CometConfig config;

    /**
     * Return value of this item as string or throw an {@link ConfigException} if value is missing.
     *
     * @return value for this item as string.
     * @throws ConfigException if value is missing or of the wrong type.
     */
    public String getString() throws ConfigException {
        return config.getString(this);
    }

    /**
     * Returns optional value for this item as string.
     *
     * @return the optional value for this item as string.
     */
    public Optional<String> getOptionalString() {
        return config.getOptionalString(this);
    }

    /**
     * Returns value of this item as int or throw an {@link ConfigException} if value is missing or have wrong type.
     *
     * @return value of this item as int.
     * @throws ConfigException if value is missing or of the wrong type.
     */
    public int getInt() throws ConfigException {
        return config.getInt(this);
    }

    /**
     * Returns value of this item as {@link java.time.Duration} or throw an {@link ConfigException}
     * if value is missing or have wrong type.
     *
     * @return the value of this item as {@link java.time.Duration}
     * @throws ConfigException if value is missing or of the wrong type.
     */
    public Duration getDuration() throws ConfigException {
        return config.getDuration(this);
    }

    @Override
    public String toString() {
        return "configKey=" + getConfigKey() + " or environmentKey=" + getEnvironmentKey();
    }
}
