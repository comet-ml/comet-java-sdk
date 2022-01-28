package ml.comet.experiment.impl.utils;

import lombok.experimental.UtilityClass;

/**
 * Utilities to work with artifacts.
 */
@UtilityClass
public class ArtifactUtils {

    /**
     * Creates standard full name of the artifact in form 'workspace/name:version'.
     *
     * @param workspace the Comet workspace.
     * @param name      the name of the artifact.
     * @param version   the version of the artifact.
     * @return the standard full name of the artifact.
     */
    public static String artifactFullName(String workspace, String name, String version) {
        return String.format("%s/%s:%s", workspace, name, version);
    }
}
