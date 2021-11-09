package ml.comet.experiment.utils;

import lombok.experimental.UtilityClass;

import static ml.comet.experiment.utils.ConfigUtils.readCometSdkVersion;

/**
 * The common Comet SDK utilities.
 */
@UtilityClass
public class CometUtils {
    /**
     * The current version of the Comet Java SDK.
     */
    public static final String COMET_JAVA_SDK_VERSION = readCometSdkVersion();

    /**
     * Prints the current version of the Java SDK.
     */
    public void printCometSdkVersion() {
        System.out.println();
        System.out.println("Comet Java SDK version: " + COMET_JAVA_SDK_VERSION);
        System.out.println();
    }
}
