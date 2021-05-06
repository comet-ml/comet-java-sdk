package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RestProject {
    private String projectId;
    private String projectName;
    private String ownerUserName;
    private String projectDescription;
    private String workspaceName;
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
