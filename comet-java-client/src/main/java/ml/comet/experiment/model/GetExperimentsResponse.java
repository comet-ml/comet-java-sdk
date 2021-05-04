package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GetExperimentsResponse {
    List<ExperimentMetadataRest> experiments;
}
