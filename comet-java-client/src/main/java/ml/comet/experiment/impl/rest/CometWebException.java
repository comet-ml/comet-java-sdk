package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
public class CometWebException {
    private int code;
    private String msg;
    private String logMsg;
    private Map<String, String> params;

    public CometWebException(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}
