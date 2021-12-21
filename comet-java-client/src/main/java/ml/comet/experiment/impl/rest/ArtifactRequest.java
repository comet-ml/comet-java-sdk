package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class ArtifactRequest extends BaseExperimentObject {
    private String artifactId;
    private String artifactName;
    private String artifactVersionId;
    private String workspaceName;
    private String projectName;
    private String description;
    private String version;
    private String metadata;
    private String versionMetadata;
    private String artifactType;
    private Boolean isPublic = false;
    private String note;
    private String comment;
    private String emoji;
    private ArtifactVersionState state;
    private String[] tags;
    private String[] versionTags;
    private String[] alias;
}
