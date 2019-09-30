package com.comet.experiment;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import com.ning.http.client.multipart.FilePart;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.comet.experiment.Constants.WRITE;

public class Connection {
    private Logger logger;
    static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    static ExecutorService executorService = Executors.newSingleThreadExecutor();
    Optional<String> apiKey;
    Optional<String> restApiKey;
    String cometBaseUrl;
    int maxAuthRetries;

    protected Connection(String cometBaseUrl, Optional<String> apiKey, Optional<String> restApiKey, Logger logger, int maxAuthRetries) {
        this.cometBaseUrl = cometBaseUrl;
        this.apiKey = apiKey;
        this.restApiKey = restApiKey;
        this.logger = logger;
        this.maxAuthRetries = maxAuthRetries;
    }

    protected Connection(String cometBaseUrl, Optional<String> apiKey, Optional<String> restApiKey, Logger logger) {
        this.cometBaseUrl = cometBaseUrl;
        this.apiKey = apiKey;
        this.restApiKey = restApiKey;
        this.logger = logger;
        this.maxAuthRetries = 1;
    }

    public Optional<String> sendPost(String body, String endpoint) {
        try {
            String url = cometBaseUrl + WRITE + endpoint;
            logger.debug(String.format("sending {} to {}", body, url));
            Response response = null;
            for (int i = 1; i < maxAuthRetries; i++) {
                response = asyncHttpClient
                        .preparePost(url)
                        .setBody(body)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Comet-Sdk-Api", apiKey.get())
                        .execute().get();

                if (response.getStatusCode() != 200) {
                    if (i < maxAuthRetries) {
                        logger.debug(String.format("for body %s and endpoint %s response %s, retrying\n", body, endpoint, response.getResponseBody()));
                        Thread.sleep((2^i) * 1000);
                    } else {
                        logger.error(String.format("for body %s and endpoint %s response %s, last retry failed\n", body, endpoint, response.getResponseBody()));
                    }
                } else {
                    logger.debug(String.format("for body %s and endpoint %s response %s\n", body, endpoint, response.getResponseBody()));
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

    public void sendPostAsync(String body, String endpoint) {
        try {
            String url = cometBaseUrl + WRITE + endpoint;
            ListenableFuture<Response> future = asyncHttpClient
                    .preparePost(url)
                    .setBody(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Comet-Sdk-Api", apiKey.get())
                    .execute();
            if (!endpoint.equals("/output")) {
                future.addListener(new ResponseListener(body, endpoint, future), executorService);
            }
        } catch (Exception e) {
            logger.error("Failed to post to " + endpoint);
            e.printStackTrace();
        }
    }

    public Optional<String> sendPost(File file, String endpoint, Map<String, String> params) {
        try {
            String url = getUrl(cometBaseUrl + WRITE + endpoint, params);
            Response response = asyncHttpClient
                    .preparePost(url)
                    .addBodyPart(new FilePart("file", file))
                    .addHeader("Content-Type", "multipart/form-data")
                    .addHeader("Comet-Sdk-Api", apiKey.get())
                    .execute().get();

            if (response.getStatusCode() != 200) {
                logger.error(String.format("endpoint %s response %s", endpoint, response.getResponseBody()));
            } else {
                logger.debug(String.format("endpoint %s response %s", endpoint, response.getResponseBody()));
            }
            return Optional.ofNullable(response.getResponseBody());
        } catch (Exception e) {
            logger.error("Failed to post to " + endpoint);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public Optional<String> sendGet(String endpoint, Map<String, String> params) {
        try {
            String url = getUrl(cometBaseUrl + endpoint, params);
            AsyncHttpClient.BoundRequestBuilder builder = asyncHttpClient
                    .prepareGet(url)
                    .addHeader("Content-Type", "application/json");
            if (apiKey.isPresent()) {
                builder.addHeader("Comet-Sdk-Api", apiKey.get());
            } else {
                builder.addHeader("Authorization", restApiKey.get());
            }

            Response response = builder.execute().get();

            if (response.getStatusCode() != 200) {
                logger.error(String.format("endpoint %s response %s", endpoint, response.getResponseBody()));
            } else {
                logger.debug(String.format("endpoint %s response %s", endpoint, response.getResponseBody()));
            }
            return Optional.ofNullable(response.getResponseBody());
        } catch (Exception ex) {
            logger.error("Failed to get from " + endpoint);
            ex.printStackTrace();
            return Optional.empty();
        }
    }

    private static String getUrl(String url, Map<String, String> params) {
        try {
            URIBuilder builder = new URIBuilder(url);
            params.forEach((k, v) -> builder.addParameter(k, v));
            return builder.build().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("failed to create URL ", e);
        }
    }

    class ResponseListener implements Runnable {
        private String body;
        private String endpoint;
        private ListenableFuture<Response> future;
        public ResponseListener(String body, String endpoint, ListenableFuture<Response> future) {
            this.body = body;
            this.endpoint = endpoint;
            this.future = future;
        }

        @Override
        public void run() {
            try {
                Response response = future.get();
                if (response.getStatusCode() != 200){
                    logger.error(String.format("for body %s and endpoint %s response %s\n", body, endpoint, response.getResponseBody()));
                } else {
                    logger.debug(String.format("for body %s and endpoint %s response %s\n", body, endpoint, response.getResponseBody()));
                }
            } catch (Exception ex) {
                logger.error("failed to get response for " + endpoint);
                ex.printStackTrace();
            }
        }
    }
}
