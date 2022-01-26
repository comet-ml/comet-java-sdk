package ml.comet.experiment.impl;

import com.vdurmont.semver4j.Semver;
import lombok.Getter;
import lombok.Setter;
import ml.comet.experiment.registrymodel.Model;
import ml.comet.experiment.registrymodel.ModelBuilder;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

import static ml.comet.experiment.impl.resources.LogMessages.INVALID_MODEL_REGISTRY_NAME_PROVIDED;
import static ml.comet.experiment.impl.resources.LogMessages.getString;
import static ml.comet.experiment.impl.utils.ModelUtils.createRegistryModelName;

/**
 * The implementation of the {@link Model}.
 */
public final class RegistryModelImpl implements Model {
    private final String name;
    private Semver version;
    private String registryModelName;
    @Getter
    private Boolean isPublic;
    @Setter
    private String workspace;
    @Getter
    private String description;
    @Getter
    private String comment;
    @Getter
    private List<String> stages;
    @Getter
    @Setter
    private String experimentModelId;

    RegistryModelImpl(String name) {
        this.name = name;
        this.version = new Semver("1.0.0");
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getVersion() {
        return this.version.getValue();
    }

    @Override
    public String getWorkspace() {
        return this.workspace;
    }

    @Override
    public String getRegistryName() {
        return this.registryModelName;
    }

    @Override
    public boolean isPublic() {
        if (this.isPublic != null) {
            return this.isPublic;
        }
        return false;
    }

    /**
     * The implementation of the {@link ModelBuilder}.
     */
    public static class RegistryModelBuilderImpl implements ModelBuilder {
        final RegistryModelImpl model;

        public RegistryModelBuilderImpl(String name) {
            this.model = new RegistryModelImpl(name);
        }

        @Override
        public RegistryModelBuilderImpl withVersion(String version) {
            this.model.version = new Semver(version);
            return this;
        }

        @Override
        public RegistryModelBuilderImpl withWorkspace(String workspace) {
            this.model.workspace = workspace;
            return this;
        }

        @Override
        public RegistryModelBuilderImpl withRegistryName(String registryModelName) {
            this.model.registryModelName = registryModelName;
            return this;
        }

        @Override
        public RegistryModelBuilderImpl asPublic(boolean isPublic) {
            this.model.isPublic = isPublic;
            return this;
        }

        @Override
        public RegistryModelBuilderImpl withDescription(String description) {
            this.model.description = description;
            return this;
        }

        @Override
        public RegistryModelBuilderImpl withComment(String comment) {
            this.model.comment = comment;
            return this;
        }

        @Override
        public RegistryModelBuilderImpl withStages(List<String> tags) {
            this.model.stages = tags;
            return this;
        }

        @Override
        public Model build() {
            if (StringUtils.isBlank(this.model.registryModelName)) {
                this.model.registryModelName = createRegistryModelName(this.model.name);
            } else if (!createRegistryModelName(this.model.registryModelName).equals(this.model.registryModelName)) {
                // check that provided registry name is valid
                throw new IllegalArgumentException(
                        getString(INVALID_MODEL_REGISTRY_NAME_PROVIDED,
                                this.model.registryModelName, createRegistryModelName(this.model.registryModelName)));
            }

            return this.model;
        }
    }
}
