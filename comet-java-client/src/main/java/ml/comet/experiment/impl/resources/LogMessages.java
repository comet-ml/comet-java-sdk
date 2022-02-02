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
    public static final String EXPERIMENT_CREATED = "EXPERIMENT_CREATED";
    public static final String EXPERIMENT_CLEANUP_PROMPT = "EXPERIMENT_CLEANUP_PROMPT";
    public static final String EXPERIMENT_HEARTBEAT_STOPPED_PROMPT = "EXPERIMENT_HEARTBEAT_STOPPED_PROMPT";
    public static final String ASSETS_FOLDER_UPLOAD_COMPLETED = "ASSETS_FOLDER_UPLOAD_COMPLETED";
    public static final String ARTIFACT_VERSION_CREATED_WITHOUT_PREVIOUS = "ARTIFACT_VERSION_CREATED_WITHOUT_PREVIOUS";
    public static final String ARTIFACT_VERSION_CREATED_WITH_PREVIOUS = "ARTIFACT_VERSION_CREATED_WITH_PREVIOUS";
    public static final String ARTIFACT_UPLOAD_STARTED = "ARTIFACT_UPLOAD_STARTED";
    public static final String ARTIFACT_UPLOAD_COMPLETED = "ARTIFACT_UPLOAD_COMPLETED";
    public static final String EXPERIMENT_INVENTORY_STATUS_PROMPT = "EXPERIMENT_INVENTORY_STATUS_PROMPT";
    public static final String START_DOWNLOAD_ARTIFACT_ASSETS = "START_DOWNLOAD_ARTIFACT_ASSETS";
    public static final String ARTIFACT_ASSETS_DOWNLOAD_COMPLETED = "ARTIFACT_ASSETS_DOWNLOAD_COMPLETED";
    public static final String COMPLETED_DOWNLOAD_ARTIFACT_ASSET = "COMPLETED_DOWNLOAD_ARTIFACT_ASSET";
    public static final String MODEL_REGISTERED_IN_WORKSPACE = "MODEL_REGISTERED_IN_WORKSPACE";
    public static final String MODEL_VERSION_CREATED_IN_WORKSPACE = "MODEL_VERSION_CREATED_IN_WORKSPACE";
    public static final String DOWNLOADING_REGISTRY_MODEL_PROMPT = "DOWNLOADING_REGISTRY_MODEL_PROMPT";
    public static final String DOWNLOADING_REGISTRY_MODEL_TO_FILE = "DOWNLOADING_REGISTRY_MODEL_TO_FILE";
    public static final String DOWNLOADING_REGISTRY_MODEL_TO_DIR = "DOWNLOADING_REGISTRY_MODEL_TO_DIR";
    public static final String EXTRACTED_N_REGISTRY_MODEL_FILES = "EXTRACTED_N_REGISTRY_MODEL_FILES";

    public static final String LOG_ASSET_FOLDER_EMPTY = "LOG_ASSET_FOLDER_EMPTY";
    public static final String LOG_REMOTE_ASSET_URI_FILE_NAME_TO_DEFAULT = "LOG_REMOTE_ASSET_URI_FILE_NAME_TO_DEFAULT";
    public static final String ARTIFACT_LOGGED_WITHOUT_ASSETS = "ARTIFACT_LOGGED_WITHOUT_ASSETS";
    public static final String ARTIFACT_HAS_NO_ASSETS_TO_DOWNLOAD = "ARTIFACT_HAS_NO_ASSETS_TO_DOWNLOAD";
    public static final String ARTIFACT_ASSETS_FILE_EXISTS_PRESERVING = "ARTIFACT_ASSETS_FILE_EXISTS_PRESERVING";
    public static final String ARTIFACT_DOWNLOAD_FILE_OVERWRITTEN = "ARTIFACT_DOWNLOAD_FILE_OVERWRITTEN";
    public static final String UPDATE_REGISTRY_MODEL_DESCRIPTION_IGNORED = "UPDATE_REGISTRY_MODEL_DESCRIPTION_IGNORED";
    public static final String UPDATE_REGISTRY_MODEL_IS_PUBLIC_IGNORED = "UPDATE_REGISTRY_MODEL_IS_PUBLIC_IGNORED";

    public static final String FAILED_REGISTER_EXPERIMENT = "FAILED_REGISTER_EXPERIMENT";
    public static final String CONFLICTING_ARTIFACT_ASSET_NAME = "CONFLICTING_ARTIFACT_ASSET_NAME";
    public static final String FAILED_READ_DATA_FOR_EXPERIMENT = "FAILED_READ_DATA_FOR_EXPERIMENT";
    public static final String FAILED_TO_SEND_LOG_REQUEST = "FAILED_TO_SEND_LOG_REQUEST";
    public static final String FAILED_TO_SEND_LOG_ASSET_REQUEST = "FAILED_TO_SEND_LOG_ASSET_REQUEST";
    public static final String FAILED_TO_LOG_ASSET_FOLDER = "FAILED_TO_LOG_ASSET_FOLDER";
    public static final String FAILED_TO_LOG_ASSET = "FAILED_TO_LOG_ASSET";
    public static final String FAILED_TO_LOG_REMOTE_ASSET = "FAILED_TO_LOG_REMOTE_ASSET";
    public static final String FAILED_TO_LOG_CODE_ASSET = "FAILED_TO_LOG_CODE_ASSET";
    public static final String FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER = "FAILED_TO_LOG_SOME_ASSET_FROM_FOLDER";
    public static final String ARTIFACT_NOT_FOUND = "ARTIFACT_NOT_FOUND";
    public static final String ARTIFACT_NOT_READY = "ARTIFACT_NOT_READY";
    public static final String ARTIFACT_HAS_NO_DETAILS = "ARTIFACT_HAS_NO_DETAILS";
    public static final String GET_ARTIFACT_FAILED_UNEXPECTEDLY = "GET_ARTIFACT_FAILED_UNEXPECTEDLY";
    public static final String FAILED_TO_UPSERT_ARTIFACT = "FAILED_TO_UPSERT_ARTIFACT";
    public static final String FAILED_TO_UPDATE_ARTIFACT_VERSION_STATE = "FAILED_TO_UPDATE_ARTIFACT_VERSION_STATE";
    public static final String FAILED_TO_UPLOAD_SOME_ARTIFACT_ASSET = "FAILED_TO_UPLOAD_SOME_ARTIFACT_ASSET";
    public static final String FAILED_TO_SEND_LOG_ARTIFACT_ASSET_REQUEST = "FAILED_TO_SEND_LOG_ARTIFACT_ASSET_REQUEST";
    public static final String FAILED_TO_FINALIZE_ARTIFACT_VERSION = "FAILED_TO_FINALIZE_ARTIFACT_VERSION";
    public static final String TIMEOUT_FOR_EXPERIMENT_INVENTORY_CLEANUP = "TIMEOUT_FOR_EXPERIMENT_INVENTORY_CLEANUP";
    public static final String FAILED_TO_CLEAN_EXPERIMENT_INVENTORY = "FAILED_TO_CLEAN_EXPERIMENT_INVENTORY";
    public static final String EXPERIMENT_ALREADY_CLOSED_STATUS_ERROR = "EXPERIMENT_ALREADY_CLOSED_STATUS_ERROR";
    public static final String FAILED_TO_READ_LOGGED_ARTIFACT_ASSETS = "FAILED_TO_READ_LOGGED_ARTIFACT_ASSETS";
    public static final String FAILED_TO_DOWNLOAD_ASSET_FILE_ALREADY_EXISTS =
            "FAILED_TO_DOWNLOAD_ASSET_FILE_ALREADY_EXISTS";
    public static final String FAILED_TO_RESOLVE_ASSET_FILE = "FAILED_TO_RESOLVE_ASSET_FILE";
    public static final String FAILED_TO_DOWNLOAD_ASSET = "FAILED_TO_DOWNLOAD_ASSET";
    public static final String FAILED_TO_READ_DOWNLOADED_FILE_SIZE = "FAILED_TO_READ_DOWNLOADED_FILE_SIZE";
    public static final String FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS = "FAILED_TO_DOWNLOAD_ARTIFACT_ASSETS";
    public static final String FAILED_TO_COMPARE_CONTENT_OF_FILES = "FAILED_TO_COMPARE_CONTENT_OF_FILES";
    public static final String FAILED_TO_CREATE_TEMPORARY_ASSET_DOWNLOAD_FILE =
            "FAILED_TO_CREATE_TEMPORARY_ASSET_DOWNLOAD_FILE";
    public static final String FAILED_TO_DELETE_TEMPORARY_ASSET_FILE = "FAILED_TO_DELETE_TEMPORARY_ASSET_FILE";
    public static final String REMOTE_ASSET_CANNOT_BE_DOWNLOADED = "REMOTE_ASSET_CANNOT_BE_DOWNLOADED";
    public static final String FAILED_TO_PARSE_REMOTE_ASSET_LINK = "FAILED_TO_PARSE_REMOTE_ASSET_LINK";
    public static final String FAILED_TO_SET_ARTIFACT_VERSION_LEQ_THAN_CURRENT =
            "FAILED_TO_SET_ARTIFACT_VERSION_LEQ_THAN_CURRENT";
    public static final String FAILED_TO_FIND_ASSET_IN_ARTIFACT = "FAILED_TO_FIND_ASSET_IN_ARTIFACT";
    public static final String FAILED_TO_LOG_MODEL_ASSET = "FAILED_TO_LOG_MODEL_ASSET";
    public static final String FAILED_TO_LOG_MODEL_FOLDER = "FAILED_TO_LOG_MODEL_FOLDER";
    public static final String EXPERIMENT_HAS_NO_MODELS = "EXPERIMENT_HAS_NO_MODELS";
    public static final String FAILED_TO_FIND_EXPERIMENT_MODEL_BY_NAME = "FAILED_TO_FIND_EXPERIMENT_MODEL_BY_NAME";
    public static final String INVALID_MODEL_REGISTRY_NAME_PROVIDED = "INVALID_MODEL_REGISTRY_NAME_PROVIDED";
    public static final String VERSION_AND_STAGE_SET_DOWNLOAD_MODEL = "VERSION_AND_STAGE_SET_DOWNLOAD_MODEL";
    public static final String FAILED_TO_DOWNLOAD_REGISTRY_MODEL = "FAILED_TO_DOWNLOAD_REGISTRY_MODEL";


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
                    System.err.printf("Failed to format log message for key '%s', reason: %s\n", key, t);
                }
            } else {
                return format;
            }
        }
        return String.format("WARN: Failed to format log message for key '%s'", key);
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
