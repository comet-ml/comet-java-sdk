package ml.comet.experiment.registrymodel;

import lombok.Data;

/**
 * Holds information about created/updated model registry record.
 */
@Data
public class ModelRegistryRecord {
    private String registryName;
    private String registryModelId;
    private String registryModelItemId;

    public ModelRegistryRecord(String registryModelId, String registryModelItemId) {
        this.registryModelId = registryModelId;
        this.registryModelItemId = registryModelItemId;
    }
}
