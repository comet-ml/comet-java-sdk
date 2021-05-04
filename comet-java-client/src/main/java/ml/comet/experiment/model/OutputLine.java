package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputLine {
    private String output;
    private boolean stderr = false;
    private long localTimestamp;
    private Long offset;
}
