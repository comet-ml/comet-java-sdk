package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encodes common fields of response received from Comet REST API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class RestApiResponse {
    private String msg;
    private int code;
    private int sdkErrorCode;
    private String data;

    public RestApiResponse(int statusCode) {
        this.code = statusCode;
    }

    public RestApiResponse(int statusCode, String msg) {
        this(statusCode);
        this.msg = msg;
    }

    public RestApiResponse(int statusCode, String msg, int sdkErrorCode) {
        this(statusCode, msg);
        this.sdkErrorCode = sdkErrorCode;
    }

    public boolean hasFailed() {
        return code != 200 || sdkErrorCode != 0;
    }
}
