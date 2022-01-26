package ml.comet.experiment.impl.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test cases to check {@link ModelUtils} implementation.
 */
public class ModelUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "plainName1,plainname1",
            "&plainName1$#,plainname1",
            "plainName1$#,plainname1",
            "plain$#Name1,plain-name1",
            "plain$#%Name1,plain-name1",
            "plain$#%@Name1,plain-name1",
            "plain$#2%@3Name1,plain-2-3name1",
    })
    public void testCreateRegistryModelName(String name, String expected) {
        String actual = ModelUtils.createRegistryModelName(name);
        assertEquals(expected, actual, "wrong result");
    }
}
