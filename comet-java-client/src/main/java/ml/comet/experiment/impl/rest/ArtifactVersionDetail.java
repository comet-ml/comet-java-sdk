package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.vdurmont.semver4j.Semver;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.impl.LoggedArtifactImpl;
import ml.comet.experiment.impl.utils.CometUtils;

import java.sql.Timestamp;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactVersionDetail extends BaseExperimentObject {
    String artifactVersionId;
    String artifactVersion;
    String owner;
    String createdFrom;
    Long sizeInBytes = 0L;
    String metadata;
    String note;
    Timestamp added;
    ArtifactVersionState state;
    List<String> tags;
    List<String> alias;
    ArtifactDto artifact;

    /**
     * Copy values to the provided {@link LoggedArtifact} instance.
     *
     * @param artifact the {@link LoggedArtifactImpl} instance to be populated.
     * @return the {@link LoggedArtifact} instance.
     */
    public LoggedArtifact copyToLoggedArtifact(LoggedArtifactImpl artifact) {
        artifact.setSemanticVersion(new Semver(this.artifactVersion));
        artifact.setArtifactId(this.artifact.getArtifactId());
        artifact.setArtifactVersionId(this.artifactVersionId);
        artifact.setExperimentKey(this.experimentKey);
        artifact.setSizeInBytes(this.sizeInBytes);
        artifact.setWorkspace(this.artifact.getWorkspaceName());

        artifact.setAliases(CometUtils.setFromList(this.alias));
        artifact.setArtifactTags(CometUtils.setFromList(this.artifact.getTags()));
        artifact.setVersionTags(CometUtils.setFromList(this.tags));

        artifact.setMetadataJson(this.metadata);
        return artifact;
    }
}
