package ml.comet.experiment.impl.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static ml.comet.experiment.impl.utils.ResourceUtils.readCometSdkVersion;

/**
 * The common Comet SDK utilities.
 */
@UtilityClass
public final class CometUtils {
    /**
     * The current version of the Comet Java SDK.
     */
    public static final String COMET_JAVA_SDK_VERSION = readCometSdkVersion();

    /**
     * Prints the current version of the Comet Java SDK.
     */
    public void printCometSdkVersion() {
        System.out.println();
        System.out.println("Comet Java SDK version: " + COMET_JAVA_SDK_VERSION);
        System.out.println();
    }

    /**
     * Creates link to the experiment at Comet.
     *
     * @param baseUrl       the server's base URL
     * @param workspaceName the name of the workspace.
     * @param projectName   the name of project
     * @param experimentKey the key of the experiment.
     * @return the link to the experiment.
     */
    public String createExperimentLink(String baseUrl, String workspaceName, String projectName, String experimentKey) {
        String url = String.format("%s/%s/%s/%s",
                baseUrl, workspaceName, projectName, experimentKey);
        // check URI syntax and return
        URI uri = URI.create(url);
        return uri.toString();
    }

    /**
     * Generates global unique identifier in format supported by Comet.ml
     *
     * @return the global unique identifier in format supported by Comet.ml.
     */
    @SuppressWarnings("checkstyle:AbbreviationAsWordInName")
    public static String generateGUID() {
        String guid = UUID.randomUUID().toString();
        return StringUtils.remove(guid, '-');
    }

    /**
     * Puts provided value into the map as string if it is not {@code null}.
     *
     * @param map   the container map.
     * @param key   the key to use.
     * @param value the optional value.
     */
    public static <T> void putNotNull(@NonNull Map<T, String> map, @NonNull T key, Object value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }
}
