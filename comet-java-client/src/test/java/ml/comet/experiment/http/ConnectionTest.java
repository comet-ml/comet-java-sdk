package ml.comet.experiment.http;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import lombok.NonNull;
import ml.comet.experiment.model.CreateExperimentRequest;
import ml.comet.experiment.model.CreateExperimentResponse;
import ml.comet.experiment.model.GetWorkspacesResponse;
import ml.comet.experiment.utils.JsonUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static ml.comet.experiment.constants.Constants.*;
import static ml.comet.experiment.http.Connection.COMET_SDK_API_HEADER;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
public class ConnectionTest {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionTest.class);
    private static final String TEST_API_KEY = UUID.randomUUID().toString();

    @Test
    public void testSendGet(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        Map<String, String> params = new HashMap<String, String>() {{
            put(EXPERIMENT_KEY, TEST_API_KEY);
            put("type", ASSET_TYPE_UNKNOWN);
            put("overwrite", Boolean.toString(true));
        }};
        Map<String, StringValuePattern> queryParams = this.createQueryParams(params);
        String endpoint = WORKSPACES;
        GetWorkspacesResponse workspaces = new GetWorkspacesResponse(new ArrayList<String>() {{
            add("comet-ml-team");
            add("workspace");
        }});
        String body = JsonUtils.toJson(workspaces);

        // create test HTTP stub
        stubFor(get(urlPathEqualTo(endpoint))
                .withQueryParams(queryParams)
                .willReturn(ok(body).withHeader("Content-Type", ConnectionUtils.JSON_MIME_TYPE)));

        // execute request and check results
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendGet(endpoint, params);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(body, response.get(), "wrong response body");

        // verify that Auth header was provided as expected
        verify(getRequestedFor(urlPathEqualTo(endpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));
    }

    @Test
    public void testSendPost(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        String endpoint = NEW_EXPERIMENT;
        CreateExperimentRequest expRequest = new CreateExperimentRequest(
                "workspaceName", "projectName", "experimentName");
        String requestStr = JsonUtils.toJson(expRequest);
        CreateExperimentResponse expResponse = new CreateExperimentResponse(
                UUID.randomUUID().toString(), "workspaceName", "projectName",
                "link", "experimentName"
        );
        String responseStr = JsonUtils.toJson(expResponse);

        // create test HTTP stub
        stubFor(post(urlPathEqualTo(endpoint))
                .willReturn(ok(responseStr).withHeader("Content-Type", ConnectionUtils.JSON_MIME_TYPE)));

        // execute request and check results
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendPost(requestStr, endpoint, true);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(responseStr, response.get(), "wrong response body");

        // verify that Auth header was provided as expected
        verify(postRequestedFor(urlPathEqualTo(endpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));
    }

    private Map<String, StringValuePattern> createQueryParams(@NonNull Map<String, String> params) {
        Map<String, StringValuePattern> queryParams = new HashMap<>();
        params.forEach((k, v) -> queryParams.put(k, equalTo(v)));
        return queryParams;
    }
}
