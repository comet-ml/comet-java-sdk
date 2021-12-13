package ml.comet.experiment.impl.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ArtifactDto {
    private String artifactId;
    private String artifactName;
    private String projectId;
    private String workspaceId;
    private String workspaceName;
    private String experimentKey;
    private String description;
    private String version;
    private String metadata;
    private String artifactType;
    private Boolean isPublic;
    private String owner;
    private Long s3FileSize;
    private String emoji;
    private List<String> tags;
}
