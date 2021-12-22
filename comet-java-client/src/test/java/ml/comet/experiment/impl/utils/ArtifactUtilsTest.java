package ml.comet.experiment.impl.utils;

import ml.comet.experiment.artifact.GetArtifactOptions;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ArtifactUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "someName, someName,,",
            "someWorkspace/someName, someName, someWorkspace,",
            "someWorkspace/someName:1.2.3, someName, someWorkspace, 1.2.3"
    })
    public void testParseArtifactName(String name, String expectedName,
                                      String expectedWorkspace, String expectedVersionOrAlias) {
        GetArtifactOptions opts = ArtifactUtils.parseArtifactName(name);
        assertNotNull(opts);
        assertEquals(expectedName, opts.getArtifactName(), "wrong name");
        if (StringUtils.isNotBlank(expectedWorkspace)) {
            assertEquals(expectedWorkspace, opts.getWorkspace(), "wrong workspace");
        }
        if (StringUtils.isNotBlank(expectedVersionOrAlias)) {
            assertEquals(expectedVersionOrAlias, opts.getVersionOrAlias());
        }
    }
}
