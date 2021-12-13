package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import ml.comet.experiment.model.ExperimentMetadata;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static ml.comet.experiment.impl.utils.CometUtils.durationOrNull;
import static ml.comet.experiment.impl.utils.CometUtils.instantOrNull;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("unused")
public class ExperimentMetadataRest extends BaseExperimentObject {
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

    /**
     * Converts this instance into public API model object.
     *
     * @return the instance of the {@link ExperimentMetadata} with data from this object.
     */
    public ExperimentMetadata toExperimentMetadata() {
        ExperimentMetadata data = new ExperimentMetadata();
        data.setExperimentKey(this.experimentKey);
        data.setExperimentName(this.experimentName);
        data.setUserName(this.userName);
        data.setProjectId(this.projectId);
        data.setProjectName(this.projectName);
        data.setWorkspaceName(this.workspaceName);
        data.setDuration(durationOrNull(this.durationMillis));
        data.setStartTime(instantOrNull(this.startTimeMillis));
        data.setEndTime(instantOrNull(this.endTimeMillis));
        data.setArchived(this.isArchived);
        data.setRunning(this.isRunning());
        return data;
    }
}
