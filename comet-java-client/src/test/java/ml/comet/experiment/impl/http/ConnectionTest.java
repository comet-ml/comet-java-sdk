package ml.comet.experiment.impl.http;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import lombok.NonNull;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.constants.SdkErrorCodes;
import ml.comet.experiment.impl.rest.CometWebJavaSdkException;
import ml.comet.experiment.impl.utils.JsonUtils;
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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
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
    private static final String someEndpoint = "/someEndpoint";
    private static final String someJsonResponse = "[\"someJsonResponse\"]";
    private static final String someRequestStr = "someRequestString";
    private static final int BAD_REQUEST_ERROR_CODE = 400;

    private static final HashMap<QueryParamName, String> someParams = new HashMap<QueryParamName, String>() {{
        put(EXPERIMENT_KEY, "someValue");
        put(OVERWRITE, Boolean.toString(true));
    }};

    @Test
    public void testSendGetWithRetries(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(get(urlPathEqualTo(someEndpoint))
                .withQueryParams(this.createQueryParams(someParams))
                .willReturn(ok(someJsonResponse).withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendGetWithRetries(someEndpoint, someParams);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(someJsonResponse, response.get(), "wrong response body");

        // verify that Auth header was set as expected
        //
        verify(getRequestedFor(urlPathEqualTo(someEndpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendGet(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(get(urlPathEqualTo(someEndpoint))
                .withQueryParams(this.createQueryParams(someParams))
                .willReturn(ok(someJsonResponse).withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendGet(someEndpoint, someParams);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(someJsonResponse, response.get(), "wrong response body");

        // verify that Auth header was set as expected
        //
        verify(getRequestedFor(urlPathEqualTo(someEndpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendGetBadRequest(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test error response
        //
        String errorMessage = "Some message";
        int sdkErrorCode = SdkErrorCodes.noArtifactFound;
        CometWebJavaSdkException ex = new CometWebJavaSdkException(BAD_REQUEST_ERROR_CODE, errorMessage, sdkErrorCode);
        String responseStr = JsonUtils.toJson(ex);

        stubFor(get(urlPathEqualTo(someEndpoint))
                .withQueryParams(this.createQueryParams(someParams))
                .willReturn(aResponse().withBody(responseStr).withStatus(BAD_REQUEST_ERROR_CODE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        CometApiException cometApiException = assertThrows(CometApiException.class, () ->
                connection.sendGet(someEndpoint, someParams));
        assertDoesNotThrow(connection::close);

        // test exception values
        //
        assertNotNull(cometApiException);
        assertEquals(sdkErrorCode, cometApiException.getSdkErrorCode(), "wrong SDK error code");

        // verify that Auth header was set as expected
        //
        verify(getRequestedFor(urlPathEqualTo(someEndpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendPostWithRetries(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(someEndpoint))
                .willReturn(ok(someJsonResponse).withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendPostWithRetries(someRequestStr, someEndpoint, true);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(someJsonResponse, response.get(), "wrong response body");

        // verify that Auth header was set as expected
        //
        verify(postRequestedFor(urlPathEqualTo(someEndpoint))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));
        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendPostWithRetries_throwsException(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(someEndpoint))
                .willReturn(badRequest().withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        assertThrows(CometApiException.class, () ->
                connection.sendPostWithRetries(someRequestStr, someEndpoint, true));
    }

    /**
     * Tests that empty optional returned if max retry attempts exceeded and throwOnFailure is false.
     */
    @Test
    public void testSendPostWithRetries_returnsEmptyOptional(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(someEndpoint))
                .willReturn(badRequest().withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendPostWithRetries(someRequestStr, someEndpoint, false);
        assertDoesNotThrow(connection::close);

        assertFalse(response.isPresent(), "empty optional expected");
    }

    @Test
    public void testSendPostAsync(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(someEndpoint))
                .willReturn(ok(someJsonResponse).withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.sendPostAsync(someRequestStr, someEndpoint);
        assertNotNull(responseListenableFuture, "future expected");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        assertDoesNotThrow(() -> {
            Response response = completableFuture
                    .exceptionally(throwable -> fail("response failed", throwable))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(someJsonResponse, response.getResponseBody(), "wrong response body");
        });
        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    /**
     * Tests that ListenableFuture returned will propagate CometApiException in case if error status code received
     * from the endpoint.
     */
    @Test
    public void testSendPostAsync_propagatesException_onErrorStatusCodeReceived(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(someEndpoint))
                .willReturn(badRequest().withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.sendPostAsync(someRequestStr, someEndpoint);
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
