package ml.comet.experiment.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
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

    public ValueMinMaxDto() {}

    public ValueMinMaxDto(String name, String valueCurrent, Long timestampCurrent, Long stepCurrent, String runContextCurrent) {
        this.name = name;
        this.valueMax = valueCurrent;
        this.valueMin = valueCurrent;
        this.valueCurrent = valueCurrent;
        this.timestampMax = timestampCurrent;
        this.timestampMin = timestampCurrent;
        this.timestampCurrent = timestampCurrent;
        this.stepMax = stepCurrent;
        this.stepMin = stepCurrent;
        this.stepCurrent = stepCurrent;
        this.runContextMax = runContextCurrent;
        this.runContextMin = runContextCurrent;
        this.runContextCurrent = runContextCurrent;
    }

    public ValueMinMaxDto(String name, String valueMax, String valueMin, String valueCurrent, Long timestampMax, Long timestampMin, Long timestampCurrent, Long stepMax, Long stepMin, Long stepCurrent, String runContextMax, String runContextMin, String runContextCurrent) {
        this.name = name;
        this.valueMax = valueMax;
        this.valueMin = valueMin;
        this.timestampMax = timestampMax;
        this.timestampMin = timestampMin;
        this.valueCurrent = valueCurrent;
        this.timestampCurrent = timestampCurrent;
        this.stepMax = stepMax;
        this.stepMin = stepMin;
        this.stepCurrent = stepCurrent;
        this.runContextMax = runContextMax;
        this.runContextMin = runContextMin;
        this.runContextCurrent = runContextCurrent;
    }

    public void setMax(String value, Long timestamp, String context, Long step) {
        this.valueMax = value;
        this.timestampMax = timestamp;
        this.runContextMax = context;
        this.stepMax = step;
    }

    public void setMin(String value, Long timestamp, String context, Long step) {
        this.valueMin = value;
        this.timestampMin = timestamp;
        this.runContextMin = context;
        this.stepMin = step;
    }

    public void setCurrent(String value, Long timestamp, String context, Long step) {
        this.valueCurrent = value;
        this.timestampCurrent = timestamp;
        this.runContextCurrent = context;
        this.stepCurrent = step;
    }
}
