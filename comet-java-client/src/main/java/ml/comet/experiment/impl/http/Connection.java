package ml.comet.experiment.impl.http;

import lombok.NonNull;
import lombok.Value;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.rest.CometWebJavaSdkException;
import ml.comet.experiment.impl.utils.JsonUtils;
import org.asynchttpclient.AsyncCompletionHandler;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static ml.comet.experiment.impl.http.ConnectionUtils.createGetRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostByteArrayRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostFileRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostFormRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostJsonRequest;
import static org.asynchttpclient.Dsl.asyncHttpClient;

/**
 * Represents connection with the CometML server. Provides utility methods to send
 * various data records to the server.
 *
 * <p>Make sure to close this connection to avoid resources leak.
 */
@Value
public class Connection implements Closeable {
    // The default request timeout in milliseconds
    public static final int REQUEST_TIMEOUT_MS = 60 * 1000;
    // The default connection shutdown timeout in milliseconds
    public static final int CONNECTION_SHUTDOWN_TIMEOUT_MS = 5 * 1000;
    // The name of the HTTP header with Comet API key
    public static final String COMET_SDK_API_HEADER = "Comet-Sdk-Api";

    private static final String RESPONSE_NO_BODY = "NO BODY";

    AsyncHttpClient asyncHttpClient;
    String cometBaseUrl;
    String apiKey;
    Logger logger;
    /**
     * The maximum number of retries when contacting server.
     */
    int maxAuthRetries;
    /**
     * This is inventory tracker to maintain remaining list of scheduled asynchronous request posts. It will be used
     * to properly close this connection only after all scheduled requests are processed.
     */
    AtomicInteger requestsInventory;

    /**
     * Creates new instance with specified parameters.
     *
     * @param cometBaseUrl   the base URL of the Comet server's endpoints.
     * @param apiKey         the API key to authorize Comet API access
     * @param maxAuthRetries the maximum number of retries per failed request.
     * @param logger         the Logger to collect log records.
     */
    public Connection(@NonNull String cometBaseUrl, @NonNull String apiKey,
                      int maxAuthRetries, @NonNull Logger logger) {
        this.cometBaseUrl = cometBaseUrl;
        this.apiKey = apiKey;
        this.logger = logger;
        this.maxAuthRetries = maxAuthRetries;
        this.requestsInventory = new AtomicInteger();
        // create configured HTTP client
        AsyncHttpClientConfig conf = new DefaultAsyncHttpClientConfig.Builder()
                .setRequestTimeout(REQUEST_TIMEOUT_MS).setShutdownTimeout(CONNECTION_SHUTDOWN_TIMEOUT_MS).build();
        this.asyncHttpClient = asyncHttpClient(conf);
    }

    /**
     * Allows sending synchronous GET request to the specified endpoint with given request parameters.
     * It will attempt to retry request if failed for the {@code maxAuthRetries} attempts.
     *
     * @param endpoint the request path of the endpoint
     * @param params   the map with request parameters.
     * @return the {@link Optional} of response body.
     */
    public Optional<String> sendGetWithRetries(@NonNull String endpoint, @NonNull Map<QueryParamName, String> params) {
        return executeRequestSyncWithRetries(
                createGetRequest(this.buildCometUrl(endpoint), params), false);
    }

    /**
     * Allows sending synchronous GET request to the specified endpoint with given request parameters.
     * If request failed to {@link CometApiException} will be thrown with related details.
     *
     * @param endpoint the request path of the endpoint
     * @param params   the map with request parameters.
     * @return the {@link Optional} of response body.
     * @throws CometApiException if failed.
     */
    public Optional<String> sendGet(@NonNull String endpoint, @NonNull Map<QueryParamName, String> params)
            throws CometApiException {
        return executeRequestSync(
                createGetRequest(this.buildCometUrl(endpoint), params)
        );
    }

