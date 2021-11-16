package ml.comet.experiment.impl.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Collection of utilities to access bundled resource files.
 */
public class ResourceUtils {
    // The name of the properties file bundled as resource with JAVA SDK options, such as current version.
    private static final String SDK_OPTIONS_RESOURCE_FILE = "comet-java-sdk-options.properties";
    // The key in the properties file for current version
    private static final String COMET_SDK_VERSION_KEY = "comet.java.sdk.version";

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

    static String readCometSdkVersion() {
        try {
            Properties p = ResourceUtils.readProperties(SDK_OPTIONS_RESOURCE_FILE);
            if (p.containsKey(COMET_SDK_VERSION_KEY)) {
                return p.getProperty(COMET_SDK_VERSION_KEY);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
