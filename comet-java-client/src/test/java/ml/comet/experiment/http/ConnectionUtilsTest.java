package ml.comet.experiment.http;

import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.utils.JsonUtils;
import ml.comet.experiment.utils.TestUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.request.body.multipart.ByteArrayPart;
import org.asynchttpclient.request.body.multipart.FilePart;
import org.asynchttpclient.util.HttpConstants;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

import static ml.comet.experiment.constants.Constants.*;
import static org.junit.jupiter.api.Assertions.*;

public class ConnectionUtilsTest {

    private static final String SOME_TEXT_FILE_NAME = "someTextFile.txt";

    @Test
    public void testCreateGetRequest() {
        String url = "http://test.com/get";
        HashMap<String, String> params = new HashMap<String, String>() {{
            put(EXPERIMENT_KEY, "test_key");
            put("type", ASSET_TYPE_SOURCE_CODE);
            put("overwrite", Boolean.toString(false));
        }};
        Request r = ConnectionUtils.createGetRequest(url, params);

        this.validateRequest(r, url, params, HttpConstants.Methods.GET, null);
        assertEquals(HttpConstants.Methods.GET, r.getMethod(), "wrong HTTP method");
    }

    @Test
    public void testCreatePostFileRequest() {
        String url = "http://test.com" + ADD_ASSET;
        HashMap<String, String> params = new HashMap<String, String>() {{
            put(EXPERIMENT_KEY, "test_key");
            put("type", ASSET_TYPE_SOURCE_CODE);
            put("overwrite", Boolean.toString(false));
        }};
        File file = TestUtils.getFile(SOME_TEXT_FILE_NAME);
        assertNotNull(file, "test file not found");

        Request r = ConnectionUtils.createPostFileRequest(file, url, params);
        this.validateRequest(r, url, params, HttpConstants.Methods.POST, ConnectionUtils.FORM_MIME_TYPE);

        // check body parts
        assertEquals(1, r.getBodyParts().size(), "wrong number of body parts");
        FilePart part = (FilePart) r.getBodyParts().get(0);
        assertEquals(ConnectionUtils.FILE, part.getName(), "wrong name");
        assertEquals(ConnectionUtils.FORM_MIME_TYPE, part.getContentType(), "wrong content type");
        assertEquals(file, part.getFile(), "wrong file");
    }

    @Test
    public void testCreatePostByteArrayRequest() {
        String url = "http://test.com" + ADD_ASSET;
        HashMap<String, String> params = new HashMap<String, String>() {{
            put(EXPERIMENT_KEY, "test_key");
            put("type", ASSET_TYPE_UNKNOWN);
            put("overwrite", Boolean.toString(true));
        }};
        byte[] data = "The test byte data".getBytes();

        Request r = ConnectionUtils.createPostByteArrayRequest(data, url, params);
        this.validateRequest(r, url, params, HttpConstants.Methods.POST, ConnectionUtils.FORM_MIME_TYPE);

        // check body parts
        assertEquals(1, r.getBodyParts().size(), "wrong number of body parts");
        ByteArrayPart part = (ByteArrayPart) r.getBodyParts().get(0);
        assertEquals(ConnectionUtils.FILE, part.getName(), "wrong name");
        assertEquals(ConnectionUtils.FORM_MIME_TYPE, part.getContentType(), "wrong content type");
        assertEquals(data, part.getBytes(), "wrong data array");
    }

    @Test
    public void testCreatePostJsonRequest() {
        String url = "http://test.com" + ADD_ASSET;
        HtmlRest html = new HtmlRest("<html><body></body></html", "test_key",
                false, System.currentTimeMillis());
        String json = JsonUtils.toJson(html);

        Request r = ConnectionUtils.createPostJsonRequest(json, url);
        this.validateRequest(r, url, null, HttpConstants.Methods.POST, ConnectionUtils.JSON_MIME_TYPE);

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

    private void validateRequest(Request r, String url, HashMap<String, String> params,
                                 String method, String contentType) {
        StringBuilder buf = new StringBuilder(url);
        if (params != null) {
            buf.append("?");
            params.forEach((k, v) -> buf.append(k).append("=").append(v).append("&"));
            buf.deleteCharAt(buf.length() - 1); // remove last ampersand
        }

        URI expected = URI.create(buf.toString());
        assertEquals(expected, URI.create(r.getUrl()));
        assertEquals(method, r.getMethod(), "wrong HTTP method");
        if (contentType != null) {
            assertEquals(contentType, r.getHeaders().get("Content-Type"), "wrong content type");
        }
    }
}
