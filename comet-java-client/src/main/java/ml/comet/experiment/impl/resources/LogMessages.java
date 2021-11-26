package ml.comet.experiment.impl.resources;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;

/**
 * Provides access to the log messages to be presented to the user.
 */
public class LogMessages {
    public static final String LOG_ASSET_FOLDER_EMPTY = "LOG_ASSET_FOLDER_EMPTY";


    /**
     * Gets a formatted string for the given key from this resource bundle.
     *
     * @param key  the key for the desired string
     * @param args the formatting arguments. See {@link String#format(String, Object...)}
     * @return the formatted string for the given key
     */
    public static String getString(String key, Object... args) {
        String format = getString(key);
        if (!StringUtils.isEmpty(format)) {
            if (args != null) {
                try {
                    return String.format(format, args);
                } catch (Throwable t) {
                    System.err.println("Failed to format message for key: " + key);
                    t.printStackTrace();
                }
            } else {
                return format;
            }
        }
        return StringUtils.EMPTY;
    }

    /**
     * Gets a string for the given key from this resource bundle.
     *
     * @param key the key for the desired string
     * @return the string for the given key
     */
    public static String getString(String key) {
        if (res != null) {
            try {
                return res.getString(key);
            } catch (Throwable t) {
                System.err.println("Failed to get message for key: " + key);
                t.printStackTrace();
            }
        }
        return StringUtils.EMPTY;
    }

    static PropertyResourceBundle res;

    static {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("messages");
        if (is != null) {
            try {
                res = new PropertyResourceBundle(is);
            } catch (IOException e) {
                System.err.println("Failed to initialize messages bundle");
                e.printStackTrace();
            }
        } else {
            System.err.println("Failed to find messages bundle");
        }
    }
}
