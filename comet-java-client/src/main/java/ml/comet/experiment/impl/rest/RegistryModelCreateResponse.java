package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.registrymodel.ModelRegistry;

@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class RegistryModelCreateResponse {
    private String registryModelId;
    private String registryModelItemId;

    public ModelRegistry toModelRegistry() {
        return new ModelRegistry(this.registryModelId, this.registryModelItemId);
    }
}
