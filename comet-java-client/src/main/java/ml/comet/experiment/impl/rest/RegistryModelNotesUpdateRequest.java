package ml.comet.experiment.impl.rest;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegistryModelNotesUpdateRequest {
    private String workspaceName;
    private String registryModelName;
    private String notes;

    public RegistryModelNotesUpdateRequest(String notes, String registryModelName, String workspaceName) {
        this.notes = notes;
        this.registryModelName = registryModelName;
        this.workspaceName = workspaceName;
    }
}
