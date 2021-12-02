package ml.comet.experiment.impl.http;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import lombok.NonNull;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.impl.constants.QueryParamName;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.badRequest;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.OVERWRITE;
import static ml.comet.experiment.impl.http.Connection.COMET_SDK_API_HEADER;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@WireMockTest
public class ConnectionTest {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionTest.class);
    private static final String TEST_API_KEY = UUID.randomUUID().toString();
    private static final int MAX_AUTH_RETRIES_DEFAULT = 4;

    @Test
    public void testSendGetWithRetries(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        //
        HashMap<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, "someValue");
            put(OVERWRITE, Boolean.toString(true));
        }};
        Map<String, StringValuePattern> queryParams = this.createQueryParams(params);

        String endpoint = "/someEndpoint";
        String responseStr = "[\"someJsonResponse\"]";

        // create test HTTP stub
        //
        stubFor(get(urlPathEqualTo(endpoint))
                .withQueryParams(queryParams)
                .willReturn(ok(responseStr).withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendGetWithRetries(endpoint, params);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(responseStr, response.get(), "wrong response body");

        // verify that Auth header was set as expected
        //
        verify(getRequestedFor(urlPathEqualTo(endpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendPostWithRetries(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        //
        String endpoint = "/someEndpoint";
        String requestStr = "someRequestString";
        String responseStr = "[\"someJsonResponse\"]";

        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(endpoint))
                .willReturn(ok(responseStr).withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendPostWithRetries(requestStr, endpoint, true);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(responseStr, response.get(), "wrong response body");

        // verify that Auth header was set as expected
        //
        verify(postRequestedFor(urlPathEqualTo(endpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));
        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendPostWithRetriesException(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        //
        String endpoint = "/someEndpoint";
        String requestStr = "someRequestString";

        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(endpoint))
                .willReturn(badRequest().withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        assertThrows(CometApiException.class, () ->
                connection.sendPostWithRetries(requestStr, endpoint, true));
    }

    /**
     * Tests that empty optional returned if max retry attempts exceeded and throwOnFailure is false.
     */
    @Test
    public void testSendPostWithRetriesEmptyOptional(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        //
        String endpoint = "/someEndpoint";
        String requestStr = "someRequestString";

        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(endpoint))
                .willReturn(badRequest().withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendPostWithRetries(requestStr, endpoint, false);
        assertDoesNotThrow(connection::close);

        assertFalse(response.isPresent(), "empty optional expected");
    }

    @Test
    public void testSendPostAsync(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        //
        String endpoint = "/someEndpoint";
        String requestStr = "someRequestString";
        String responseStr = "[\"someJsonResponse\"]";

        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(endpoint))
                .willReturn(ok(responseStr).withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.sendPostAsync(requestStr, endpoint);
        assertNotNull(responseListenableFuture, "future expected");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        assertDoesNotThrow(() -> {
            Response response = completableFuture
                    .exceptionally(throwable -> fail("response failed", throwable))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(responseStr, response.getResponseBody(), "wrong response body");
        });
        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendPostAsyncErrorStatus(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test data
        //
        String endpoint = "/someEndpoint";
        String requestStr = "someRequestString";

        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(endpoint))
                .willReturn(badRequest().withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.sendPostAsync(requestStr, endpoint);
        assertNotNull(responseListenableFuture, "future expected");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        Exception exception = assertThrows(ExecutionException.class, () ->
                completableFuture.get(5, TimeUnit.SECONDS));
        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
        // check that correct exception returned
        assertTrue(exception.getCause() instanceof CometApiException, "wrong exception returned");
    }

    private Map<String, StringValuePattern> createQueryParams(@NonNull Map<QueryParamName, String> params) {
        Map<String, StringValuePattern> queryParams = new HashMap<>();
        params.forEach((k, v) -> queryParams.put(k.paramName(), equalTo(v)));
        return queryParams;
    }
}
