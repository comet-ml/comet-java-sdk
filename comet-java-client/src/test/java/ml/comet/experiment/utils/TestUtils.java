package ml.comet.experiment.utils;

import java.io.File;
import java.net.URL;
import java.util.Objects;

public class TestUtils {
    /**
     * Allows getting file from test resources.
     *
     * @param name the resource file name.
     * @return the <code>File</code> object or <code>null</code> if resource not found.
     */
    public static File getFile(String name) {
        URL resource = Thread.currentThread().getContextClassLoader().getResource(name);
        if (Objects.isNull(resource)) {
            return null;
        }
        return new File(resource.getFile());
    }
}
