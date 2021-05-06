package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OutputUpdate {
    private String experimentKey;
    private String runContext;
    private List<OutputLine> outputLines;
}
