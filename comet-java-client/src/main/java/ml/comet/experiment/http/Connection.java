package ml.comet.experiment.http;

//import com.ning.http.client.AsyncHttpClient;
//import com.ning.http.client.ListenableFuture;
//import com.ning.http.client.Response;
//import com.ning.http.client.multipart.ByteArrayPart;
//import com.ning.http.client.multipart.FilePart;
//import com.ning.http.client.providers.netty.util.HttpUtils;

import lombok.Value;
import ml.comet.experiment.utils.JsonUtils;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Value
public class Connection {
    private static final OkHttpClient CLIENT = new OkHttpClient();
    private static final String COMET_SDK_API = "Comet-Sdk-Api";
    private static final MediaType MEDIA_TYPE_JSON = MediaType.get("application/json");
    private static final MediaType MEDIATYPE_FORM_DATA = MediaType.get("multipart/form-data");
    private static final String FILE = "file";
    String cometBaseUrl;
    String apiKey;
    Logger logger;
    int maxAuthRetries;

    public Optional<String> sendGet(String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        Request.Builder builder = new Request.Builder()
                .url(url);
        return executeRequestWithAuth(builder, url);
    }

    public Optional<String> sendPost(String body, String endpoint) {
        String url = cometBaseUrl + endpoint;
        logger.debug("sending {} to {}", body, url);
        Request.Builder builder = createPostJsonRequest(body, url);
        return executeRequestWithAuth(builder, url);
    }

    public Optional<String> sendPost(File file, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        logger.debug("sending POST file {} to {}", file.getName(), url);
        Request.Builder builder = createPostFileRequest(file, url);
        return executeRequestWithAuth(builder, url);
    }

    public Optional<String> sendPost(byte[] bytes, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        logger.debug("sending POST bytearray with length {} to {}", bytes.length, url);
        Request.Builder builder = createPostByteArrayRequest(bytes, url);
        return executeRequestWithAuth(builder, url);
    }

    public void sendPostAsync(Object payload, String endpoint) {
        sendPostAsync(JsonUtils.toJson(payload), endpoint);
    }

    public void sendPostAsync(String body, String endpoint) {
        String url = cometBaseUrl + endpoint;
        Request.Builder builder = createPostJsonRequest(body, url);
        executeRequestWithAuthAsync(builder);
    }

    public void sendPostAsync(File file, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        Request.Builder builder = createPostFileRequest(file, url);
        executeRequestWithAuthAsync(builder);
    }

    public void sendPostAsync(byte[] bytes, String endpoint, Map<String, String> params) {
        String url = getUrl(cometBaseUrl + endpoint, params);
        logger.debug("sending POST bytearray with length {} to {}", bytes.length, url);
        Request.Builder builder = createPostByteArrayRequest(bytes, url);
        executeRequestWithAuthAsync(builder);
    }

    private void executeRequestWithAuthAsync(Request.Builder requestBuilder) {
        requestBuilder.addHeader(COMET_SDK_API, apiKey);
        Request request = requestBuilder.build();
        CLIENT.newCall(request).enqueue(getAsyncCallback());
    }

    private Optional<String> executeRequestWithAuth(Request.Builder requestBuilder, String endpoint) {
        requestBuilder.addHeader(COMET_SDK_API, apiKey);
        Request request = requestBuilder.build();
        try {
            Response response = null;
            for (int i = 1; i < maxAuthRetries; i++) {
                response = CLIENT.newCall(request).execute();

                if (!response.isSuccessful()) {
                    if (i < maxAuthRetries - 1) {
                        logger.debug("for endpoint {} response {}, retrying\n", endpoint, response.body());
                        Thread.sleep((2 ^ i) * 1000L);
                    } else {
                        logger.error("for endpoint {} response {}, last retry failed\n", endpoint, response.body());
                    }
                } else {
                    logger.debug("for endpoint {} response {}\n", endpoint, response.body());
                    break;
                }
            }

            if (ObjectUtils.anyNull(response, response.body())) {
                return Optional.empty();
            }
            return Optional.of(response.body().string());
        } catch (Exception e) {
            logger.error("Failed to post to " + endpoint);
            e.printStackTrace();
            return Optional.empty();
        }
    }

    private Request.Builder createPostJsonRequest(String body, String url) {
        return new Request.Builder()
                .url(url)
                .post(RequestBody.create(body, MEDIA_TYPE_JSON));
    }

    private Request.Builder createPostFileRequest(File file, String url) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        builder.addFormDataPart(FILE, file.getName(), RequestBody.create(file, MultipartBody.FORM));
        return new Request.Builder()
                .url(url)
                .post(builder.build());
    }

    private Request.Builder createPostByteArrayRequest(byte[] bytes, String url) {
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        builder.addFormDataPart(FILE, FILE, RequestBody.create(bytes, MultipartBody.FORM));
        return new Request.Builder()
                .url(url)
                .post(builder.build());
    }

    private static String getUrl(String url, Map<String, String> params) {
        HttpUrl.Builder builder = HttpUrl.get(url).newBuilder();
        params.forEach(builder::addQueryParameter);
        return builder.build().toString();
    }

    private Callback getAsyncCallback() {
        return new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                logger.error("failed to get response for " + call.request().url());
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (response.isSuccessful()) {
                    logger.debug("for endpoint {} response {}\n", call.request().url(), response.body());
                } else {
                    logger.error("for endpoint {} response {}\n", call.request().url(), response.body());
                }
            }
        };
    }

}
