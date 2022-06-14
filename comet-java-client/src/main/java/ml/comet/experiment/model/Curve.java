package ml.comet.experiment.model;

import lombok.Data;
import lombok.NonNull;

/**
 * Represents data of the curve as x/y pairs.
 */
@Data
@SuppressWarnings("checkstyle:MemberName")
public class Curve {
    private DataPoint[] dataPoints;
    private String name;

    /**
     * Creates new instance with specified parameters.
     *
     * @param dataPoints the curve data points.
     * @param name       the name of the curve.
     */
    public Curve(@NonNull DataPoint[] dataPoints, @NonNull String name) {
        this.dataPoints = dataPoints;
        this.name = name;
    }
}
