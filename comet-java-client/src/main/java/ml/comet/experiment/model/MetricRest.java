package ml.comet.experiment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricRest {
    private String experimentKey;
    private String metricName;
    private String metricValue;
    private String context;
    private Long step;
    private Long epoch;
    private Long timestamp;
}
