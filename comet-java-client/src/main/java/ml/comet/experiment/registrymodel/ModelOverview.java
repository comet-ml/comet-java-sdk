package ml.comet.experiment.registrymodel;

import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Holds overview of particular registry model which was retrieved from Comet server.
 */
@Data
public class ModelOverview {
    private String registryModelId;
    private String workspaceId;
    private String modelName;
    private String description;
    private long numberOfVersions;
    private boolean isPublic;
    private String userName;
    private List<ModelVersionOverview> versions;
    private Instant createdAt;
    private Instant lastUpdated;
}
