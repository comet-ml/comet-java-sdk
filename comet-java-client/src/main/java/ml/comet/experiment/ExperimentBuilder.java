package ml.comet.experiment;

import lombok.experimental.UtilityClass;
import ml.comet.experiment.builder.OnlineExperimentBuilder;
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
    public static OnlineExperimentBuilder OnlineExperiment() {
        return OnlineExperimentImpl.builder();
    }
}
