package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class CometWebJavaSdkException extends CometWebException {
    @SuppressWarnings("checkstyle:MemberName")
    private int sdk_error_code;

    public CometWebJavaSdkException(int code, String msg, int sdkErrorCode) {
        super(code, msg);
        this.sdk_error_code = sdkErrorCode;
    }

    public int getSdkErrorCode() {
        return sdk_error_code;
    }
}