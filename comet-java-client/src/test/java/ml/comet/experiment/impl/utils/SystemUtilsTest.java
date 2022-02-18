package ml.comet.experiment.impl.utils;

import ml.comet.experiment.impl.rest.LogAdditionalSystemInfo;
import ml.comet.experiment.impl.rest.SetSystemDetailsRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static ml.comet.experiment.impl.utils.SystemUtils.extraKeys;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SystemUtilsTest {

    @Test
    public void testReadSystemDetails() {
        SetSystemDetailsRequest d = SystemUtils.readSystemDetails();
        assertNotNull(d, "system details expected");
        assertEquals(org.apache.commons.lang3.SystemUtils.USER_NAME, d.getUser());
        assertEquals(org.apache.commons.lang3.SystemUtils.OS_NAME, d.getOs());
        assertEquals(org.apache.commons.lang3.SystemUtils.OS_VERSION, d.getOsRelease());
        assertEquals(org.apache.commons.lang3.SystemUtils.OS_ARCH, d.getMachine());

        List<LogAdditionalSystemInfo> infoList = d.getLogAdditionalSystemInfoList();
        assertNotNull(infoList, "additional properties expected");

        Properties props = System.getProperties();
        extraKeys.forEach(key -> {
            if (props.containsKey(key)) {
                checkPropertyLogged(key, props.getProperty(key), infoList);
            }
        });
    }

    static void checkPropertyLogged(Object key, Object value, List<LogAdditionalSystemInfo> infoList) {
        System.out.printf("%s : %s\n", key, value);
        assertTrue(infoList.stream()
                        .anyMatch(info -> Objects.equals(key, info.getKey()) && Objects.equals(value, info.getValue())),
                String.format("property must be logged, %s : %s", key, value));
    }
}
