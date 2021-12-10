package ml.comet.experiment.impl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encodes common fields of response received for POST logXXX requests.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogDataResponse {
    private String msg;
    private int code;
    private int sdkErrorCode;
    private String data;

    public boolean hasFailed() {
        return code != 200 || sdkErrorCode != 0;
    }
}
