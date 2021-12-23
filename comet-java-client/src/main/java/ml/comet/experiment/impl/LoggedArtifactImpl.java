package ml.comet.experiment.impl;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.artifact.ArtifactException;
import ml.comet.experiment.artifact.LoggedArtifact;
import ml.comet.experiment.artifact.LoggedArtifactAsset;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * The implementation of the {@link LoggedArtifact}.
 */
@ToString
public final class LoggedArtifactImpl extends BaseArtifactImpl implements LoggedArtifact {
    @Getter
    private final Logger logger = LoggerFactory.getLogger(LoggedArtifact.class);

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
    @Getter
    String artifactId;

    final BaseExperiment baseExperiment;

    LoggedArtifactImpl(String name, String type, BaseExperiment baseExperiment) {
        super(name, type);
        this.baseExperiment = baseExperiment;
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

    @Override
    public String getVersionId() {
        return this.artifactVersionId;
    }

    @Override
    public Collection<LoggedArtifactAsset> readAssets() throws ArtifactException {
        return this.baseExperiment.readArtifactAssets(this);
    }
}
