package ml.comet.experiment.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestCometUtils {

    @Test
    public void testJavaSdkVersionParsing() {
        // Tests that Comet Java SDK version was set
        assertNotNull(CometUtils.COMET_JAVA_SDK_VERSION);
        assertFalse(StringUtils.isEmpty(CometUtils.COMET_JAVA_SDK_VERSION));
    }
}