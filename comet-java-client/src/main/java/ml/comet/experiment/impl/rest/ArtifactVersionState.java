package ml.comet.experiment.impl.rest;

public enum ArtifactVersionState {
    OPEN(0),
    CLOSED(1),
    ERROR(2);

    private Integer value;

    ArtifactVersionState(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
