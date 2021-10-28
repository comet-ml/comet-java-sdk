package ml.comet.experiment.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExperimentMetadataRest {
    private String experimentKey;
    private String experimentName;
    private String optimizationId;
    private String userName;
    private String projectId;
    private String projectName;
    private String workspaceName;
    private String filePath;
    private String fileName;
    private Boolean throttle;
    private Long durationMillis;
    private Long startTimeMillis;
    private Long endTimeMillis;
    private boolean isArchived;
    private boolean running;
}
