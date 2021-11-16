package ml.comet.experiment.config;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

/**
 * Utility class to read values from environment variables.
 */
@UtilityClass
final class EnvironmentConfig {

    /**
     * Allows reading configuration option from the environment.
     *
     * @param optionName the option name.
     * @return Optional with specific value if found or empty otherwise.
     * @throws IllegalStateException if optionName is
     */
    public Optional<String> getEnvVariable(String optionName) throws IllegalStateException {
        if (StringUtils.isEmpty(optionName)) {
            throw new IllegalStateException("optionName is empty");
        }
        String res = System.getenv(optionName);
        if (StringUtils.isEmpty(res)) {
            return Optional.empty();
        }
        return Optional.of(res);
    }
}
