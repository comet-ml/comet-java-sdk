package ml.comet.experiment.impl.resources;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;

/**
 * Provides access to the log messages to be presented to the user.
 */
@UtilityClass
public class LogMessages {

    public static final String EXPERIMENT_LIVE = "EXPERIMENT_LIVE";
    public static final String EXPERIMENT_CLEANUP_PROMPT = "EXPERIMENT_CLEANUP_PROMPT";
    public static final String EXPERIMENT_HEARTBEAT_STOPPED_PROMPT = "EXPERIMENT_HEARTBEAT_STOPPED_PROMPT";
    public static final String ASSETS_FOLDER_UPLOAD_COMPLETED = "ASSETS_FOLDER_UPLOAD_COMPLETED";
    public static final String ARTIFACT_VERSION_CREATED_WITHOUT_PREVIOUS = "ARTIFACT_VERSION_CREATED_WITHOUT_PREVIOUS";
    public static final String ARTIFACT_VERSION_CREATED_WITH_PREVIOUS = "ARTIFACT_VERSION_CREATED_WITH_PREVIOUS";
    public static final String ARTIFACT_UPLOAD_STARTED = "ARTIFACT_UPLOAD_STARTED";

    public static final String LOG_ASSET_FOLDER_EMPTY = "LOG_ASSET_FOLDER_EMPTY";
    public static final String LOG_REMOTE_ASSET_URI_FILE_NAME_TO_DEFAULT = "LOG_REMOTE_ASSET_URI_FILE_NAME_TO_DEFAULT";
    public static final String ARTIFACT_LOGGED_WITHOUT_ASSETS = "ARTIFACT_LOGGED_WITHOUT_ASSETS";

    public static final String CONFLICTING_ARTIFACT_ASSET_NAME = "CONFLICTING_ARTIFACT_ASSET_NAME";
    public static final String FAILED_READ_DATA_FOR_EXPERIMENT = "FAILED_READ_DATA_FOR_EXPERIMENT";
    public static final String FAILED_TO_SEND_LOG_REQUEST = "FAILED_TO_SEND_LOG_REQUEST";
    public static final String FAILED_TO_SEND_LOG_ASSET_REQUEST = "FAILED_TO_SEND_LOG_ASSET_REQUEST";
    public static final String FAILED_TO_LOG_ASSET_FOLDER = "FAILED_TO_LOG_ASSET_FOLDER";
    public static final String FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER = "FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER";


    /**
     * Gets a formatted string for the given key from this resource bundle.
     *
     * @param key  the key for the desired string
     * @param args the formatting arguments. See {@link String#format(String, Object...)}
     * @return the formatted string for the given key
     */
    public static String getString(String key, Object... args) {
        String format = getString(key);
        if (StringUtils.isNotBlank(format)) {
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
        InputStream is = Thread.currentThread()
                .getContextClassLoader().getResourceAsStream("messages.properties");
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
