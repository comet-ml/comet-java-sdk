package ml.comet.experiment.impl;

import com.vdurmont.semver4j.Semver;
import lombok.Data;

import java.util.Map;
import java.util.Set;

/**
 * The base superclass for all artifact implementations.
 */
@Data
class BaseArtifactImpl {
    private final String name;
    private final String type;
    Set<String> aliases;
    Map<String, Object> artifactMetadata;
    Semver semanticVersion;
    Set<String> versionTags;

    BaseArtifactImpl(String name, String type) {
        this.name = name;
        this.type = type;
    }
}
