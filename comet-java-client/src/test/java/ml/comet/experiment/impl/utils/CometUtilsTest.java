package ml.comet.experiment.impl.utils;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class CometUtilsTest {

    @Test
    public void testJavaSdkVersionParsing() {
        // Tests that Comet Java SDK version was set
        assertNotNull(CometUtils.COMET_JAVA_SDK_VERSION);
        assertFalse(StringUtils.isEmpty(CometUtils.COMET_JAVA_SDK_VERSION));
    }

    @Test
    public void testGenerateGUID() {
        String guid = CometUtils.generateGUID();
        assertFalse(StringUtils.isEmpty(guid), "GUID expected");
        assertEquals(32, guid.length(), "wrong length");
    }
}
