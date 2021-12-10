package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ml.comet.experiment.model.Value;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValueMinMaxRest {
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

    /**
     * Converts this object into public API {@link Value} object.
     *
     * @return the initialized {@link Value} instance.
     */
    public Value toValue() {
        Value v = new Value();
        v.setName(this.name);
        v.setMax(this.valueMax);
        v.setMin(this.valueMin);
        v.setCurrent(this.valueCurrent);
        v.setTimestampMax(Instant.ofEpochMilli(this.timestampMax));
        v.setTimestampMin(Instant.ofEpochMilli(this.timestampMin));
        v.setTimestampCurrent(Instant.ofEpochMilli(this.timestampCurrent));
        v.setContextMax(this.runContextMax);
        v.setContextMin(this.runContextMin);
        v.setContextCurrent(this.runContextCurrent);
        v.setStepMax(this.stepMax);
        v.setStepMin(this.stepMin);
        v.setStepCurrent(this.stepCurrent);
        return v;
    }
}
