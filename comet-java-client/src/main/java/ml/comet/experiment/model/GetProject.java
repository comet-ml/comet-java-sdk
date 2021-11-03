package ml.comet.experiment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.Timestamp;


@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetProject {
    private String projectId;
    private String userName;
    private String projectName;
    private String projectDesc;
    private String imagePath;
    private String teamId;
    private boolean isOwner = false;

    @JsonProperty("isPublic")
    private boolean isPublic;

    @JsonProperty("isShared")
    private boolean isShared = false;

    @JsonProperty("isStarterProject")
    private boolean isStarterProject = false;

    private int numOfExperiments;

    @JsonProperty("isGeneral")
    private boolean isGeneral = false;

    @JsonProperty("isUserOnTeamForProject")
    private boolean isUserOnTeamForProject = false;

    private Long lastUpdated;

    private Long createdAt;

    private String teamName = null;

    private boolean canEdit = false;

    private int maxAllowedPinnedExperiments = 10;

    public GetProject() {
    }

    public GetProject(
            String projectId,
            String userName,
            String projectName,
            String projectDesc,
            String imagePath,
            Boolean isOwner,
            int numOfExperiments,
            boolean isPublic,
            Long lastUpdated,
            boolean isShared,
            boolean isStarterProject,
            Timestamp createdAt
    ) {
        this.projectId = projectId;
        this.userName = userName;
        this.projectName = projectName;
        this.projectDesc = projectDesc;
        this.imagePath = imagePath;
        this.isOwner = isOwner;
        this.numOfExperiments = numOfExperiments;
        this.isPublic = isPublic;
        this.lastUpdated = lastUpdated;
        this.isShared = isShared;
        this.isStarterProject = isStarterProject;

        if (isOwner) {
            this.canEdit = true;
        }

        if (createdAt != null) {
            this.createdAt = createdAt.getTime();
        } else {
            this.createdAt = null;
        }
    }

    public void setGeneral(boolean general) {
        isGeneral = general;
        if (general) {
            this.canEdit = true;
        }
    }

    @JsonProperty("owner")
    public boolean getIsOwner() {
        return isOwner;
    }

    @JsonProperty("isOwner")
    public boolean isOwner() {
        return isOwner;
    }

    public void setOwner(boolean owner) {
        isOwner = owner;
        if (owner) {
            this.canEdit = true;
        }
    }

    public void setUserOnTeamForProject(boolean userOnTeamForProject) {
        isUserOnTeamForProject = userOnTeamForProject;
        if (userOnTeamForProject) {
            this.canEdit = true;
        }
    }
}
