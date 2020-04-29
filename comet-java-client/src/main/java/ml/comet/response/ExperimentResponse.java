package ml.comet.response;

import lombok.Data;

import java.util.List;

@Data
public class ExperimentResponse {
    private List<ExperimentRest> experiments;
}
