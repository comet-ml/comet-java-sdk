package ml.comet.experiment.utils;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.util.Properties;

import static ml.comet.experiment.utils.ResourceUtils.readCometSdkVersion;

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
}
