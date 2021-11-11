package ml.comet.experiment.config;


import ml.comet.experiment.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.net.MalformedURLException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CometConfigTest {

    private static final File fullCometConfig = Objects.requireNonNull(
            TestUtils.getFile("full-comet-config.conf"));

    private MockedStatic<EnvironmentConfig> mockedEnvironmentConfig;

    @BeforeEach
    public void init() {
        mockedEnvironmentConfig = Mockito.mockStatic(EnvironmentConfig.class);
    }

    @AfterEach
    public void close() {
        mockedEnvironmentConfig.close();
        CometConfig.clearConfigOverride();
    }

    @Test
    public void testMandatoryValueIsMissing() {
        // we are testing that exception is raising when mandatory value is missing from all the configuration sources
        assertThrows(ConfigException.class, CometConfig.COMET_API_KEY::getString);
    }

    @Test
    public void testOptionalValueIsMissing() {
        // we test that empty optional returned when optional value is missing from all the configuration sources
        Optional<String> projectName = CometConfig.COMET_PROJECT_NAME.getOptionalString();
        assertNotNull(projectName);
        assertFalse(projectName.isPresent());
    }

    @Test
    public void testExtractionOrder() {
        // testing that values from environment variables supersedes the values from configuration.
        ConfigItem item = CometConfig.COMET_API_KEY;

        // override default configuration and check that value is present
        //
        String expectedKeyValue = "full";
        CometConfig.applyConfigOverride(fullCometConfig);
        String actualKeyValue = item.getString();
        assertEquals(expectedKeyValue, actualKeyValue, "wrong value returned from OVERRIDE");

        // set environment variable and test that new value returned for the same config item
        //
        expectedKeyValue = "someApiKeyValue";
        mockedEnvironmentConfig.when(() -> EnvironmentConfig.getEnvVariable(
                item.getEnvironmentKey())).thenReturn(Optional.of(expectedKeyValue));

        actualKeyValue = item.getString();
        assertEquals(expectedKeyValue, actualKeyValue, "wrong value returned from ENV");
    }

    @Test
    public void testDefaultValue() {
        // testing that default value is taken from default config file (reference.conf)
        Duration timeout = CometConfig.COMET_TIMEOUT_CLEANING_SECONDS.getDuration();
        assertNotNull(timeout);
        assertEquals(timeout.getSeconds(), 3600);
    }

    @Test
    public void testDefaultValueOverrideByApplication() {
        // testing that default value was overridden explicitly from application config file (application.conf)
        int retries = CometConfig.COMET_MAX_AUTH_RETRIES.getInt();
        assertEquals(retries, 5);
    }

    @Test
    public void testApplyConfigOverrideFromFile() {
        // testing that default value was overridden explicitly from another config file (full-comet-config.conf)
        CometConfig.applyConfigOverride(fullCometConfig);
        Duration timeout = CometConfig.COMET_TIMEOUT_CLEANING_SECONDS.getDuration();
        assertNotNull(timeout);
        assertEquals(timeout.getSeconds(), 60);
    }

    @Test
    public void testApplyConfigOverrideFromUrl() throws MalformedURLException {
        // testing that default value was overridden explicitly from another config file by URL (full-comet-config.conf)
        CometConfig.applyConfigOverride(fullCometConfig.toURI().toURL());
        Duration timeout = CometConfig.COMET_TIMEOUT_CLEANING_SECONDS.getDuration();
        assertNotNull(timeout);
        assertEquals(timeout.getSeconds(), 60);
    }

    @Test
    public void testClearOverrideConfig() {
        // testing that default value restore after override cleared

        // override default value and check
        //
        CometConfig.applyConfigOverride(fullCometConfig);
        Duration timeout = CometConfig.COMET_TIMEOUT_CLEANING_SECONDS.getDuration();
        assertNotNull(timeout);
        assertEquals(timeout.getSeconds(), 60);

        // clear override and check value
        //
        CometConfig.clearConfigOverride();
        timeout = CometConfig.COMET_TIMEOUT_CLEANING_SECONDS.getDuration();
        assertNotNull(timeout);
        assertEquals(timeout.getSeconds(), 3600);
    }
}
