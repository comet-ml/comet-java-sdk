package ml.comet.experiment;

import lombok.experimental.UtilityClass;
import ml.comet.experiment.builder.ApiExperimentBuilder;
import ml.comet.experiment.builder.CometApiBuilder;
import ml.comet.experiment.builder.OnlineExperimentBuilder;
import ml.comet.experiment.impl.ApiExperimentImpl;
import ml.comet.experiment.impl.CometApiImpl;
import ml.comet.experiment.impl.OnlineExperimentImpl;

/**
 * The factory of builders to be used for creation of different {@link Experiment} implementations.
 */
@UtilityClass
public class ExperimentBuilder {

    /**
     * Returns instance of the {@link OnlineExperimentBuilder} which can be used to
     * configure and create fully initialized instance of the {@link OnlineExperiment}.
     *
     * <p>The configured instance of {@link OnlineExperiment} can be created as following:
     * <pre>
     *     OnlineExperiment experiment = ExperimentBuilder
     *                                      .OnlineExperiment()
     *                                      .build();
     * </pre>
     *
     * @return the instance of the {@link OnlineExperimentBuilder}.
     */
    @SuppressWarnings({"MethodName"})
    public static OnlineExperimentBuilder OnlineExperiment() {
        return OnlineExperimentImpl.builder();
    }

    /**
     * The factory to create instance of the {@link ApiExperimentBuilder} which can be used
     * to configure and create fully initialized instance of the {@link ApiExperiment}.
     *
     * @return the initialized instance of the {@link ApiExperimentBuilder}.
     */
    @SuppressWarnings({"MethodName"})
    public static ApiExperimentBuilder ApiExperiment() {
        return ApiExperimentImpl.builder();
    }

    /**
     * The factory to create instance of the {@link CometApiBuilder} which can be used to configure
     * and create fully initialized instance of the {@link CometApi}.
     *
     * @return the instance of the {@link CometApiBuilder}.
     */
    @SuppressWarnings("checkstyle:MethodName")
    public static CometApiBuilder CometApi() {
        return CometApiImpl.builder();
    }
}
