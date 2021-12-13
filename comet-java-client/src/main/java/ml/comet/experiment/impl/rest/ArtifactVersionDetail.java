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
import ml.comet.experiment.impl.utils.DataModelUtils;
import ml.comet.experiment.impl.utils.JsonUtils;
import org.slf4j.Logger;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.HashSet;
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
    List<ArtifactVersionAsset> files;
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
        if (this.alias != null) {
            a.setAliases(new HashSet<>(this.alias));
        }
        a.setArtifactId(this.artifact.getArtifactId());
        if (this.artifact.getTags() != null) {
            a.setArtifactTags(new HashSet<>(this.artifact.getTags()));
        }
        a.setArtifactVersionId(this.artifactVersionId);
        a.setExperimentKey(this.experimentKey);
        a.setSizeInBytes(this.sizeInBytes);
        if (this.tags != null) {
            a.setVersionTags(new HashSet<>(this.tags));
        }
        a.setWorkspace(this.artifact.getWorkspaceName());

        if (this.metadata != null) {
            try {
                a.setArtifactMetadata(DataModelUtils.metadataFromJson(this.metadata));
            } catch (Throwable e) {
                logger.error("Couldn't decode metadata for artifact {}:{}", a.getName(), a.getVersion());
            }
        }

        return a;
    }
}
