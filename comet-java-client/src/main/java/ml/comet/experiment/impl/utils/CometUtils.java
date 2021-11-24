package ml.comet.experiment.impl.utils;

import lombok.experimental.UtilityClass;

import java.net.URI;

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
}
