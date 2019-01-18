package com.comet.experiment;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Response;
import com.ning.http.client.multipart.FilePart;
import org.apache.http.client.utils.URIBuilder;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Connection {
    private static Logger logger = Logger.getLogger(Connection.class.getName());
    static AsyncHttpClient asyncHttpClient = new AsyncHttpClient();
    static ExecutorService executorService = Executors.newSingleThreadExecutor();
    String restApiKey;
    String cometBaseUrl = "https://staging.comet.ml/api/rest/v1/write";

    protected Connection(String restApiKey) {
        this.restApiKey = restApiKey;
    }

    public Optional<String> sendPost(String body, String endpoint) {
        try {
            String url = cometBaseUrl + endpoint;
            Response response = asyncHttpClient
                    .preparePost(url)
                    .setBody(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", restApiKey)
                    .execute().get();

            logger.info(String.format("for body %s and endpoint %s response %s\n", body, endpoint, response.getResponseBody()));
            return Optional.ofNullable(response.getResponseBody());
        } catch (Exception e) {
            logger.severe("Failed to post to " + endpoint);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    public void sendPostAsync(String body, String endpoint) {
        try {
            String url = cometBaseUrl + endpoint;
            ListenableFuture<Response> future = asyncHttpClient
                    .preparePost(url)
                    .setBody(body)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", restApiKey)
                    .execute();
            future.addListener(new ResponseListener(body, endpoint, future), executorService);
        } catch (Exception e) {
            logger.severe("Failed to post to " + endpoint);
            e.printStackTrace();
        }
    }

    public Optional<String> sendPost(File file, String endpoint, Map<String, String> params) {
        try {
            String url = getUrl(cometBaseUrl + endpoint, params);
            Response response = asyncHttpClient
                    .preparePost(url)
                    .addBodyPart(new FilePart("file", file))
                    .addHeader("Content-Type", "multipart/form-data")
                    .addHeader("Authorization", restApiKey)
                    .execute().get();

            return Optional.ofNullable(response.getResponseBody());
        } catch (Exception e) {
            logger.severe("Failed to post to " + endpoint);
            e.printStackTrace();
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
}

class ResponseListener implements Runnable {
    private static Logger logger = Logger.getLogger(ResponseListener.class.getName());
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
        if (!endpoint.equals("/output")) { // Prevent infinite loop of log lines
            try {
                Response response = future.get();
                logger.info(String.format("for body %s and endpoint %s response %s\n", body, endpoint, response.getResponseBody()));
            } catch (Exception ex) {
                logger.severe("failed to get response for " + endpoint);
                ex.printStackTrace();
            }
        }
    }
}
