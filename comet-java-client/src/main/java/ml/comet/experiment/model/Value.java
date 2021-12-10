package ml.comet.experiment.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Represents value logged by the Comet experiment as a parameter, metric, etc.
 */
@Data
@NoArgsConstructor
public class Value {
    private String name;
    private String max;
    private String min;
    private String current;
    private Instant timestampMax;
    private Instant timestampMin;
    private Instant timestampCurrent;
    private String contextMax;
    private String contextMin;
    private String contextCurrent;
    private Long stepMax;
    private Long stepMin;
    private Long stepCurrent;
}
