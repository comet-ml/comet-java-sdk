package ml.comet.experiment.impl.http;

import lombok.NonNull;
import lombok.Value;
import ml.comet.experiment.exception.CometApiException;
import ml.comet.experiment.exception.CometGeneralException;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.rest.CometWebJavaSdkException;
import org.asynchttpclient.AsyncCompletionHandlerBase;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.AsyncHttpClientConfig;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.HttpResponseBodyPart;
import org.asynchttpclient.HttpResponseStatus;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ml.comet.experiment.impl.http.ConnectionUtils.createGetRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostByteArrayRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostFileRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostFormRequest;
import static ml.comet.experiment.impl.http.ConnectionUtils.createPostJsonRequest;
import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

/**
 * Represents connection with the CometML server. Provides utility methods to send
 * various data records to the server.
 *
 * <p>Make sure to close this connection to avoid resources leak.
 */
@Value
public class Connection implements Closeable {
    // The default read timeout in milliseconds
    public static final int READ_TIMEOUT_MS = 60 * 1000;
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
                .setReadTimeout(READ_TIMEOUT_MS)
                .setRequestTimeout(REQUEST_TIMEOUT_MS)
                .setShutdownTimeout(CONNECTION_SHUTDOWN_TIMEOUT_MS)
                .build();
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
        return sendGetWithRetries(endpoint, params, false);
    }

    /**
     * Allows sending synchronous GET request to the specified endpoint with given request parameters.
     * It will attempt to retry request if failed for the {@code maxAuthRetries} attempts.
     *
     * <p>If request failed the {@link CometApiException} will be thrown with related details if {@code throwOnFailure}
     * parameter set.
     *
     * @param endpoint       the request path of the endpoint
     * @param params         the map with request parameters.
     * @param throwOnFailure if {@code true} then {@link CometApiException} will be thrown on failure.
     *                       Otherwise, the empty {@link Optional} returned.
     * @return the {@link Optional} of response body or empty {@link Optional}.
     * @throws CometApiException if failed and {@code throwOnFailure} parameter set.
     */
    public Optional<String> sendGetWithRetries(
            @NonNull String endpoint, @NonNull Map<QueryParamName, String> params, boolean throwOnFailure)
            throws CometApiException {
        try {
            this.requestsInventory.incrementAndGet();
            return executeRequestSyncWithRetries(
                    createGetRequest(this.buildCometUrl(endpoint), params), throwOnFailure);
        } finally {
            this.requestsInventory.decrementAndGet();
        }
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
        try {
            this.requestsInventory.incrementAndGet();
            return executeRequestSyncWithRetries(createPostJsonRequest(json, url), throwOnFailure);
        } finally {
            this.requestsInventory.decrementAndGet();
        }
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
     * Allows downloading remote assets to the provided file.
     *
     * @param file     the {@link File} instance to collect received data.
     * @param endpoint the request path of the endpoint.
     * @param params   the map with request parameters.
     * @return the {@link ListenableFuture} which can be used to monitor status of the request execution.
     */
    public ListenableFuture<Response> downloadAsync(@NonNull File file,
                                                    @NonNull String endpoint,
                                                    @NonNull Map<QueryParamName, String> params) {
        Request request = createGetRequest(this.buildCometUrl(endpoint), params);

        AsyncFileDownloadHandler handler = new AsyncFileDownloadHandler(file, this.logger);
        try {
            handler.open();
        } catch (Throwable e) {
            this.logger.error("Failed to start download to the file {}", file.getPath(), e);
            // make sure to release resources
            handler.close();
            return new ListenableFuture.CompletedFailure<>(e);
        }
        return this.executeDownloadAsync(request, handler);
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
     * @throws IOException if an I/O error occurs.
     */
    public void waitAndClose(@NonNull Duration timeout) throws IOException {
        try {
            Awaitility
                    .await()
                    .atMost(timeout)
                    .pollInterval(100, TimeUnit.MILLISECONDS)
                    .untilAtomic(this.requestsInventory, is(lessThanOrEqualTo(0)));
        } catch (ConditionTimeoutException e) {
            getLogger().error(
                    String.format("Timeout exceeded while waiting for remaining requests to complete, "
                            + "remaining requests: %d", this.requestsInventory.get()), e);
        } finally {
            // close connection
            this.close();
        }
    }

    /**
     * Executes provided download request asynchronously.
     *
     * @param request  the request to be executed.
     * @param listener the {@link DownloadListener} to collect received bytes.
     * @return the {@link ListenableFuture} which can be used to check request status.
     */
    ListenableFuture<Response> executeDownloadAsync(@NonNull Request request, @NonNull DownloadListener listener) {
        return this.executeRequestAsync(request, listener);
    }

    /**
     * Executes provided request asynchronously.
     *
     * @param request the request to be executed.
     * @return the {@link ListenableFuture} which can be used to check request status.
     */
    ListenableFuture<Response> executeRequestAsync(@NonNull Request request) {
        return this.executeRequestAsync(request, null);
    }

    /**
     * Executes provided request asynchronously.
     *
     * @param request          the request to be executed.
     * @param downloadListener the {@link DownloadListener} to collect received bytes.
     * @return the {@link ListenableFuture} which can be used to check request status.
     */
    ListenableFuture<Response> executeRequestAsync(@NonNull Request request,
                                                   DownloadListener downloadListener) {
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
                new AsyncCompletionInventoryHandler(this.requestsInventory, this.logger, endpoint, downloadListener));
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
        org.asynchttpclient.Response response = null;
        for (int i = 1; i < this.maxAuthRetries; i++) {
            if (this.asyncHttpClient.isClosed()) {
                this.logger.warn("failed to execute request {}, the connection already closed.", request);
                if (throwOnFailure) {
                    throw new CometApiException("failed to execute request, the connection already closed.");
                }
                return Optional.empty();
            }

            int statusCode = 0;
            try {
                // execute request and wait for completion until default REQUEST_TIMEOUT_MS exceeded
                response = this.asyncHttpClient.executeRequest(request).get();
                statusCode = response.getStatusCode();

                // check status code for possible errors
                ConnectionUtils.checkResponseStatus(response);

                // success - log debug and continue with result
                if (this.logger.isDebugEnabled()) {
                    this.logger.debug("for endpoint {} got response {}", endpoint, response.getResponseBody());
                }
                break;

            } catch (CometApiException apiException) {
                // connection attempt failed - check if to retry
                String body = response != null ? response.getStatusText() : RESPONSE_NO_BODY;
                if (i < this.maxAuthRetries - 1) {
                    // sleep for a while and repeat
                    this.logger.debug("for endpoint {} got response {}, retrying", endpoint, body);
                    try {
                        Thread.sleep((2 ^ i) * 1000L);
                    } catch (InterruptedException ignore) {
                        this.logger.error("Interrupted while sleeping");
                    }
                } else {
                    // maximal number of attempts exceeded - throw or return
                    this.logger.error(
                            "For endpoint {} got the response '{}', the last retry failed from {} attempts",
                            endpoint, body, this.maxAuthRetries);
                    if (throwOnFailure) {
                        throw apiException;
                    }
                    return Optional.empty();
                }
            } catch (CometWebJavaSdkException ex) {
                // remote endpoint signalled processing error - throw or return
                this.logger.error("Failed to execute request: {}, remote endpoint raised error", request, ex);
                if (throwOnFailure) {
                    throw new CometApiException(statusCode, ex.getMessage(), ex.getSdkErrorCode());
                }
                return Optional.empty();
            } catch (Throwable e) {
                // unexpected error - throw or return
                this.logger.error("Failed to execute request: {}, unexpected error", request, e);
                if (throwOnFailure) {
                    throw new CometApiException("failed to execute request, unexpected error", e);
                }
                return Optional.empty();
            }
        }

        if (response == null) {
            return Optional.empty();
        } else {
            return Optional.of(response.getResponseBody());
        }
    }

    private String buildCometUrl(String endpoint) {
        return this.cometBaseUrl + endpoint;
    }

    /**
     * The request completion listener to be used to maintain the current requests' inventory status.
     */
    static final class AsyncCompletionInventoryHandler extends AsyncCompletionHandlerBase {
        final AtomicInteger requestInventory;
        final Logger logger;
        final String endpoint;
        DownloadListener downloadListener;
        UploadListener uploadListener;
        HttpResponseStatus status;

        AsyncCompletionInventoryHandler(AtomicInteger inventory, Logger logger, String endpoint) {
            this.requestInventory = inventory;
            this.logger = logger;
            this.endpoint = endpoint;
        }

        AsyncCompletionInventoryHandler(AtomicInteger inventory, Logger logger, String endpoint,
                                        DownloadListener downloadListener) {
            this(inventory, logger, endpoint);
            this.downloadListener = downloadListener;
        }

        AsyncCompletionInventoryHandler(AtomicInteger inventory, Logger logger, String endpoint,
                                        UploadListener uploadListener) {
            this(inventory, logger, endpoint);
            this.uploadListener = uploadListener;
        }

        @Override
        public State onStatusReceived(HttpResponseStatus status) throws Exception {
            this.status = status;
            return super.onStatusReceived(status);
        }

        @Override
        public Response onCompleted(Response response) {
            // check response status and throw exception if failed
            try {
                ConnectionUtils.checkResponseStatus(response);
                // decrease inventory only after check passed,
                // if it was not passed it will be decreased in onThrowable()
                this.decreaseInventory();
            } catch (CometWebJavaSdkException ex) {
                throw new CometApiException(response.getStatusCode(), ex.getMessage(), ex.getSdkErrorCode());
            } finally {
                // make sure to notify all registered listeners
                this.fireOnEnd();
            }
            return response;
        }

        @Override
        public State onBodyPartReceived(final HttpResponseBodyPart content) throws Exception {
            if (this.downloadListener != null && this.status.getStatusCode() == 200) {
                try {
                    this.downloadListener.onBytesReceived(content.getBodyPartBytes());
                } catch (Throwable t) {
                    this.downloadListener.onThrowable(t);
                    throw t;
                }
            }
            return super.onBodyPartReceived(content);
        }

        @Override
        public State onContentWriteProgress(long amount, long current, long total) {
            if (this.uploadListener != null) {
                try {
                    this.uploadListener.onBytesSent(amount, current, total);
                } catch (Throwable t) {
                    this.uploadListener.onThrowable(t);
                }
            }
            return super.onContentWriteProgress(amount, current, total);
        }

        @Override
        public void onThrowable(Throwable t) {
            // decrease inventory
            this.decreaseInventory();
            this.logger.error("failed to execute request to the endpoint {}", this.endpoint, t);

            this.fireOnThrowable(t);
        }

        private void decreaseInventory() {
            this.requestInventory.decrementAndGet();
        }

        private void fireOnEnd() {
            if (this.downloadListener != null) {
                try {
                    this.downloadListener.onRequestResponseCompleted();
                } catch (Throwable t) {
                    this.downloadListener.onThrowable(t);
                }
            }
            if (this.uploadListener != null) {
                try {
                    this.uploadListener.onRequestResponseCompleted();
                } catch (Throwable t) {
                    this.uploadListener.onThrowable(t);
                }
            }
        }

        private void fireOnThrowable(Throwable t) {
            if (this.downloadListener != null) {
                try {
                    this.downloadListener.onThrowable(t);
                } catch (Throwable t2) {
                    logger.warn("downloadListener.onThrowable", t2);
                }
            }
            if (this.uploadListener != null) {
                try {
                    this.uploadListener.onThrowable(t);
                } catch (Throwable t2) {
                    logger.warn("uploadListener.onThrowable", t2);
                }
            }
        }
    }

    /**
     * The handler to manage downloading to the file.
     */
    static final class AsyncFileDownloadHandler implements DownloadListener {

        final File outFile;
        final Logger logger;
        RandomAccessFile file;

        AsyncFileDownloadHandler(File file, Logger logger) {
            this.outFile = file;
            this.logger = logger;
        }

        void open() throws IOException {
            this.file = new RandomAccessFile(this.outFile, "rw");
            if (Files.exists(this.outFile.toPath())) {
                // truncate existing file
                this.file.setLength(0);
            }
        }

        @Override
        public void onBytesReceived(byte[] bytes) throws IOException {
            try {
                this.file.seek(this.file.length());
                this.file.write(bytes);
            } catch (IOException e) {
                this.logger.error("Failed to write received bytes to the file {}", this.outFile.getPath(), e);
                throw e;
            }
        }

        @Override
        public void onRequestResponseCompleted() {
            this.close();
        }

        @Override
        public void onThrowable(Throwable t) {
            this.logger.error("Failed to download to the file {}", this.outFile.getPath(), t);
            this.close();
        }

        void close() {
            try {
                if (this.file != null) {
                    this.file.close();
                }
            } catch (IOException e) {
                this.logger.error("Failed to close the download file {}", this.outFile.getPath(), e);
            }
        }
    }
}