    /**
     * Allows sending POST to the specified endpoint with body as JSON string. This method will retry request using
     * {@link #maxAuthRetries} attempts. If failed empty {@link Optional} will be returned or {@link CometApiException}
     * will be thrown if {@link #maxAuthRetries} attempts exceeded.
     *
     * @param json           the JSON string to be posted.
     * @param endpoint       the relative path to the endpoint
     * @param throwOnFailure the flag to indicate if exception should be thrown on failure of request execution.
     * @return the {@link Optional} of response body.
     * @throws CometApiException if throwOnFailure set to {@code true} and request was failed.
     */
    public Optional<String> sendPostWithRetries(
            @NonNull String json, @NonNull String endpoint, boolean throwOnFailure) throws CometApiException {
        String url = this.buildCometUrl(endpoint);
        if (logger.isDebugEnabled()) {
            logger.debug("sending JSON {} to {}", json, url);
        }
        return executeRequestSyncWithRetries(createPostJsonRequest(json, url), throwOnFailure);
    }

    /**
     * Allows asynchronous sending of text as JSON encoded body of the POST request.
     *
     * @param json     the JSON encoded text.
     * @param endpoint the relative path to the endpoint.
     * @return the {@link ListenableFuture} which can be used to monitor status of the request execution.
     */
    public ListenableFuture<Response> sendPostAsync(@NonNull String json, @NonNull String endpoint) {
        return executeRequestAsync(
                createPostJsonRequest(json, this.buildCometUrl(endpoint)));
    }

    /**
     * Allows asynchronous posting the content of the file as multipart form data to the specified endpoint.
     *
     * @param file        the file to be included.
     * @param endpoint    the relative path to the endpoint.
     * @param queryParams the request query parameters
     * @param formParams  the form parameters
     * @return the {@link ListenableFuture} which can be used to monitor status of the request execution.
     */
    public ListenableFuture<Response> sendPostAsync(@NonNull File file, @NonNull String endpoint,
                                                    @NonNull Map<QueryParamName, String> queryParams,
                                                    Map<FormParamName, Object> formParams) {
        return executeRequestAsync(
                createPostFileRequest(file, this.buildCometUrl(endpoint), queryParams, formParams));
    }

    /**
     * Allows asynchronous sending of provided byte array as POST request to the specified endpoint.
     *
     * @param bytes      the data array
     * @param endpoint   the relative path to the endpoint.
     * @param params     the request parameters map.
     * @param formParams the form parameters
     * @return the {@link ListenableFuture} which can be used to monitor status of the request execution.
     */
    public ListenableFuture<Response> sendPostAsync(byte[] bytes, @NonNull String endpoint,
                                                    @NonNull Map<QueryParamName, String> params,
                                                    Map<FormParamName, Object> formParams) {
        String url = this.buildCometUrl(endpoint);
        if (logger.isDebugEnabled()) {
            logger.debug("sending POST bytearray with length {} to {}", bytes.length, url);
        }

        return executeRequestAsync(createPostByteArrayRequest(bytes, url, params, formParams));
    }

    /**
     * Allows asynchronous FORM submission to the specified endpoint.
     *
     * @param endpoint   the relative path to the endpoint.
     * @param params     the request parameters map.
     * @param formParams the form parameters
     * @return the {@link ListenableFuture} which can be used to monitor status of the request execution.
     */
    public ListenableFuture<Response> sendPostAsync(@NonNull String endpoint,
                                                    @NonNull Map<QueryParamName, String> params,
                                                    @NonNull Map<FormParamName, Object> formParams) {
        String url = this.buildCometUrl(endpoint);
        if (logger.isDebugEnabled()) {
            logger.debug("sending POST form to {}", url);
        }

        return executeRequestAsync(createPostFormRequest(url, params, formParams));
    }

