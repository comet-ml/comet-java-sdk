package ml.comet.experiment.http;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import com.ning.http.client.multipart.ByteArrayPart;
import com.ning.http.client.multipart.FilePart;
import lombok.Value;
import ml.comet.experiment.utils.JsonUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Value
public class Connection {
    private static final AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_APPLICATION_JSON = "application/json";
    private static final String CONTENT_MULTIPART_FORM_DATA = "multipart/form-data";
    private static final String COMET_SDK_API = "Comet-Sdk-Api";
    String cometBaseUrl;
    String apiKey;
    Logger logger;
    int maxAuthRetries;

    public Optional<String> sendGet(String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        AsyncHttpClient.BoundRequestBuilder builder = asyncHttpClient
                .prepareGet(url)
                .addHeader(CONTENT_TYPE, CONTENT_APPLICATION_JSON);
        return executeRequestWithAuth(builder, url);
    }

    public Optional<String> sendPost(String body, String endpoint) {
        String url = cometBaseUrl + endpoint;
        logger.debug("sending {} to {}", body, url);
        AsyncHttpClient.BoundRequestBuilder requestBuilder = asyncHttpClient
                .preparePost(url)
                .setBody(body)
                .addHeader(CONTENT_TYPE, CONTENT_APPLICATION_JSON);
        return executeRequestWithAuth(requestBuilder, url);
    }

    public Optional<String> sendPost(File file, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        logger.debug("sending POST file {} to {}", file.getName(), url);
        AsyncHttpClient.BoundRequestBuilder requestBuilder = createPostFileRequest(file, url);
        return executeRequestWithAuth(requestBuilder, url);
    }

    public Optional<String> sendPost(byte[] bytes, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        logger.debug("sending POST bytearray with length {} to {}", bytes.length, url);
        AsyncHttpClient.BoundRequestBuilder requestBuilder = createPostByteArrayRequest(bytes, url);
        return executeRequestWithAuth(requestBuilder, url);
    }

    public void sendPostAsync(Object payload, String endpoint) {
        sendPostAsync(JsonUtils.toJson(payload), endpoint);
    }

    public void sendPostAsync(String body, String endpoint) {
        String url = cometBaseUrl + endpoint;
        AsyncHttpClient.BoundRequestBuilder requestBuilder = asyncHttpClient
                .preparePost(url)
                .setBody(body)
                .addHeader(CONTENT_TYPE, CONTENT_APPLICATION_JSON);
        executeRequestWithAuthAsync(requestBuilder, url);
    }

    public void sendPostAsync(File file, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        AsyncHttpClient.BoundRequestBuilder requestBuilder = createPostFileRequest(file, url);
        executeRequestWithAuthAsync(requestBuilder, url);
    }

    public void sendPostAsync(byte[] bytes, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        logger.debug("sending POST bytearray with length {} to {}", bytes.length, url);
        AsyncHttpClient.BoundRequestBuilder requestBuilder = createPostByteArrayRequest(bytes, url);
        executeRequestWithAuthAsync(requestBuilder, url);
    }

    private void executeRequestWithAuthAsync(AsyncHttpClient.BoundRequestBuilder requestBuilder, String endpoint) {
        requestBuilder.addHeader(COMET_SDK_API, apiKey);
        try {
            ListenableFuture<Response> future = requestBuilder.execute();
            if (!endpoint.equals("/output")) {
                future.addListener(new ResponseListener(endpoint, future), executorService);
            }
        } catch (Exception e) {
            logger.error("Failed to post to " + endpoint);
            e.printStackTrace();
        }
    }

    private Optional<String> executeRequestWithAuth(AsyncHttpClient.BoundRequestBuilder requestBuilder, String endpoint) {
        requestBuilder.addHeader(COMET_SDK_API, apiKey);
        try {
            Response response = null;
            for (int i = 1; i < maxAuthRetries; i++) {
                response = requestBuilder.execute().get();

                if (response.getStatusCode() != 200) {
                    if (i < maxAuthRetries - 1) {
                        logger.debug("for endpoint {} response {}, retrying\n", endpoint, response.getResponseBody());
                        Thread.sleep((2 ^ i) * 1000L);
                    } else {
                        logger.error("for endpoint {} response {}, last retry failed\n", endpoint, response.getResponseBody());
                    }
                } else {
                    logger.debug("for endpoint {} response {}\n", endpoint, response.getResponseBody());
                    break;
                }
            }

            if (response == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(response.getResponseBody());
        } catch (Exception e) {
            logger.error("Failed to post to " + endpoint);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private AsyncHttpClient.BoundRequestBuilder createPostByteArrayRequest(byte[] bytes, String url) {
        return asyncHttpClient
                .preparePost(url)
                .addBodyPart(new ByteArrayPart("file", bytes))
                .addHeader(CONTENT_TYPE, CONTENT_MULTIPART_FORM_DATA);
    }

    private AsyncHttpClient.BoundRequestBuilder createPostFileRequest(File file, String url) {
        return asyncHttpClient
                .preparePost(url)
                .addBodyPart(new FilePart("file", file))
                .addHeader(CONTENT_TYPE, CONTENT_MULTIPART_FORM_DATA);
    }

    private static String getUrl(String url, Map<String, String> params) {
        try {
            URIBuilder builder = new URIBuilder(url);
            params.forEach(builder::addParameter);
            return builder.build().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("failed to create URL ", e);
        }
    }

    @Value
    class ResponseListener implements Runnable {
        String endpoint;
        ListenableFuture<Response> future;

        @Override
        public void run() {
            try {
                Response response = future.get();
                if (response.getStatusCode() != 200) {
                    logger.error("for endpoint {} response {}\n", endpoint, response.getResponseBody());
                } else {
                    logger.debug("for endpoint {} response {}\n", endpoint, response.getResponseBody());
                }
            } catch (Exception ex) {
                logger.error("failed to get response for " + endpoint);
                ex.printStackTrace();
            }
        }
    }
}
