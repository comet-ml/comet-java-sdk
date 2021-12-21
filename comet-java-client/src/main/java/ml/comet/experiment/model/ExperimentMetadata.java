package ml.comet.experiment.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

/**
 * Holds metadata associated with particular Comet experiment.
 */
@Data
@NoArgsConstructor
@SuppressWarnings("unused")
public class ExperimentMetadata {
    private String experimentKey;
    private String experimentName;
    private String userName;
    private String projectId;
    private String projectName;
    private String workspaceName;
    private Duration duration;
    private Instant startTime;
    private Instant endTime;
    private boolean isArchived;
    private boolean running;
}