    /**
     * Closes this connection immediately by releasing underlying resources.
     *
     * <p>Please note that some asynchronous post request can still be not processed, which will result in errors.
     * Use this method with great caution and only if you are not expecting any request still be unprocessed. For all
     * other cases it is recommended to use waitAndClose method.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        this.asyncHttpClient.close();
    }

    /**
     * Allows to properly close this connection after all scheduled posts request are executed or if timeout expired.
     *
     * @param timeout the maximum duration to wait before closing connection.
     * @throws IOException          if an I/O error occurs.
     * @throws InterruptedException if current thread was interrupted during wait.
     * @throws TimeoutException     if cleaning timeout exceeded.
     */
    public void waitAndClose(@NonNull Duration timeout) throws IOException, InterruptedException, TimeoutException {
        long nanosTimeout = timeout.toNanos();
        final long deadline = System.nanoTime() + nanosTimeout;
        // block until all requests in inventory are processed or timeout exceeded
        while (this.requestsInventory.get() > 0) {
            if (Thread.interrupted()) {
                throw new InterruptedException();
            }
            nanosTimeout = deadline - System.nanoTime();
            if (nanosTimeout <= 0L) {
                throw new TimeoutException(String.format(
                        "timeout exceeded while waiting for remaining requests to complete, "
                                + "remaining requests: %d", this.requestsInventory.get()));
            }
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("waiting for {} request items to execute, elapsed {} seconds",
                        this.requestsInventory.get(), TimeUnit.SECONDS.convert(nanosTimeout, TimeUnit.NANOSECONDS));
            }
            // give other processes a chance
            Thread.sleep(100);
        }

