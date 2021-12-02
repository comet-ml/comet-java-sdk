package ml.comet.experiment.impl.http;

import ml.comet.experiment.impl.constants.ApiEndpoints;
import ml.comet.experiment.impl.constants.FormParamName;
import ml.comet.experiment.impl.constants.QueryParamName;
import ml.comet.experiment.impl.utils.JsonUtils;
import ml.comet.experiment.impl.utils.TestUtils;
import ml.comet.experiment.model.HtmlRest;
import org.asynchttpclient.Request;
import org.asynchttpclient.request.body.multipart.ByteArrayPart;
import org.asynchttpclient.request.body.multipart.FilePart;
import org.asynchttpclient.request.body.multipart.StringPart;
import org.asynchttpclient.util.HttpConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_JSON;
import static io.netty.handler.codec.http.HttpHeaderValues.APPLICATION_OCTET_STREAM;
import static io.netty.handler.codec.http.HttpHeaderValues.MULTIPART_FORM_DATA;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static ml.comet.experiment.impl.constants.FormParamName.FILE;
import static ml.comet.experiment.impl.constants.FormParamName.METADATA;
import static ml.comet.experiment.impl.constants.QueryParamName.EXPERIMENT_KEY;
import static ml.comet.experiment.impl.constants.QueryParamName.OVERWRITE;
import static org.asynchttpclient.util.HttpConstants.Methods.POST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnectionUtilsTest {

    private static final String SOME_TEXT_FILE_NAME = "someTextFile.txt";

    @Test
    public void testCreateGetRequest() {
        String url = "http://test.com/get";
        HashMap<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, "someValue");
            put(OVERWRITE, Boolean.toString(true));
        }};
        Request r = ConnectionUtils.createGetRequest(url, params);

        this.validateRequest(r, url, params, HttpConstants.Methods.GET, null);
    }

    @Test
    public void testCreatePostFileRequest() {
        // Create test data
        //
        String url = "http://test.com" + ApiEndpoints.ADD_ASSET;
        HashMap<QueryParamName, String> queryParams = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, "someValue");
            put(OVERWRITE, Boolean.toString(true));
        }};
        HashMap<FormParamName, Object> formParams = new HashMap<FormParamName, Object>() {{
            put(METADATA, "some string");
        }};
        File file = TestUtils.getFile(SOME_TEXT_FILE_NAME);
        assertNotNull(file, "test file not found");

        // Create request
        //
        Request r = ConnectionUtils.createPostFileRequest(file, url, queryParams, formParams);
        this.validateRequest(r, url, queryParams, POST, MULTIPART_FORM_DATA.toString());

        // Check body parts
        //
        assertEquals(2, r.getBodyParts().size(), "wrong number of body parts");
        // file part
        FilePart filePart = (FilePart) r.getBodyParts().get(0);
        assertEquals(FILE.paramName(), filePart.getName(), "wrong name");
        assertEquals(TEXT_PLAIN.toString(), filePart.getContentType(), "wrong content type");
        assertEquals(file, filePart.getFile(), "wrong file");
        // metadata part
        StringPart stringPart = (StringPart) r.getBodyParts().get(1);
        assertEquals(METADATA.paramName(), stringPart.getName(), "wrong name");
        assertEquals(formParams.get(METADATA), stringPart.getValue(), "wrong value");
    }

    @Test
    public void testCreatePostByteArrayRequest() {
        // Create test data
        //
        String url = "http://test.com" + ApiEndpoints.ADD_ASSET;
        HashMap<QueryParamName, String> params = new HashMap<QueryParamName, String>() {{
            put(EXPERIMENT_KEY, "someValue");
            put(OVERWRITE, Boolean.toString(true));
        }};
        HashMap<FormParamName, Object> formParams = new HashMap<FormParamName, Object>() {{
            put(METADATA, "some string");
        }};
        byte[] data = "The test byte data".getBytes();

        // Create request
        //
        Request r = ConnectionUtils.createPostByteArrayRequest(data, url, params, formParams);
        this.validateRequest(r, url, params, POST, MULTIPART_FORM_DATA.toString());

        // Check body parts
        //
        assertEquals(2, r.getBodyParts().size(), "wrong number of body parts");
        // data part
        ByteArrayPart part = (ByteArrayPart) r.getBodyParts().get(0);
        assertEquals(FILE.paramName(), part.getName(), "wrong name");
        assertEquals(APPLICATION_OCTET_STREAM.toString(), part.getContentType(), "wrong content type");
        assertEquals(data, part.getBytes(), "wrong data array");
        // metadata part
        StringPart stringPart = (StringPart) r.getBodyParts().get(1);
        assertEquals(METADATA.paramName(), stringPart.getName(), "wrong name");
        assertEquals(formParams.get(METADATA), stringPart.getValue(), "wrong value");
    }

    @Test
    public void testCreatePostJsonRequest() {
        String url = "http://test.com" + ApiEndpoints.ADD_ASSET;
        HtmlRest html = new HtmlRest("<html><body></body></html", "test_key",
                false, System.currentTimeMillis());
        String json = JsonUtils.toJson(html);

        Request r = ConnectionUtils.createPostJsonRequest(json, url);
        this.validateRequest(r, url, null, POST, APPLICATION_JSON.toString());

        assertEquals(json.length(), r.getBodyGenerator().createBody().getContentLength(), "wrong body");
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 202, 299})
    public void testIsResponseSuccessfulTrue(int statusCode) {
        assertTrue(ConnectionUtils.isResponseSuccessful(statusCode));
    }

    @ParameterizedTest
    @ValueSource(ints = {300, 401, 404, 500})
    public void testIsResponseSuccessfulFalse(int statusCode) {
        assertFalse(ConnectionUtils.isResponseSuccessful(statusCode));
    }

    private void validateRequest(Request r, String url, HashMap<QueryParamName, String> params,
                                 String method, String contentType) {
        StringBuilder buf = new StringBuilder(url);
        if (params != null) {
            buf.append("?");
            params.forEach((k, v) -> buf.append(k.paramName()).append("=").append(v).append("&"));
            buf.deleteCharAt(buf.length() - 1); // remove last ampersand
        }

        URI expected = URI.create(buf.toString());
        assertEquals(expected, URI.create(r.getUrl()));
        assertEquals(method, r.getMethod(), "wrong HTTP method");
        if (contentType != null) {
            assertEquals(contentType, r.getHeaders().get(CONTENT_TYPE.toString()), "wrong content type");
        }
    }
}
