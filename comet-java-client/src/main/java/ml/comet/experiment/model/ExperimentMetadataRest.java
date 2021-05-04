package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
