package ml.comet.experiment.impl;

import lombok.Setter;
import ml.comet.experiment.artifact.LoggedArtifact;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The implementation of the {@link LoggedArtifact}.
 */
public final class LoggedArtifactImpl extends BaseArtifactImpl implements LoggedArtifact {
    @Setter
    Set<String> artifactTags;
    @Setter
    long sizeInBytes;
    @Setter
    String experimentKey;
    @Setter
    String workspace;
    @Setter
    String artifactVersionId;
    @Setter
    String artifactId;

    public LoggedArtifactImpl(String name, String type) {
        super(name, type);
    }

    @Override
    public Set<String> getArtifactTags() {
        if (this.artifactTags == null) {
            return Collections.emptySet();
        }
        return this.artifactTags;
    }

    @Override
    public String getArtifactType() {
        return this.getType();
    }

    @Override
    public Map<String, Object> getMetadata() {
        if (this.artifactMetadata == null) {
            return Collections.emptyMap();
        }
        return this.artifactMetadata;
    }

    @Override
    public long getSize() {
        return this.sizeInBytes;
    }

    @Override
    public String getSourceExperimentKey() {
        return this.experimentKey;
    }

    @Override
    public String getVersion() {
        if (this.semanticVersion != null) {
            return this.semanticVersion.getValue();
        }
        return StringUtils.EMPTY;
    }

    @Override
    public String getWorkspace() {
        return this.workspace;
    }
}
