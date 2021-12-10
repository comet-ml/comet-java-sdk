package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.model.Project;

import java.time.Instant;

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

    /**
     * Converts this into {@link Project} model object of the public API.
     *
     * @return the initialized {@link Project} instance.
     */
    public Project toProject() {
        Project p = new Project();
        p.setProjectId(this.projectId);
        p.setProjectName(this.projectName);
        p.setOwnerUserName(this.ownerUserName);
        p.setProjectDescription(this.projectDescription);
        p.setWorkspaceName(this.workspaceName);
        p.setPublic(this.isPublic);
        p.setNumberOfExperiments(this.numberOfExperiments);
        p.setLastUpdated(Instant.ofEpochMilli(this.lastUpdated));
        return p;
    }
}
