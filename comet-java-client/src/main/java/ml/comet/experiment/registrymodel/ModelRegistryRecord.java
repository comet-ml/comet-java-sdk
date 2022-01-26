package ml.comet.experiment.registrymodel;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Holds information about created/updated model registry record.
 */
@Data
@AllArgsConstructor
public class ModelRegistryRecord {
    private String registryModelId;
    private String registryModelItemId;
}
