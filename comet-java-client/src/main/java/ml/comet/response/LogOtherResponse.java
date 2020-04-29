package ml.comet.response;

import lombok.Data;

import java.util.List;

@Data
public class LogOtherResponse {
    private List<ValueSummary> logOtherList;
}
