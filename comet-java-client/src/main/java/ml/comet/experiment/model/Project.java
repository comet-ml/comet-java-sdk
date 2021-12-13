package ml.comet.experiment.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Holds data describing particular Comet project.
 */
@Data
@NoArgsConstructor
public class Project {
    private String projectId;
    private String projectName;
    private String ownerUserName;
    private String projectDescription;
    private String workspaceName;
    private boolean isPublic;
    private Integer numberOfExperiments;
    private Instant lastUpdated;
}
