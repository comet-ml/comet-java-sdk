package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExperimentTimeRequest {
    private String experimentKey;
    private Long startTimeMillis;
    private Long endTimeMillis;
}
