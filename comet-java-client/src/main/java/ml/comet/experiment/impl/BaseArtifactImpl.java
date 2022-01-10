package ml.comet.experiment.impl;

import com.vdurmont.semver4j.Semver;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ml.comet.experiment.impl.utils.DataModelUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * The base superclass for all artifact implementations.
 */
abstract class BaseArtifactImpl {
    @Getter
    @ToString.Include
    private final String name;
    @Getter
    @ToString.Include
    private final String type;
    @Getter
    @Setter
    Set<String> aliases;
    @Getter
    @Setter
    @ToString.Include
    Semver semanticVersion;
    @Getter
    @Setter
    Set<String> versionTags;
    @Setter
    String metadataJson;
    @Setter
    private Map<String, Object> metadata;

    BaseArtifactImpl(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public Map<String, Object> getMetadata() {
        if (this.metadata != null) {
            return this.metadata;
        }
        if (StringUtils.isNotBlank(this.metadataJson)) {
            try {
                this.metadata = DataModelUtils.metadataFromJson(this.metadataJson);
                return this.metadata;
            } catch (Throwable e) {
                this.getLogger().error("Failed to parse artifact metadata from JSON {}", this.metadataJson, e);
            }
        }
        return Collections.emptyMap();
    }

    abstract Logger getLogger();
}
