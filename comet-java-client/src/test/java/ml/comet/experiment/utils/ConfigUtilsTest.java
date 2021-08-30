package ml.comet.experiment.utils;

import ml.comet.experiment.env.EnvironmentVariableExtractor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.function.Supplier;

import static ml.comet.experiment.constants.Constants.BASE_URL_DEFAULT;
import static ml.comet.experiment.constants.Constants.MAX_AUTH_RETRIES_DEFAULT;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.API_KEY;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.BASE_URL;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.PROJECT_NAME;
import static ml.comet.experiment.env.EnvironmentVariableExtractor.WORKSPACE_NAME;

public class ConfigUtilsTest {

    private static final String MAX_AUTH_RETRIES_MOCKED_VALUE = "1";
    private static MockedStatic<EnvironmentVariableExtractor> mockedErrorReporter;

    @Before
    public void init() {
        mockedErrorReporter = Mockito.mockStatic(EnvironmentVariableExtractor.class);
    }

    @After
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
        Assert.assertEquals(BASE_URL_DEFAULT, ConfigUtils.getBaseUrlOrDefault());
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(BASE_URL)).thenReturn(Optional.of(BASE_URL));
        Assert.assertEquals(BASE_URL, ConfigUtils.getBaseUrlOrDefault());
    }

    @Test
    public void testMaxAuthRetriesDefaultValue() {
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(EnvironmentVariableExtractor.MAX_AUTH_RETRIES)).thenReturn(Optional.empty());
        Assert.assertEquals(MAX_AUTH_RETRIES_DEFAULT, ConfigUtils.getMaxAuthRetriesOrDefault());
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(EnvironmentVariableExtractor.MAX_AUTH_RETRIES)).thenReturn(Optional.of(MAX_AUTH_RETRIES_MOCKED_VALUE));
        Assert.assertEquals(Integer.parseInt(MAX_AUTH_RETRIES_MOCKED_VALUE), ConfigUtils.getMaxAuthRetriesOrDefault());
    }

    @Test(expected = IllegalStateException.class)
    public void testExceptionIsThrownWhenNoApiKeyPresent() {
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(API_KEY)).thenReturn(Optional.empty());
        ConfigUtils.getApiKeyOrThrow();
    }

    @Test(expected = IllegalStateException.class)
    public void testExceptionIsThrownWhenNoProjectNameIsPresent() {
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(PROJECT_NAME)).thenReturn(Optional.empty());
        ConfigUtils.getProjectNameOrThrow();
    }

    @Test(expected = IllegalStateException.class)
    public void testExceptionIsThrownWhenNoWorkspaceNameIsPresent() {
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(WORKSPACE_NAME)).thenReturn(Optional.empty());
        ConfigUtils.getWorkspaceNameOrThrow();
    }


    public void validateValueExtractOrder(String envVarName, Supplier<Optional<String>> supplier) {
        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(envVarName)).thenReturn(Optional.empty());

        Assert.assertFalse(supplier.get().isPresent());

        mockedErrorReporter.when(() -> EnvironmentVariableExtractor.getEnvVariable(envVarName)).thenReturn(Optional.of(envVarName));

        Assert.assertTrue(supplier.get().isPresent());
        Assert.assertEquals(envVarName, supplier.get().get());
    }



}
