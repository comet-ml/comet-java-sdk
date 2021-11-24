package ml.comet.experiment.impl.http;

import lombok.NonNull;
import lombok.Value;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import org.asynchttpclient.Request;
import org.asynchttpclient.RequestBuilder;
import org.asynchttpclient.Response;
import org.asynchttpclient.request.body.generator.ByteArrayBodyGenerator;
import org.asynchttpclient.request.body.multipart.ByteArrayPart;
import org.asynchttpclient.request.body.multipart.FileLikePart;
import org.asynchttpclient.request.body.multipart.FilePart;
import org.asynchttpclient.request.body.multipart.Part;
import org.asynchttpclient.request.body.multipart.StringPart;
import org.asynchttpclient.util.HttpConstants;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static ml.comet.experiment.impl.constants.FormParamName.FILE;

/**
 * Collection of the utilities used by <code>Connection</code>.
 */
public class ConnectionUtils {
    static final String MIME_MULTIPART_FORM_DATA = "multipart/form-data";
    static final String MIME_APPLICATION_JSON = "application/json";
    static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";

    /**
     * Creates GET request to the given endpoint with specified query parameters.
     *
     * @param url    the endpoint URL
     * @param params the request parameters.
     * @return the GET request.
     */
    static Request createGetRequest(@NonNull String url, Map<QueryParamName, String> params) {
        return createRequestBuilder(HttpConstants.Methods.GET, params)
                .setUrl(url)
                .build();
    }

    /**
     * Creates POST request from given file to the specified endpoint.
     *
     * @param file       the file to be included in the body parts.
     * @param url        the URL of the endpoint.
     * @param params     the query parameters of the request.
     * @param formParams the form parameters to be added.
     * @return the POST request with specified file.
     */
    static Request createPostFileRequest(@NonNull File file, @NonNull String url,
                                         Map<QueryParamName, String> params,
                                         Map<FormParamName, Object> formParams) {
        return createMultipartRequestBuilder(
                new FilePart(FILE.paramName(), file), params, formParams)
                .setUrl(url)
                .build();
    }

    /**
     * Creates POST request from given byte array to the specified endpoint.
     *
     * @param bytes      the bytes array to include into request.
     * @param url        the URL of the endpoint.
     * @param params     the query parameters of the request.
     * @param formParams the form parameters to be added
     * @return the POST request with specified byte array as body part.
     */
    static Request createPostByteArrayRequest(byte[] bytes, @NonNull String url,
                                              Map<QueryParamName, String> params,
                                              Map<FormParamName, Object> formParams) {
        return createMultipartRequestBuilder(
                new ByteArrayPart(FILE.paramName(), bytes, MIME_APPLICATION_OCTET_STREAM), params, formParams)
                .setUrl(url)
                .build();
    }

    /**
     * Creates Request with specified body for given url.
     */
    static Request createPostJsonRequest(@NonNull String body, @NonNull String url) {
        return new RequestBuilder()
                .setUrl(url)
                .setHeader("Content-Type", MIME_APPLICATION_JSON)
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
     * Creates request builder configured with common parameters.
     *
     * @param httpMethod the HTTP method.
     * @param params     the query parameters to be added to the request builder.
     * @return the pre-configured request builder.
     */
    static RequestBuilder createRequestBuilder(@NonNull String httpMethod, Map<QueryParamName, String> params) {
        RequestBuilder builder = new RequestBuilder(httpMethod);
        if (params != null) {
            params.forEach((k, v) -> builder.addQueryParam(k.paramName(), v));
        }
        return builder;
    }

    /**
     * Creates multipart request builder using provided parameters.
     *
     * @param fileLikePart the file like part to be added
     * @param params       the query parameters to be added
     * @param formParams   the form parameters
     * @return the pre-configured request builder.
     */
    static RequestBuilder createMultipartRequestBuilder(
            @NonNull FileLikePart fileLikePart, Map<QueryParamName, String> params,
            Map<FormParamName, Object> formParams) {
        RequestBuilder builder = createRequestBuilder(HttpConstants.Methods.POST, params);
        List<Part> parts = new ArrayList<>();
        parts.add(fileLikePart);
        if (formParams != null) {
            formParams.forEach((k, v) -> parts.add(createPart(k.paramName(), v)));
        }
        builder.setBodyParts(parts);
        return builder;
    }

    private static Part createPart(String name, @NonNull Object value) {
        return new StringPart(name, value.toString());
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
