package ml.comet.experiment.utils;

import ml.comet.experiment.env.EnvironmentVariableExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_DEFAULT;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigUtilsTest {

    private static final String MAX_AUTH_RETRIES_MOCKED_VALUE = "1";
    private static final String TEST_BASE_URL = "https://www.comet.ml";
    private static MockedStatic<EnvironmentVariableExtractor> mockedErrorReporter;
    private static final File emptyCometConfig = Objects.requireNonNull(
            TestUtils.getFile("empty-comet-config.conf"));
    private static final File fullCometConfig = Objects.requireNonNull(
            TestUtils.getFile("full-comet-config.conf"));

    @BeforeEach
    public void init() {
        ConfigUtils.setDefaultConfig();
        ConfigUtils.clearOverrideConfig();
        mockedErrorReporter = Mockito.mockStatic(EnvironmentVariableExtractor.class);
    }

    @AfterEach
    public void close() {
        mockedErrorReporter.close();
    }


    @Test
    public void testApiKeyExtractOrder() {
        validateValueExtractOrder(API_KEY, ConfigUtils::getApiKey);
    }

    @Test
    public void testProjectNameExtractOrder() {
        validateValueExtractOrder(PROJECT_NAME, ConfigUtils::getProjectName);
    }

    @Test
    public void testWorkspaceNameExtractOrder() {
        validateValueExtractOrder(WORKSPACE_NAME, ConfigUtils::getWorkspaceName);
    }

    @Test
    public void testBaseUrlDefaultValue() {
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(BASE_URL)).thenReturn(Optional.empty());
        assertEquals(TEST_BASE_URL, ConfigUtils.getBaseUrlOrDefault());
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(BASE_URL)).thenReturn(Optional.of(BASE_URL));
        assertEquals(BASE_URL, ConfigUtils.getBaseUrlOrDefault());
    }

    @Test
    public void testMaxAuthRetriesDefaultValue() {
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(EnvironmentVariableExtractor.MAX_AUTH_RETRIES)).thenReturn(Optional.empty());
        assertEquals(MAX_AUTH_RETRIES_DEFAULT, ConfigUtils.getMaxAuthRetriesOrDefault());
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(EnvironmentVariableExtractor.MAX_AUTH_RETRIES)).thenReturn(Optional.of(MAX_AUTH_RETRIES_MOCKED_VALUE));
        assertEquals(Integer.parseInt(MAX_AUTH_RETRIES_MOCKED_VALUE), ConfigUtils.getMaxAuthRetriesOrDefault());
    }

    @Test
    public void testExceptionIsThrownWhenNoApiKeyPresent() {
        ConfigUtils.setDefaultConfig(emptyCometConfig);
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(API_KEY)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, ConfigUtils::getApiKeyOrThrow);
    }

    @Test
    public void testExceptionIsThrownWhenNoProjectNameIsPresent() {
        ConfigUtils.setDefaultConfig(emptyCometConfig);
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(PROJECT_NAME)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, ConfigUtils::getProjectNameOrThrow);
    }

    @Test
    public void testExceptionIsThrownWhenNoWorkspaceNameIsPresent() {
        ConfigUtils.setDefaultConfig(emptyCometConfig);
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(WORKSPACE_NAME)).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, ConfigUtils::getWorkspaceNameOrThrow);
    }

    public void validateValueExtractOrder(String envVarName, Supplier<Optional<String>> supplier) {
        //test from env var
        ConfigUtils.setDefaultConfig(emptyCometConfig);
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(envVarName)).thenReturn(Optional.of(envVarName));
        assertTrue(supplier.get().isPresent());
        assertEquals(envVarName, supplier.get().get());

        //test from default config
        ConfigUtils.setDefaultConfig(fullCometConfig);
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(envVarName)).thenReturn(Optional.empty());
        assertTrue(supplier.get().isPresent());
        assertEquals("full", supplier.get().get());

        //test from comet config override
        ConfigUtils.setDefaultConfig(emptyCometConfig);
        ConfigUtils.setOverrideConfig(fullCometConfig);
        assertTrue(supplier.get().isPresent());
        assertEquals("full", supplier.get().get());
    }
}
