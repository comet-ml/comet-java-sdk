package ml.comet.experiment.impl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestProject {
    private String projectId;
    private String projectName;
    private String ownerUserName;
    private String projectDescription;
    private String workspaceName;
    @JsonProperty("public")
    private boolean isPublic;
    private int numberOfExperiments;
    private Long lastUpdated;
}
