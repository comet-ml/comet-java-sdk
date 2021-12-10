package ml.comet.experiment.impl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateExperimentRequest {
    private String workspaceName;
    private String projectName;
    private String experimentName;
    private Boolean disableHeartBeat = false;

    public CreateExperimentRequest(String workspaceName, String projectName, String experimentName) {
        this.workspaceName = workspaceName;
        this.projectName = projectName;
        this.experimentName = experimentName;
    }
}
