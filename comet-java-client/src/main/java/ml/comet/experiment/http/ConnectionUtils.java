package ml.comet.experiment.http;

import lombok.NonNull;
import lombok.Value;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.request.body.generator.ByteArrayBodyGenerator;
import org.asynchttpclient.request.body.multipart.ByteArrayPart;
import org.asynchttpclient.request.body.multipart.FilePart;
import org.asynchttpclient.util.HttpConstants;
import org.slf4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.function.Function;

/**
 * Collection of the utilities used by <code>Connection</code>.
 */
public class ConnectionUtils {
    static final String FILE = "file";
    static final String FORM_MIME_TYPE = "multipart/form-data";
    static final String JSON_MIME_TYPE = "application/json";

    /**
     * Creates GET request to the given endpoint with specified query parameters.
     *
     * @param url    the endpoint URL
     * @param params the request parameters.
     * @return the GET request.
     */
    static Request createGetRequest(@NonNull String url, Map<String, String> params) {
        RequestBuilder builder = new RequestBuilder();
        builder.setUrl(url);
        if (params != null) {
            params.forEach(builder::addQueryParam);
        }

        return builder.build();
    }

    /**
     * Creates POST request from given file to the specified endpoint.
     *
     * @param file   the file to be included in the body parts.
     * @param url    the URL of the endpoint.
     * @param params the query parameters of the request.
     * @return the POST request with specified file.
     */
    static Request createPostFileRequest(@NonNull File file, @NonNull String url, Map<String, String> params) {
        RequestBuilder builder = new RequestBuilder(HttpConstants.Methods.POST);
        builder
                .setUrl(url)
                .setHeader("Content-Type", FORM_MIME_TYPE)
                .addBodyPart(new FilePart(FILE, file, FORM_MIME_TYPE));
        if (params != null) {
            params.forEach(builder::addQueryParam);
        }

        return builder.build();
    }

    /**
     * Creates POST request from given byte array to the specified endpoint.
     *
     * @param bytes  the bytes array to include into request.
     * @param url    the URL of the endpoint.
     * @param params the query parameters of the request.
     * @return the POST request with specified byte array as body part.
     */
    static Request createPostByteArrayRequest(byte[] bytes, @NonNull String url, Map<String, String> params) {
        RequestBuilder builder = new RequestBuilder(HttpConstants.Methods.POST);
        builder
                .setUrl(url)
                .setHeader("Content-Type", FORM_MIME_TYPE)
                .addBodyPart(new ByteArrayPart(FILE, bytes, FORM_MIME_TYPE));
        if (params != null) {
            params.forEach(builder::addQueryParam);
        }

        return builder.build();
    }

    /**
     * Creates Request with specified body for given url.
     */
    static Request createPostJsonRequest(@NonNull String body, @NonNull String url) {
        return new RequestBuilder()
                .setUrl(url)
                .setHeader("Content-Type", JSON_MIME_TYPE)
                .setBody(new ByteArrayBodyGenerator(body.getBytes()))
                .setMethod(HttpConstants.Methods.POST)
                .build();
    }

    /**
     * Returns true if the status code is in [200..300), which means the request was successfully received,
     * understood, and accepted.
     */
    static boolean isResponseSuccessful(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * The function allowing to debug response.
     */
    @Value
    public static class DebugLogResponse implements Function<Response, Response> {
        Logger logger;
        String endpoint;

        @Override
        public Response apply(Response response) {
            // log response for debug purposes
            if (ConnectionUtils.isResponseSuccessful(response.getStatusCode())) {
                logger.debug("for endpoint {} response {}\n", endpoint, response.getResponseBody());
            } else {
                logger.error("for endpoint {} response {}\n", endpoint, response.getStatusText());
            }
            return response;
        }
    }
}
