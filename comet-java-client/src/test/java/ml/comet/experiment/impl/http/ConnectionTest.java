package ml.comet.experiment.impl.http;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import lombok.NonNull;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.impl.TestUtils;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.constants.SdkErrorCodes;
import ml.comet.experiment.impl.rest.CometWebJavaSdkException;
import ml.comet.experiment.impl.utils.JsonUtils;
import org.apache.commons.io.FileUtils;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.unauthorized;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.OVERWRITE;
import static ml.comet.experiment.impl.http.Connection.COMET_SDK_API_HEADER;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
    private static final String SOME_ENDPOINT = "/someEndpoint";
    private static final String SOME_JSON_RESPONSE = "[\"someJsonResponse\"]";
    private static final String SOME_REQUEST_STRING = "someRequestString";
    private static final int BAD_REQUEST_ERROR_CODE = 400;
    private static final String SOME_ERROR_STATUS_MESSAGE = "some error status";
    private static final int SDK_ERROR_CODE = SdkErrorCodes.noArtifactFound;

    static final String IMAGE_FILE_NAME = "someChart.png";
    static final long IMAGE_FILE_SIZE = 31451L;

    private static final CometWebJavaSdkException COMET_WEB_JAVA_SDK_EXCEPTION = new CometWebJavaSdkException(
            BAD_REQUEST_ERROR_CODE, SOME_ERROR_STATUS_MESSAGE, SDK_ERROR_CODE);

    private static final HashMap<QueryParamName, String> SOME_PARAMS = new HashMap<QueryParamName, String>() {{
        put(EXPERIMENT_KEY, "someValue");
        put(OVERWRITE, Boolean.toString(true));
    }};

    @Test
    public void testSendGetWithRetries(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(get(urlPathEqualTo(SOME_ENDPOINT))
                .withQueryParams(createQueryParams(SOME_PARAMS))
                .willReturn(ok(SOME_JSON_RESPONSE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendGetWithRetries(SOME_ENDPOINT, SOME_PARAMS);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(SOME_JSON_RESPONSE, response.get(), "wrong response body");

        // verify that Auth header was set as expected
        //
        verify(getRequestedFor(urlPathEqualTo(SOME_ENDPOINT))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendGetWithRetries_throwOnFailure_onCometWebException(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test error response
        //
        stubFor(get(urlPathEqualTo(SOME_ENDPOINT))
                .withQueryParams(createQueryParams(SOME_PARAMS))
                .willReturn(aResponse()
                        .withBody(JsonUtils.toJson(COMET_WEB_JAVA_SDK_EXCEPTION))
                        .withStatus(BAD_REQUEST_ERROR_CODE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        CometApiException cometApiException = assertThrows(CometApiException.class, () ->
                connection.sendGetWithRetries(SOME_ENDPOINT, SOME_PARAMS, true));
        assertDoesNotThrow(connection::close);

        // test exception values
        //
        checkWebJavaSdkException(cometApiException);

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testSendPostWithRetries(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(ok(SOME_JSON_RESPONSE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendPostWithRetries(SOME_REQUEST_STRING, SOME_ENDPOINT, true);
        assertDoesNotThrow(connection::close);

        assertTrue(response.isPresent(), "response expected");
        assertEquals(SOME_JSON_RESPONSE, response.get(), "wrong response body");

        // verify that Auth header was set as expected
        //
        verify(postRequestedFor(urlPathEqualTo(SOME_ENDPOINT))
                .withHeader(COMET_SDK_API_HEADER, equalTo(TEST_API_KEY)));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    /**
     * Tests that correct exception raised when HTTP error status code received and max retry attempts exceeded
     * with throwOnFailure set to false.
     */
    @Test
    public void testSendPostWithRetries_throwOnFailure_statusError(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(unauthorized()
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        CometApiException apiException = assertThrows(CometApiException.class, () ->
                connection.sendPostWithRetries(SOME_REQUEST_STRING, SOME_ENDPOINT, true));

        // check exception
        assertEquals("Unauthorized", apiException.getStatusMessage(), "wrong status message");
        assertEquals(401, apiException.getStatusCode(), "wrong status code");

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    /**
     * Tests that correct exception raised when Comet web error code received and max retry attempts exceeded
     * with throwOnFailure set to false.
     */
    @Test
    public void testSendPostWithRetries_throwOnFailure_onCometWebException(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test error response
        //
        stubFor(post(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(aResponse()
                        .withBody(JsonUtils.toJson(COMET_WEB_JAVA_SDK_EXCEPTION))
                        .withStatus(BAD_REQUEST_ERROR_CODE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        CometApiException cometApiException = assertThrows(CometApiException.class, () ->
                connection.sendPostWithRetries(SOME_REQUEST_STRING, SOME_ENDPOINT, true));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");

        // check that correct exception returned
        checkWebJavaSdkException(cometApiException);
    }

    /**
     * Tests that empty optional returned if max retry attempts exceeded and throwOnFailure is false.
     */
    @Test
    public void testSendPostWithRetries_throwOnFailure_returnsEmptyOptional(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(badRequest()
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);
        Optional<String> response = connection.sendPostWithRetries(SOME_REQUEST_STRING, SOME_ENDPOINT, false);
        assertDoesNotThrow(connection::close);

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");

        assertFalse(response.isPresent(), "empty optional expected");
    }

    @Test
    public void testSendPostAsync(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test HTTP stub
        //
        stubFor(post(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(ok(SOME_JSON_RESPONSE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.sendPostAsync(SOME_REQUEST_STRING, SOME_ENDPOINT);
        assertNotNull(responseListenableFuture, "future expected");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        assertDoesNotThrow(() -> {
            Response response = completableFuture
                    .exceptionally(throwable -> fail("response failed", throwable))
                    .get(5, TimeUnit.SECONDS);
            assertEquals(SOME_JSON_RESPONSE, response.getResponseBody(), "wrong response body");
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
        stubFor(post(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(notFound()
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.sendPostAsync(SOME_REQUEST_STRING, SOME_ENDPOINT);
        assertNotNull(responseListenableFuture, "future expected");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        Exception exception = assertThrows(ExecutionException.class, () ->
                completableFuture.get(5, TimeUnit.SECONDS));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");

        // check that correct exception returned
        assertTrue(exception.getCause() instanceof CometApiException, "wrong exception returned");

        // check exception values
        CometApiException apiException = (CometApiException) exception.getCause();
        assertEquals("Not Found", apiException.getStatusMessage(), "wrong status message");
        assertEquals(404, apiException.getStatusCode(), "wrong status code");
    }

    /**
     * Tests that ListenableFuture returned will propagate CometApiException in case if Comet web error code received
     * from the endpoint.
     */
    @Test
    public void testSendPostAsync_propagatesException_onCometWebException(@NonNull WireMockRuntimeInfo wmRuntimeInfo) {
        // create test error response
        //
        stubFor(post(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(aResponse()
                        .withBody(JsonUtils.toJson(COMET_WEB_JAVA_SDK_EXCEPTION))
                        .withStatus(BAD_REQUEST_ERROR_CODE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.sendPostAsync(
                SOME_REQUEST_STRING, SOME_ENDPOINT);
        assertNotNull(responseListenableFuture, "future expected");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        Exception exception = assertThrows(ExecutionException.class, () ->
                completableFuture.get(5, TimeUnit.SECONDS));
        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");

        // check that correct exception returned
        assertTrue(exception.getCause() instanceof CometApiException, "wrong exception returned");

        // check exception values
        checkWebJavaSdkException((CometApiException) exception.getCause());
    }

    @Test
    public void testDownloadAsync(@NonNull WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        // create test HTTP stub
        //
        File expectedFile = TestUtils.getFile(IMAGE_FILE_NAME);
        byte[] bodyData = Files.readAllBytes(Objects.requireNonNull(expectedFile).toPath());
        stubFor(get(urlPathEqualTo(SOME_ENDPOINT))
                .withQueryParams(createQueryParams(SOME_PARAMS))
                .willReturn(aResponse()
                        .withBody(bodyData)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_OCTET_STREAM.toString())));

        // execute request and check results
        //
        File downloadFile = Files.createTempFile("testDownload", ".dat").toFile();
        downloadFile.deleteOnExit();
        String baseUrl = wmRuntimeInfo.getHttpBaseUrl();
        Connection connection = new Connection(
                baseUrl, TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.downloadAsync(
                downloadFile, SOME_ENDPOINT, SOME_PARAMS);
        assertNotNull(responseListenableFuture, "future expected");

        // check that inventory was set
        assertEquals(1, connection.getRequestsInventory().get(), "inventory must be set");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        assertDoesNotThrow(() -> {
            Response response = completableFuture
                    .exceptionally(throwable -> fail("response failed", throwable))
                    .get(10, TimeUnit.SECONDS);
            assertEquals(200, response.getStatusCode(), "wrong response status");
        }, "failed to join download response");

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");

        // check that file content as expected
        assertEquals(IMAGE_FILE_SIZE, FileUtils.sizeOf(downloadFile), "wrong file length");
        long expectedCRC32 = FileUtils.checksumCRC32(expectedFile);
        long actualCRC32 = FileUtils.checksumCRC32(downloadFile);
        assertEquals(expectedCRC32, actualCRC32, "wrong file content");
    }

    @Test
    public void testDownloadAsync_fileAccessError(@NonNull WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        // create test HTTP stub
        //
        File expectedFile = TestUtils.getFile(IMAGE_FILE_NAME);
        byte[] bodyData = Files.readAllBytes(Objects.requireNonNull(expectedFile).toPath());
        stubFor(get(urlPathEqualTo(SOME_ENDPOINT))
                .withQueryParams(createQueryParams(SOME_PARAMS))
                .willReturn(aResponse()
                        .withBody(bodyData)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_OCTET_STREAM.toString())));

        // execute request and check results
        //
        File downloadFile = Files.createTempFile(null, null,
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("r--r-----"))).toFile();
        downloadFile.deleteOnExit();
        Connection connection = new Connection(
                wmRuntimeInfo.getHttpBaseUrl(), TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> response = connection.downloadAsync(downloadFile, SOME_ENDPOINT, SOME_PARAMS);
        assertNotNull(response, "response expected");

        Exception exception = assertThrows(ExecutionException.class, response::get);
        assertNotNull(exception, "exception expected");

        assertTrue(exception.getCause() instanceof FileNotFoundException, "wrong exception cause");

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    @Test
    public void testDownloadAsync_onCometApiException(@NonNull WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        // create test error response
        //
        stubFor(get(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(aResponse()
                        .withStatus(404)));

        // execute request and check results
        //
        File downloadFile = Files.createTempFile(null, null).toFile();
        downloadFile.deleteOnExit();
        Connection connection = new Connection(
                wmRuntimeInfo.getHttpBaseUrl(), TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.downloadAsync(downloadFile,
                SOME_ENDPOINT, SOME_PARAMS);
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

    @Test
    public void testDownloadAsync_onCometWebException(@NonNull WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        // create test error response
        //
        stubFor(get(urlPathEqualTo(SOME_ENDPOINT))
                .willReturn(aResponse()
                        .withBody(JsonUtils.toJson(COMET_WEB_JAVA_SDK_EXCEPTION))
                        .withStatus(BAD_REQUEST_ERROR_CODE)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_JSON.toString())));

        // execute request and check results
        //
        File downloadFile = Files.createTempFile(null, null).toFile();
        downloadFile.deleteOnExit();
        Connection connection = new Connection(
                wmRuntimeInfo.getHttpBaseUrl(), TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.downloadAsync(downloadFile,
                SOME_ENDPOINT, SOME_PARAMS);
        assertNotNull(responseListenableFuture, "future expected");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        Exception exception = assertThrows(ExecutionException.class, () ->
                completableFuture.get(5, TimeUnit.SECONDS));

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");

        // check that correct exception returned
        assertTrue(exception.getCause() instanceof CometApiException, "wrong exception returned");

        // check exception values
        checkWebJavaSdkException((CometApiException) exception.getCause());
    }

    @Test
    public void testDownloadAsync_outputStream(@NonNull WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        // create test HTTP stub
        //
        File expectedFile = TestUtils.getFile(IMAGE_FILE_NAME);
        byte[] bodyData = Files.readAllBytes(Objects.requireNonNull(expectedFile).toPath());
        stubFor(get(urlPathEqualTo(SOME_ENDPOINT))
                .withQueryParams(createQueryParams(SOME_PARAMS))
                .willReturn(aResponse()
                        .withBody(bodyData)
                        .withHeader(CONTENT_TYPE.toString(), APPLICATION_OCTET_STREAM.toString())));

        // execute request and check results
        //
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Connection connection = new Connection(
                wmRuntimeInfo.getHttpBaseUrl(), TEST_API_KEY, MAX_AUTH_RETRIES_DEFAULT, logger);

        ListenableFuture<Response> responseListenableFuture = connection.downloadAsync(
                baos, SOME_ENDPOINT, SOME_PARAMS);
        assertNotNull(responseListenableFuture, "future expected");

        // check that inventory was set
        assertEquals(1, connection.getRequestsInventory().get(), "inventory must be set");

        // wait for result
        CompletableFuture<Response> completableFuture = responseListenableFuture.toCompletableFuture();
        assertDoesNotThrow(() -> {
                    Response response = completableFuture
                            .exceptionally(throwable -> fail("response failed", throwable))
                            .get(10, TimeUnit.SECONDS);
                    assertEquals(200, response.getStatusCode(), "wrong response status");
                }, "failed to join download response");


        // check received data
        byte[] received = baos.toByteArray();
        assertEquals(IMAGE_FILE_SIZE, received.length, "wrong received data length");
        assertArrayEquals(bodyData, received, "wrong data received");

        // check that inventory was fully processed
        assertEquals(0, connection.getRequestsInventory().get(), "inventory must be empty");
    }

    private static void checkWebJavaSdkException(CometApiException apiException) {
        assertNotNull(apiException);
        assertEquals(SDK_ERROR_CODE, apiException.getSdkErrorCode(), "wrong SDK error code");
        assertEquals(BAD_REQUEST_ERROR_CODE, apiException.getStatusCode(), "wrong status code");
        assertEquals(SOME_ERROR_STATUS_MESSAGE, apiException.getStatusMessage(), "wrong status message");
    }

    private static Map<String, StringValuePattern> createQueryParams(@NonNull Map<QueryParamName, String> params) {
        Map<String, StringValuePattern> queryParams = new HashMap<>();
        params.forEach((k, v) -> queryParams.put(k.paramName(), equalTo(v)));
        return queryParams;
    }
}
