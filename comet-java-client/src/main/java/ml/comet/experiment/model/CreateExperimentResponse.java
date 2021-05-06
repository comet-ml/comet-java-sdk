package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateExperimentResponse {
    private String experimentKey;
    private String workspaceName;
    private String projectName;
    private String link;
}
