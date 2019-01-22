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

import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;

public class Connection {
    private Logger logger;
    static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    static ExecutorService executorService = Executors.newSingleThreadExecutor();
    Optional<String> apiKey;
    Optional<String> restApiKey;
    String cometBaseUrl;

    protected Connection(String cometBaseUrl, Optional<String> apiKey, Optional<String> restApiKey, Logger logger) {
        this.cometBaseUrl = cometBaseUrl;
        this.apiKey = apiKey;
        this.restApiKey = restApiKey;
        this.logger = logger;
    }

    public Optional<String> sendPost(String body, String endpoint) {
        try {
            String url = cometBaseUrl + endpoint;
            logger.debug("sending {} to {}", body, url);
            BoundRequestBuilder builder = asyncHttpClient
                    .preparePost(url)
                    .setBody(body)
                    .addHeader("Content-Type", "application/json");
            builder = addAuth(builder);
            Response response = builder .execute().get();

            logger.debug("for body %s and endpoint %s response %s\n", body, endpoint, response.getResponseBody());
            return Optional.ofNullable(response.getResponseBody());
        } catch (Exception e) {
            logger.error("Failed to post to " + endpoint);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void sendPostAsync(String body, String endpoint) {
        try {
            String url = cometBaseUrl + endpoint;
            BoundRequestBuilder builder = asyncHttpClient
                    .preparePost(url)
                    .setBody(body)
                    .addHeader("Content-Type", "application/json");
            builder = addAuth(builder);
            ListenableFuture<Response> future = builder .execute();
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
            String url = getUrl(cometBaseUrl + endpoint, params);
            BoundRequestBuilder builder = asyncHttpClient
                    .preparePost(url)
                    .addBodyPart(new FilePart("file", file))
                    .addHeader("Content-Type", "multipart/form-data");
            builder = addAuth(builder);
            Response response = builder .execute().get();

            return Optional.ofNullable(response.getResponseBody());
        } catch (Exception e) {
            logger.error("Failed to post to " + endpoint);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private BoundRequestBuilder addAuth(BoundRequestBuilder builderArg) {
        BoundRequestBuilder builder = builderArg;
        if (restApiKey.isPresent()) {
            System.out.println("Using rest api key: " + restApiKey);
            builder = builder.addHeader("Authorization", restApiKey.get());
        }
        if (apiKey.isPresent()) {
            System.out.println("Using api key: " + apiKey);
            builder = builder.addHeader("Comet-Sdk-Api", apiKey.get());
        }
        return builder;
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
                logger.debug(String.format("for body {} and endpoint {} response {}\n", body, endpoint, response.getResponseBody()));
            } catch (Exception ex) {
                logger.error("failed to get response for " + endpoint);
                ex.printStackTrace();
            }
        }
    }
}
