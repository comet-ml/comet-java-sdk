package ml.comet.experiment.impl.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import ml.comet.experiment.model.Curve;
import ml.comet.experiment.model.DataPoint;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuppressWarnings("checkstyle:MemberName")
public class CurveData {
    private String name;
    private float[] x;
    private float[] y;

    private CurveData() {
    }

    /**
     * The factory to create data from provided {@code Curve} instance.
     *
     * @param curve the {@code Curve} instance.
     * @return the initialized data holder.
     */
    public static CurveData from(Curve curve) {
        CurveData data = new CurveData();
        data.name = curve.getName();
        DataPoint[] dataPoints = curve.getDataPoints();
        data.x = new float[dataPoints.length];
        data.y = new float[dataPoints.length];
        for (int i = 0; i < dataPoints.length; i++) {
            data.x[i] = dataPoints[i].getX();
            data.y[i] = dataPoints[i].getY();
        }
        return data;
    }
}
