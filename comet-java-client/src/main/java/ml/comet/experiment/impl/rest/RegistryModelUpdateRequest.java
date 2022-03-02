package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistryModelUpdateRequest {
    private String registryModelId;
    private String registryModelName;
    private String description;
    @JsonProperty("isPublic")
    private Boolean isPublic;
}
