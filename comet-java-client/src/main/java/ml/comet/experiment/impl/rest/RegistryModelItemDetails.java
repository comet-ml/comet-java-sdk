package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegistryModelItemDetails {
    private String registryModelItemId;
    private ExperimentModelResponse experimentModel;
    private String version;
    private String comment;
    private List<String> stages;
    private List<ExperimentAssetLink> assets;
    private String userName;
    private long createdAt;
    private long lastUpdated;
    private String restApiUrl;
}
