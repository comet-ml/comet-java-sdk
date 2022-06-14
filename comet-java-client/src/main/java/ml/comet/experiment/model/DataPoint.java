package ml.comet.experiment.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Represents particular data point on the two dimensional curve.
 */
@Data
@AllArgsConstructor
@SuppressWarnings("checkstyle:MemberName")
public class DataPoint {
    private float x;
    private float y;

    /**
     * Creates new instance with specified data.
     *
     * @param x the value by X-axis.
     * @param y the value by Y-axis.
     * @return initialized instance of {@code DataPoint} with provided values.
     */
    public static DataPoint of(float x, float y) {
        return new DataPoint(x, y);
    }
}
