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
import ml.comet.experiment.impl.utils.DataModelUtils;
import org.slf4j.Logger;

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
     * Converts to the {@link LoggedArtifact} instance.
     *
     * @param logger the logger for output.
     * @return the {@link LoggedArtifact} instance.
     */
    public LoggedArtifact toLoggedArtifact(Logger logger) {
        LoggedArtifactImpl a = new LoggedArtifactImpl(this.artifact.getArtifactName(), this.artifact.getArtifactType());
        a.setSemanticVersion(new Semver(this.artifactVersion));
        a.setArtifactId(this.artifact.getArtifactId());
        a.setArtifactVersionId(this.artifactVersionId);
        a.setExperimentKey(this.experimentKey);
        a.setSizeInBytes(this.sizeInBytes);
        a.setWorkspace(this.artifact.getWorkspaceName());

        a.setAliases(CometUtils.setFromList(this.alias));
        a.setArtifactTags(CometUtils.setFromList(this.artifact.getTags()));
        a.setVersionTags(CometUtils.setFromList(this.tags));

        if (this.metadata != null) {
            try {
                a.setArtifactMetadata(DataModelUtils.metadataFromJson(this.metadata));
            } catch (Throwable e) {
                logger.error("Couldn't decode metadata for artifact {}:{}", a.getName(), a.getVersion(), e);
            }
        }

        return a;
    }
}
