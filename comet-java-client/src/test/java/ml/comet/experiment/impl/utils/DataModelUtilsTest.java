package ml.comet.experiment.impl.utils;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DataModelUtilsTest {

    @Test
    public void testMetadataFromJson() {
        Map<String, Object> expected = new HashMap<>();
        expected.put("string", "someString");
        expected.put("boolean", Boolean.TRUE);
        expected.put("int", 100);

        String json = JsonUtils.toJson(expected);

        // convert back and check
        Map<String, Object> actual = DataModelUtils.metadataFromJson(json);
        assertNotNull(actual);

        assertEquals(expected, actual);
    }
}
