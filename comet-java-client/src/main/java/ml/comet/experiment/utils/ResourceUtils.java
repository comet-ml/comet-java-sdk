package ml.comet.experiment.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Collection of utilities to access bundled resource files.
 */
public class ResourceUtils {

    /**
     * Allows reading properties from bundled resources.
     *
     * @param properties the name of the properties file.
     * @return the initialized Properties instance.
     * @throws IOException if failed to load Properties.
     */
    public static Properties readProperties(String properties) throws IOException {
        Properties p = new Properties();
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(properties);
        if (is != null) {
            p.load(is);
        } else {
            throw new FileNotFoundException(properties);
        }
        return p;
    }
}
