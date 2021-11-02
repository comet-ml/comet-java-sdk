package ml.comet.experiment.utils;

import ml.comet.experiment.OnlineExperimentTest;

import java.io.File;
import java.net.URL;

public class TestUtils {
    public static File getFile(String name) {
        URL resource = OnlineExperimentTest.class.getClassLoader().getResource(name);
        if (resource == null) {
            return null;
        }
        return new File(resource.getFile());
    }
}
