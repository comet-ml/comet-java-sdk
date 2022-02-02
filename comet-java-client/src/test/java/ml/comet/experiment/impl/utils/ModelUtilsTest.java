package ml.comet.experiment.impl.utils;

import ml.comet.experiment.registrymodel.DownloadModelOptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.platform.commons.util.StringUtils;

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

    @ParameterizedTest(name = "[{index}] registryName: {0}, version: {1}, stage: {2}, expectedFileName: {3}")
    @CsvSource({
            "modelName, 1.0.0,,modelName_1.0.0.zip",
            "modelName,,production,modelName_production.zip",
            "modelName,,,modelName_latest.zip"
    })
    public void testRegistryModelZipFileName(String registryName, String version,
                                             String stage, String expectedFileName) {
        DownloadModelOptions.DownloadModelOptionsBuilder builder = DownloadModelOptions.Op();
        if (StringUtils.isNotBlank(version)) {
            builder.withVersion(version);
        }
        if (StringUtils.isNotBlank(stage)) {
            builder.withStage(stage);
        }
        DownloadModelOptions opts = builder.build();
        String actual = ModelUtils.registryModelZipFileName(registryName, opts);
        assertEquals(expectedFileName, actual, "wrong ZIP file name");
    }
}
