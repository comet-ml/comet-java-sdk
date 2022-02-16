package ml.comet.experiment.impl.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LogAdditionalSystemInfo {
    private String key;
    private String value;
}