        // close connection immediately
        this.close();
    }

    /**
     * Executes provided request asynchronously.
     *
     * @param request the request to be executed.
     * @return the {@link ListenableFuture} which can be used to check request status.
     */
    ListenableFuture<Response> executeRequestAsync(@NonNull Request request) {
        // check that client is not closed
        if (this.asyncHttpClient.isClosed()) {
            String msg = String.format("failed to execute request %s connection to the server already closed", request);
            return new ListenableFuture.CompletedFailure<>(
                    "asyncHttpClient already closed", new CometGeneralException(msg));
        }

        // increment inventory
        this.requestsInventory.incrementAndGet();

        request.getHeaders().add(COMET_SDK_API_HEADER, apiKey);
        String endpoint = request.getUrl();
        return this.asyncHttpClient.executeRequest(request,
                new AsyncCompletionInventoryHandler(this.requestsInventory, this.logger, endpoint));
    }

    /**
     * Synchronously executes provided request.
     *
     * @param request the request to be executed
     * @return the response body or empty {@link Optional}.
     * @throws CometApiException if request failed or remote endpoint returned error.
     */
    Optional<String> executeRequestSync(@NonNull Request request) throws CometApiException {
        request.getHeaders().add(COMET_SDK_API_HEADER, apiKey);
        Response response;
        try {
            response = this.asyncHttpClient
                    .executeRequest(request)
                    .get();
        } catch (Throwable e) {
            this.logger.error("Failed to execute request: " + request, e);
            throw new CometApiException("failed to execute request, unknown error", e);
        }

        int statusCode = response.getStatusCode();
        if (ConnectionUtils.isResponseSuccessful(statusCode)) {
            return Optional.ofNullable(response.getResponseBody());
        }

        // process error
        if (statusCode == 400 && response.hasResponseBody()) {
            // BAD_REQUEST received - parse error message
            CometWebJavaSdkException exception = JsonUtils.fromJson(
                    response.getResponseBody(), CometWebJavaSdkException.class);

            throw new CometApiException(statusCode, exception.getMsg(), exception.getSdkErrorCode());
        }

        // unknown error
        this.logger.error("Failed to execute request: ({}), unexpected error received: status code [{}], message '{}' ",
                request, statusCode, response.getResponseBody());
        throw new CometApiException(statusCode, response.getStatusText(), 0);
    }

    /**
     * Synchronously executes provided request. It will attempt to execute request {@link #maxAuthRetries} in
     * case of failure. If all attempts failed the empty optional will be returned or {@link CometApiException}
     * will be thrown in case of {@code throwOnFailure} is {@code true}.
     *
     * @param request        the request to be executed
     * @param throwOnFailure if {@code true} throws exception on failure. Otherwise, empty {@link Optional} will be
     *                       returned.
     * @return the response body or empty {@link Optional}.
     * @throws CometApiException if throwOnFailure set to {@code true} and request was failed.
     */
    Optional<String> executeRequestSyncWithRetries(
            @NonNull Request request, boolean throwOnFailure) throws CometApiException {
        request.getHeaders().add(COMET_SDK_API_HEADER, apiKey);
        String endpoint = request.getUrl();
        try {
            org.asynchttpclient.Response response = null;
            for (int i = 1; i < this.maxAuthRetries; i++) {
                if (this.asyncHttpClient.isClosed()) {
                    this.logger.warn("failed to execute request {}, the connection already closed.", request);
                    if (throwOnFailure) {
                        throw new CometApiException("failed to execute request, the connection already closed.");
                    }
                    return Optional.empty();
                }

                // execute request and wait for completion until default REQUEST_TIMEOUT_MS exceeded
                response = this.asyncHttpClient
                        .executeRequest(request)
                        .get();

                if (!ConnectionUtils.isResponseSuccessful(response.getStatusCode())) {
                    // attempt failed - check if to retry
                    if (i < this.maxAuthRetries - 1) {
                        // sleep for a while and repeat
                        this.logger.debug("for endpoint {} response {}, retrying\n",
                                endpoint, response.getStatusText());
                        Thread.sleep((2 ^ i) * 1000L);
                    } else {
                        // maximal number of attempts exceeded - throw or return
                        this.logger.error(
                                "for endpoint {} got the response '{}', the last retry failed from {} attempts",
                                endpoint, response.getStatusText(), this.maxAuthRetries);
                        if (throwOnFailure) {
                            String body = response.hasResponseBody() ? response.getResponseBody() : RESPONSE_NO_BODY;
                            throw new CometApiException(
                                    "failed to call endpoint: %s, response status: %s, body: %s, failed attempts: %d",
                                    endpoint, response.getStatusCode(), body, this.maxAuthRetries);
                        }
                        return Optional.empty();
                    }
                } else {
                    // success - log debug and stop trying
                    if (this.logger.isDebugEnabled()) {
                        this.logger.debug("for endpoint {} got response {}\n", endpoint, response.getResponseBody());
                    }
                    break;
                }
            }

            if (response == null || !response.hasResponseBody()) {
                return Optional.empty();
            }
            return Optional.of(response.getResponseBody());
        } catch (Throwable e) {
            this.logger.error("Failed to execute request: " + request, e);
            if (throwOnFailure) {
                throw new CometApiException("failed to execute request, unknown error", e);
            }
            return Optional.empty();
        }
    }

    private String buildCometUrl(String endpoint) {
        return this.cometBaseUrl + endpoint;
    }

    /**
     * The request completion listener to be used to maintain the current requests' inventory status.
     */
    static final class AsyncCompletionInventoryHandler extends AsyncCompletionHandler<Response> {
        AtomicInteger requestInventory;
        Logger logger;
        String endpoint;

        AsyncCompletionInventoryHandler(AtomicInteger inventory, Logger logger, String endpoint) {
            this.requestInventory = inventory;
            this.logger = logger;
            this.endpoint = endpoint;
        }

        @Override
        public Response onCompleted(Response response) {
            // check response status and throw exception if failed
            if (!ConnectionUtils.isResponseSuccessful(response.getStatusCode())) {
                throw new CometApiException("received error status code [%d] for request: %s, reason: %s",
                        response.getStatusCode(), this.endpoint, response.getStatusText());
            }
            // decrease inventory only after check passed,
            // if it was not passed it will be decreased in onThrowable()
            this.decreaseInventory();
            return response;
        }

        @Override
        public void onThrowable(Throwable t) {
            // decrease inventory
            this.decreaseInventory();
            this.logger.error("failed to execute request to the endpoint {}", this.endpoint, t);
        }

        private void decreaseInventory() {
            this.requestInventory.decrementAndGet();
        }
    }
}
