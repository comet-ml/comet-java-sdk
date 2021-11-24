package ml.comet.experiment;

import lombok.experimental.UtilityClass;

/**
 * This is stub to support backward compatibility.
 *
 * @deprecated You should use {@link ExperimentBuilder#OnlineExperiment()} instead to create
 * instance of the {@link OnlineExperiment}.
 */
@UtilityClass
public final class OnlineExperimentImpl {
    /**
     * Returns builder to be used to create properly configured instance of {@link OnlineExperiment}.
     *
     * @return the builder to be used to create properly configured instance of {@link OnlineExperiment}.
     * @deprecated please use {@link ExperimentBuilder#OnlineExperiment()} instead.
     */
    public static ml.comet.experiment.builder.OnlineExperimentBuilder builder() {
        return ml.comet.experiment.impl.OnlineExperimentImpl.builder();
    }
}
