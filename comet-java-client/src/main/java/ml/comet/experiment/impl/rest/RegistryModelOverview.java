package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@Data
public class RegistryModelOverview {
    private String registryModelId;
    private String modelName;
    private String description;
    private long numberOfVersions;
    @JsonProperty("isPublic")
    private boolean isPublic;
    private RegistryModelItemOverview latestVersion;
    private String imagePath;
    private String userName;
    private long createdAt;
    private long lastUpdated;
}
