package ml.comet.experiment.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import lombok.experimental.UtilityClass;
import ml.comet.experiment.constants.Constants;

import java.io.File;
import java.net.URL;

@UtilityClass
public class ConfigUtils {

    public Config getDefaultConfigFromClassPath() {
        URL resource = getContextClassLoader().getResource(Constants.DEFAULTS_CONF);
        if (resource == null) {
            return null;
        }
        return ConfigFactory.parseFile(
                new File(resource.getFile()));
    }

    public Config getConfigFromFile(File configFile) {
        return ConfigFactory.parseFile(configFile);
    }

    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
