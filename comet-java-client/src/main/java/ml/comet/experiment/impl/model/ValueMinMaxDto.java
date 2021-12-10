package ml.comet.experiment.impl.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValueMinMaxDto {
    private String name;
    private String valueMax;
    private String valueMin;
    private String valueCurrent;
    private Long timestampMax;
    private Long timestampMin;
    private Long timestampCurrent;
    private String runContextMax;
    private String runContextMin;
    private String runContextCurrent;
    private Long stepMax;
    private Long stepMin;
    private Long stepCurrent;
    private Boolean editable = false;
}
