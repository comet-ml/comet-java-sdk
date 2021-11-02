package ml.comet.experiment.http;

import ml.comet.experiment.model.HtmlRest;
import ml.comet.experiment.utils.JsonUtils;
import ml.comet.experiment.utils.TestUtils;
import org.asynchttpclient.Request;
import org.asynchttpclient.request.body.multipart.ByteArrayPart;
import org.asynchttpclient.request.body.multipart.FilePart;
import org.asynchttpclient.util.HttpConstants;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.net.URI;
import java.util.HashMap;

import static ml.comet.experiment.constants.Constants.*;

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
        Assert.assertEquals("wrong HTTP method", HttpConstants.Methods.GET, r.getMethod());
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
        Assert.assertNotNull("test file not found", file);

        Request r = ConnectionUtils.createPostFileRequest(file, url, params);
        this.validateRequest(r, url, params, HttpConstants.Methods.POST, ConnectionUtils.FORM_MIME_TYPE);

        // check body parts
        Assert.assertEquals("wrong number of body parts", 1, r.getBodyParts().size());
        FilePart part = (FilePart)r.getBodyParts().get(0);
        Assert.assertEquals("wrong name", ConnectionUtils.FILE, part.getName());
        Assert.assertEquals("wrong content type", ConnectionUtils.FORM_MIME_TYPE, part.getContentType());
        Assert.assertEquals("wrong file", file, part.getFile());
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
        Assert.assertEquals("wrong number of body parts", 1, r.getBodyParts().size());
        ByteArrayPart part = (ByteArrayPart)r.getBodyParts().get(0);
        Assert.assertEquals("wrong name", ConnectionUtils.FILE, part.getName());
        Assert.assertEquals("wrong content type", ConnectionUtils.FORM_MIME_TYPE, part.getContentType());
        Assert.assertEquals("wrong data array", data, part.getBytes());
    }

    @Test
    public void testCreatePostJsonRequest() {
        String url = "http://test.com" + ADD_ASSET;
        HtmlRest html = new HtmlRest("<html><body></body></html", "test_key",
                false, System.currentTimeMillis());
        String json = JsonUtils.toJson(html);

        Request r = ConnectionUtils.createPostJsonRequest(json, url);
        this.validateRequest(r, url, null, HttpConstants.Methods.POST, ConnectionUtils.JSON_MIME_TYPE);

        Assert.assertEquals("wrong body", json.length(), r.getBodyGenerator().createBody().getContentLength());
    }

    private void validateRequest(Request r, String url, HashMap<String, String> params,
                                 String method, String contentType) {
        StringBuilder buf = new StringBuilder(url);
        if (params != null) {
            buf.append("?");
            params.forEach((k, v) -> buf.append(k).append("=").append(v).append("&"));
            buf.deleteCharAt(buf.length()-1); // remove last ampersand
        }

        URI expected = URI.create(buf.toString());
        Assert.assertEquals(expected, URI.create(r.getUrl()));
        Assert.assertEquals("wrong HTTP method", method, r.getMethod());
        if (contentType != null) {
            Assert.assertEquals("wrong content type", contentType,
                    r.getHeaders().get("Content-Type"));
        }
    }
}
