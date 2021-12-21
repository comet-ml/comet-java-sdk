package ml.comet.experiment.impl.rest;

/**
 * Defines state of the particular version of the artifact.
 */
public enum ArtifactVersionState {
    OPEN(0),
    CLOSED(1),
    ERROR(2);

    private final Integer value;

    ArtifactVersionState(Integer value) {
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
