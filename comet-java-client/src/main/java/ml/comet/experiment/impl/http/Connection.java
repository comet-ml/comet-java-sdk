package ml.comet.experiment.impl.http;

import lombok.NonNull;
import lombok.Value;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.constants.QueryParamName;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

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

    String cometBaseUrl;
    String apiKey;
    Logger logger;
    int maxAuthRetries;
    AsyncHttpClient asyncHttpClient;
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
     *
     * @param endpoint the request path of the endpoint
     * @param params   the map with request parameters.
     * @return the Optional response body.
     */
    public Optional<String> sendGet(@NonNull String endpoint, @NonNull Map<QueryParamName, String> params) {
        return executeRequestWithAuth(
                ConnectionUtils.createGetRequest(this.buildCometUrl(endpoint), params), false);
    }

    /**
     * Allows sending POST to the specified endpoint with body as JSON string.
     *
     * @param json           the JSON string to be posted.
     * @param endpoint       the relative path to the endpoint
     * @param throwOnFailure the flag to indicate if exception should be thrown on failure of request execution.
     * @return the Optional response body.
     */
    public Optional<String> sendPost(@NonNull String json, @NonNull String endpoint, boolean throwOnFailure) {
        String url = this.buildCometUrl(endpoint);
        if (logger.isDebugEnabled()) {
            logger.debug("sending JSON {} to {}", json, url);
        }
        return executeRequestWithAuth(ConnectionUtils.createPostJsonRequest(json, url), throwOnFailure);
    }

    /**
     * Allows asynchronous sending given object as JSON encoded body of the POST request.
     *
     * @param payload  the payload object to be sent.
     * @param endpoint the relative path to the endpoint.
     */
    public void sendPostAsync(@NonNull Object payload, @NonNull String endpoint) {
        CompletableFuture<Response> future = sendPostAsync(JsonUtils.toJson(payload), endpoint)
                .toCompletableFuture()
                .exceptionally(t -> {
                            logger.error("failed to execute asynchronous request to endpoint {} with payload {}",
                                    endpoint, payload, t);
                            return null;
                        }
                );
        if (logger.isDebugEnabled()) {
            future.thenApply(getDebugLogResponse(endpoint));
        }
    }

    /**
     * Allows asynchronous sending of text as JSON encoded body of the POST request.
     *
     * @param json     the JSON encoded text.
     * @param endpoint the relative path to the endpoint.
     * @return the <code>ListenableFuture&lt;Response&gt;</code> which can be used to monitor status of
     * the request execution.
     */
    public ListenableFuture<Response> sendPostAsync(@NonNull String json, @NonNull String endpoint) {
        return executeRequestWithAuthAsync(
                ConnectionUtils.createPostJsonRequest(json, this.buildCometUrl(endpoint)));
    }

    /**
     * Allows asynchronous posting the content of the file as multipart form data to the specified endpoint.
     *
     * @param file     the file to be included.
     * @param endpoint the relative path to the endpoint.
     * @param params   the request parameters
     * @return the <code>ListenableFuture&lt;Response&gt;</code> which can be used to monitor status of
     * the request execution.
     */
    public ListenableFuture<Response> sendPostAsync(@NonNull File file, @NonNull String endpoint,
                                                    @NonNull Map<QueryParamName, String> params) {
        return executeRequestWithAuthAsync(
                ConnectionUtils.createPostFileRequest(file, this.buildCometUrl(endpoint), params));
    }

    /**
     * Allows asynchronous sending of provided byte array as POST request to the specified endpoint.
     *
     * @param bytes    the data array
     * @param endpoint the relative path to the endpoint.
     * @param params   the request parameters map.
     * @return the <code>ListenableFuture&lt;Response&gt;</code> which can be used to monitor status of
     * the request execution.
     */
    public ListenableFuture<Response> sendPostAsync(byte[] bytes, @NonNull String endpoint,
                                                    @NonNull Map<QueryParamName, String> params) {
        String url = this.buildCometUrl(endpoint);
        if (logger.isDebugEnabled()) {
            logger.debug("sending POST bytearray with length {} to {}", bytes.length, url);
        }

        return executeRequestWithAuthAsync(ConnectionUtils.createPostByteArrayRequest(bytes, url, params));
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
     * @return the <code>ListenableFuture</code> which can be used to check request status.
     */
    ListenableFuture<Response> executeRequestWithAuthAsync(@NonNull Request request) {
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
     * Synchronously executes provided request. It will attempt to execute request <code>maxAuthRetries</code> in
     * case of failure. If all attempts failed the empty optional will be returned or <code>CometGeneralException</code>
     * will be thrown in case of <code>throwOnFailure</code> is <code>true</code>.
     *
     * @param request        the request to be executed
     * @param throwOnFailure if <code>true</code> throws exception on failure. Otherwise, empty Optional will be
     *                       returned.
     * @return the response body or empty Optional.
     */
    Optional<String> executeRequestWithAuth(@NonNull Request request, boolean throwOnFailure) {
        request.getHeaders().add(COMET_SDK_API_HEADER, apiKey);
        String endpoint = request.getUrl();
        try {
            org.asynchttpclient.Response response = null;
            for (int i = 1; i < maxAuthRetries; i++) {
                // execute request and wait for completion until default REQUEST_TIMEOUT_MS exceeded
                if (!this.asyncHttpClient.isClosed()) {
                    response = this.asyncHttpClient.executeRequest(request).get();
                } else {
                    logger.warn("failed to execute request {}, the connection already closed.", request);
                    if (throwOnFailure) {
                        throw new CometGeneralException("failed to execute request, the connection already closed.");
                    }
                    return Optional.empty();
                }

                if (!ConnectionUtils.isResponseSuccessful(response.getStatusCode())) {
                    // request attempt failed
                    if (i < maxAuthRetries - 1) {
                        logger.debug("for endpoint {} response {}, retrying\n", endpoint, response.getStatusText());
                        Thread.sleep((2 ^ i) * 1000L);
                    } else {
                        logger.error("for endpoint {} response {}, last retry failed\n",
                                endpoint, response.getStatusText());
                        if (throwOnFailure) {
                            String body = response.hasResponseBody() ? response.getResponseBody() : RESPONSE_NO_BODY;
                            throw new CometGeneralException("failed to call: " + endpoint + ", response status: "
                                    + response.getStatusCode() + ", body: " + body);
                        }
                    }
                } else {
                    if (logger.isDebugEnabled()) {
                        logger.debug("for endpoint {} got response {}\n", endpoint, response.getResponseBody());
                    }
                    break;
                }
            }

            if (response == null || !response.hasResponseBody()) {
                return Optional.empty();
            }
            return Optional.of(response.getResponseBody());
        } catch (Throwable e) {
            logger.error("Failed to execute request: " + request, e);
            if (throwOnFailure) {
                throw new CometGeneralException("failed to execute request, unknown error", e);
            }
            return Optional.empty();
        }
    }

    private Function<Response, Response> getDebugLogResponse(@NonNull String endpoint) {
        return new ConnectionUtils.DebugLogResponse(this.logger, endpoint);
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
