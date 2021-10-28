package ml.comet.experiment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestProject {
    private String projectId;
    private String projectName;
    private String ownerUserName;
    private String projectDescription;
    private String workspaceName;
    @JsonProperty("public")
    private boolean isPublic;
    private int numberOfExperiments;
    private Long lastUpdated;

    public RestProject(GetProject getProject) {
        this.projectId = getProject.getProjectId();
        this.projectName = getProject.getProjectName();
        this.ownerUserName = getProject.getUserName();
        this.projectDescription = getProject.getProjectDesc();
        this.workspaceName = getProject.getTeamName();
        this.isPublic = getProject.isPublic();
        this.numberOfExperiments = getProject.getNumOfExperiments();
        this.lastUpdated = getProject.getLastUpdated();
    }
}
